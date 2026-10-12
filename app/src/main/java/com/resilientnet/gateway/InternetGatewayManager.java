package com.resilientnet.gateway;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Internet Gateway Detection Manager for ResilientNet.
 *
 * Monitors real-time internet connectivity (Cellular data or Wi-Fi).
 * When this node has active internet access, it transitions into an "Internet Gateway Node",
 * enabling it to bridge the offline Bluetooth mesh to the cloud backend.
 */
public class InternetGatewayManager {

    private static final String TAG = "ResilientNetGateway";

    private final Context context;
    private final BackendSyncManager syncManager;
    private final ConnectivityManager connectivityManager;

    private final MutableLiveData<Boolean> isGatewayNodeLiveData =
            new MutableLiveData<>(false);

    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isMonitoring = false;

    public InternetGatewayManager(
            Context context,
            BackendSyncManager syncManager) {

        this.context = context.getApplicationContext();
        this.syncManager = syncManager;
        this.connectivityManager =
                (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public LiveData<Boolean> getIsGatewayNodeLiveData() {
        return isGatewayNodeLiveData;
    }

    public boolean isGatewayOnline() {
        Boolean value = isGatewayNodeLiveData.getValue();
        return value != null && value;
    }

    /**
     * Start monitoring network state changes.
     */
    public synchronized void startGatewayMonitor() {
        if (isMonitoring || connectivityManager == null) {
            return;
        }

        isMonitoring = true;
        Log.d(TAG, "Starting Internet Gateway monitor...");

        // Check current connectivity immediately
        boolean initiallyConnected = checkActiveInternetConnection();
        isGatewayNodeLiveData.postValue(initiallyConnected);

        if (initiallyConnected) {
            Log.i(TAG, ">>> Device launched as an INTERNET GATEWAY NODE <<<");
            syncManager.syncPendingEmergencyData(null);
        } else {
            Log.d(TAG, "Device running in pure OFFLINE MESH mode.");
        }

        try {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    Log.i(TAG, "Default network available. Evaluating capabilities...");
                    refreshGatewayState();
                }

                @Override
                public void onLost(@NonNull Network network) {
                    Log.w(TAG, "Default network lost. Node transitioned to OFFLINE MESH mode.");
                    isGatewayNodeLiveData.postValue(false);
                }

                @Override
                public void onCapabilitiesChanged(
                        @NonNull Network network,
                        @NonNull NetworkCapabilities networkCapabilities) {

                    boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);

                    Log.d(TAG, "Gateway default network capabilities changed: hasInternet = " + hasInternet);
                    isGatewayNodeLiveData.postValue(hasInternet);

                    if (hasInternet) {
                        syncManager.syncPendingEmergencyData(null);
                    }
                }
            };

            connectivityManager.registerDefaultNetworkCallback(networkCallback);

        } catch (Exception e) {
            Log.e(TAG, "Failed to register default network callback: " + e.getMessage(), e);
        }
    }

    /**
     * Stop monitoring network state changes.
     */
    public synchronized void stopGatewayMonitor() {
        if (!isMonitoring) {
            return;
        }

        isMonitoring = false;
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
            networkCallback = null;
        }
        Log.d(TAG, "Stopped Internet Gateway monitor.");
    }

    /**
     * Checks if the device currently possesses active network capabilities (Wi-Fi, Cellular, or validated Internet).
     */
    public boolean checkActiveInternetConnection() {
        if (connectivityManager == null) {
            return false;
        }

        try {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return false;
            }

            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (caps == null) {
                return false;
            }

            // On local emergency LANs (e.g. connecting to laptop on same Wi-Fi), NET_CAPABILITY_VALIDATED
            // can be false if external internet is unavailable or slow. We allow connection if on Wi-Fi/Cellular/Ethernet:
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
        } catch (Exception e) {
            Log.e(TAG, "Error checking active internet: " + e.getMessage());
            return false;
        }
    }

    /**
     * Refreshes the gateway state based on real-time network evaluation.
     */
    public void refreshGatewayState() {
        boolean online = checkActiveInternetConnection();
        Log.d(TAG, "Refreshing gateway state: online = " + online);
        isGatewayNodeLiveData.postValue(online);
        if (online) {
            syncManager.syncPendingEmergencyData(null);
        }
    }

    /**
     * Manually triggers a synchronization cycle.
     */
    public void triggerManualSync(@Nullable SyncCallback callback) {
        syncManager.syncPendingEmergencyData(callback);
    }

    public BackendSyncManager getSyncManager() {
        return syncManager;
    }
}
