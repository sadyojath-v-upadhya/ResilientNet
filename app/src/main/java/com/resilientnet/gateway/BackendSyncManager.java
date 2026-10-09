package com.resilientnet.gateway;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.local.entity.MessageEntity;
import com.resilientnet.data.local.entity.SosEntity;
import com.resilientnet.data.model.MessageConstants;
import com.resilientnet.utils.session.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Backend Synchronization Engine for ResilientNet.
 *
 * Automatically uploads stored offline emergency packets (SOS alerts and pending messages)
 * to a central cloud/backend server whenever the device detects internet connectivity.
 */
public class BackendSyncManager {

    private static final String TAG = "ResilientNetSync";

    private final Context context;
    private final AppDatabase database;
    private final SessionManager sessionManager;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private final MutableLiveData<SyncStatus> syncStatusLiveData =
            new MutableLiveData<>(SyncStatus.IDLE);
    private final MutableLiveData<Long> lastSyncTimeLiveData =
            new MutableLiveData<>(0L);

    private final AtomicBoolean isSyncing = new AtomicBoolean(false);

    public BackendSyncManager(
            Context context,
            AppDatabase database,
            SessionManager sessionManager) {

        this.context = context.getApplicationContext();
        this.database = database;
        this.sessionManager = sessionManager;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public LiveData<SyncStatus> getSyncStatusLiveData() {
        return syncStatusLiveData;
    }

    public LiveData<Long> getLastSyncTimeLiveData() {
        return lastSyncTimeLiveData;
    }

    public boolean isSyncing() {
        return isSyncing.get();
    }

    /**
     * Synchronizes all unsynced local SOS alerts and pending messages to the backend.
     */
    public void syncPendingEmergencyData(@Nullable SyncCallback callback) {
        if (!isSyncing.compareAndSet(false, true)) {
            Log.d(TAG, "Sync already in progress. Skipping duplicate request.");
            return;
        }

        syncStatusLiveData.postValue(SyncStatus.SYNCING);
        if (callback != null) {
            mainHandler.post(callback::onSyncStarted);
        }

        executorService.execute(() -> {
            try {
                performSync(callback);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during backend sync: " + e.getMessage(), e);
                syncStatusLiveData.postValue(SyncStatus.ERROR);
                if (callback != null) {
                    mainHandler.post(() -> callback.onSyncFailed(e.getMessage()));
                }
            } finally {
                isSyncing.set(false);
            }
        });
    }

    private void performSync(@Nullable SyncCallback callback) {
        List<SosEntity> unsyncedSosList = database.sosDao().getUnsyncedSosRequests();
        List<MessageEntity> pendingMessageList = database.messageDao().getPendingMessages();

        int totalSos = unsyncedSosList != null ? unsyncedSosList.size() : 0;
        int totalMsg = pendingMessageList != null ? pendingMessageList.size() : 0;

        Log.d(TAG, "Starting sync: " + totalSos + " unsynced SOS alerts, " + totalMsg + " pending messages.");

        if (totalSos == 0 && totalMsg == 0) {
            Log.d(TAG, "No pending emergency data to sync. All records up to date.");
            syncStatusLiveData.postValue(SyncStatus.IDLE);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncSuccess(0, 0));
            }
            return;
        }

        String targetUrl = sessionManager.getBackendApiUrl();
        Log.i(TAG, "Posting emergency sync payload to backend: " + targetUrl);

        try {
            JSONObject payload = buildSyncJsonPayload(unsyncedSosList, pendingMessageList);

            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);

            byte[] jsonBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBytes);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Backend HTTP response code: " + responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                // Read response
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                Log.i(TAG, "Sync successful! Server response: " + sb);

                // Update local Room database
                markRecordsAsSynced(unsyncedSosList, pendingMessageList);

                syncStatusLiveData.postValue(SyncStatus.SUCCESS);
                lastSyncTimeLiveData.postValue(System.currentTimeMillis());

                if (callback != null) {
                    mainHandler.post(() -> callback.onSyncSuccess(totalSos, totalMsg));
                }

            } else {
                String errorMsg = "Backend returned HTTP " + responseCode;
                Log.w(TAG, "Backend sync failed: " + errorMsg);
                syncStatusLiveData.postValue(SyncStatus.ERROR);

                if (callback != null) {
                    mainHandler.post(() -> callback.onSyncFailed(errorMsg));
                }
            }

            conn.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Network exception during backend sync to " + targetUrl + ": " + e.getMessage());
            syncStatusLiveData.postValue(SyncStatus.ERROR);

            if (callback != null) {
                mainHandler.post(() -> callback.onSyncFailed("Network error: " + e.getMessage()));
            }
        }
    }

    private JSONObject buildSyncJsonPayload(
            @Nullable List<SosEntity> sosList,
            @Nullable List<MessageEntity> msgList) throws Exception {

        JSONObject root = new JSONObject();
        root.put("gatewayDeviceId", sessionManager.getDeviceId());
        root.put("timestamp", System.currentTimeMillis());

        JSONArray sosArray = new JSONArray();
        if (sosList != null) {
            for (SosEntity sos : sosList) {
                JSONObject item = new JSONObject();
                item.put("localId", sos.getId());
                item.put("emergencyType", sos.getEmergencyType());
                item.put("message", sos.getMessage());
                item.put("locationName", sos.getLocationName());
                item.put("latitude", sos.getLatitude());
                item.put("longitude", sos.getLongitude());
                item.put("timestamp", sos.getTimestamp());
                item.put("status", sos.getStatus());
                sosArray.put(item);
            }
        }
        root.put("sosAlerts", sosArray);

        JSONArray msgArray = new JSONArray();
        if (msgList != null) {
            for (MessageEntity msg : msgList) {
                JSONObject item = new JSONObject();
                item.put("messageId", msg.getMessageId());
                item.put("senderId", msg.getSenderId());
                item.put("receiverId", msg.getReceiverId());
                item.put("messageType", msg.getMessageType());
                item.put("content", msg.getContent());
                item.put("timestamp", msg.getTimestamp());
                item.put("latitude", msg.getLatitude());
                item.put("longitude", msg.getLongitude());
                item.put("priority", msg.getPriority());
                item.put("hopCount", msg.getHopCount());
                msgArray.put(item);
            }
        }
        root.put("messages", msgArray);

        return root;
    }

    private void markRecordsAsSynced(
            @Nullable List<SosEntity> sosList,
            @Nullable List<MessageEntity> msgList) {

        if (sosList != null) {
            for (SosEntity sos : sosList) {
                sos.setSynced(true);
                sos.setStatus(MessageConstants.DELIVERED);
                database.sosDao().updateSos(sos);
            }
        }

        if (msgList != null) {
            for (MessageEntity msg : msgList) {
                msg.setStatus(MessageConstants.DELIVERED);
                database.messageDao().updateMessage(msg);
            }
        }

        Log.d(TAG, "Successfully marked records as synced in Room database.");
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
