package com.resilientnet.communication;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.resilientnet.bluetooth.AutoMeshManager;
import com.resilientnet.bluetooth.BluetoothConnectionObserver;
import com.resilientnet.bluetooth.BluetoothConnectionService;
import com.resilientnet.bluetooth.BluetoothConnectionThread;
import com.resilientnet.data.local.dao.SosDao;
import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.local.entity.MessageEntity;
import com.resilientnet.data.local.entity.SosEntity;
import com.resilientnet.data.model.MessageConstants;
import com.resilientnet.data.model.MessageSerializer;
import com.resilientnet.data.repository.MessageRepository;

import com.resilientnet.emergency.EmergencyAlarmManager;
import com.resilientnet.gateway.BackendSyncManager;
import com.resilientnet.gateway.InternetGatewayManager;
import com.resilientnet.utils.session.SessionManager;

import java.util.List;

public class CommunicationManager
        implements BluetoothConnectionObserver {

    private static final String TAG =
            "ResilientNetCommunication";
    private static final int MAX_HOPS = 5;
    private final Context context;
    private final MessageRepository messageRepository;
    private final SosDao sosDao;

    private final BluetoothConnectionService
            bluetoothService;

    private final AutoMeshManager autoMeshManager;
    private final BackendSyncManager backendSyncManager;
    private final InternetGatewayManager internetGatewayManager;

    public CommunicationManager(
            Context context,
            MessageRepository messageRepository) {

        this.context = context.getApplicationContext();
        this.messageRepository =
                messageRepository;

        AppDatabase appDatabase = AppDatabase.getInstance(context);
        this.sosDao = appDatabase.sosDao();

        SessionManager sessionManager = new SessionManager(context);
        this.backendSyncManager =
                new BackendSyncManager(context, appDatabase, sessionManager);
        this.internetGatewayManager =
                new InternetGatewayManager(context, backendSyncManager);
        this.internetGatewayManager.startGatewayMonitor();

        bluetoothService =
                BluetoothConnectionService.getInstance(
                        context
                );

        bluetoothService.registerObserver(
                this
        );

        bluetoothService.startServer();

        autoMeshManager =
                new AutoMeshManager(
                        context,
                        bluetoothService
                );

        autoMeshManager.startAutoMesh();
    }

    // ---------------------------------------------------------
    // SEND
    // ---------------------------------------------------------

    public void sendMessage(
            MessageEntity message) {

        if (message == null) {
            return;
        }

        /*
         * Save locally first.
         *
         * This is important for ResilientNet's
         * offline-first architecture.
         */
        messageRepository.insertMessage(
                message
        );

        try {

            String serializedMessage =
                    MessageSerializer.serialize(
                            message
                    );

            Log.d(
                    TAG,
                    "Sending message over Bluetooth: "
                            + serializedMessage
            );

            if (bluetoothService.getActiveConnections().isEmpty()) {
                Log.w(
                        TAG,
                        "No active peer connections available. Triggering AutoMesh emergency scan..."
                );
                autoMeshManager.triggerEmergencyScan();
            }

            bluetoothService.sendMessage(
                    serializedMessage
            );

            if (internetGatewayManager != null && internetGatewayManager.isGatewayOnline()) {
                Log.d(
                        TAG,
                        "Device is online gateway. Triggering cloud sync for newly dispatched message..."
                );
                internetGatewayManager.triggerManualSync(null);
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to serialize message",
                    e
            );
        }
    }

    // ---------------------------------------------------------
    // RECEIVE
    // ---------------------------------------------------------

    @Override
    public void onMessageReceived(
            BluetoothDevice device,
            String message) {
        Log.d(
                TAG,
                ">>> CommunicationManager.onMessageReceived() CALLED <<<"
        );
        Log.d(
                TAG,
                "Received Bluetooth message from: "
                        + device.getAddress()
        );

        try {

            MessageEntity receivedMessage =
                    MessageSerializer.deserialize(message);
            Log.d(
                    TAG,
                    ">>> CommunicationManager deserialized message: "
                            + receivedMessage.getMessageId()
                            + " type="
                            + receivedMessage.getMessageType()
                            + " <<<"
            );
            if (receivedMessage.getMessageId() == null) {
                return;
            }

            // -------------------------------------------------
            // 1. Check maximum hop count
            // -------------------------------------------------

            if (receivedMessage.getHopCount() >= MAX_HOPS) {

                Log.d(
                        TAG,
                        "Maximum hop count reached: "
                                + receivedMessage.getMessageId()
                );

                return;
            }

            // -------------------------------------------------
            // 2. Store only if this is a new message
            // -------------------------------------------------

            messageRepository.insertReceivedMessage(
                    receivedMessage,
                    new MessageRepository.MessageReceiveCallback() {

                        @Override
                        public void onMessageProcessed(
                                boolean isNewMessage) {
                            Log.d(
                                    TAG,
                                    "Repository callback: isNewMessage="
                                            + isNewMessage
                                            + ", messageId="
                                            + receivedMessage.getMessageId()
                                            + ", type="
                                            + receivedMessage.getMessageType()
                            );
                            // Duplicate message
                            if (!isNewMessage) {

                                Log.d(
                                        TAG,
                                        "Duplicate message. "
                                                + "Not forwarding: "
                                                + receivedMessage.getMessageId()
                                );

                                return;
                            }

                            // -------------------------------------------------
                            // 2.1. If SOS, log emergency and store in SosDao
                            // -------------------------------------------------
                            if (MessageConstants.SOS.equals(receivedMessage.getMessageType())) {
                                Log.w(
                                        TAG,
                                        ">>> EMERGENCY SOS RECEIVED <<< [ID="
                                                + receivedMessage.getMessageId()
                                                + ", Sender="
                                                + receivedMessage.getSenderId()
                                                + ", Priority="
                                                + receivedMessage.getPriority()
                                                + ", Lat="
                                                + receivedMessage.getLatitude()
                                                + ", Lng="
                                                + receivedMessage.getLongitude()
                                                + ", Hop="
                                                + receivedMessage.getHopCount()
                                                + ", Content="
                                                + receivedMessage.getContent()
                                                + "]"
                                );

                                if (sosDao != null) {
                                    try {
                                        String content = receivedMessage.getContent();
                                        String emergencyType = "SOS Emergency";
                                        String sosDesc = "";
                                        if (content != null) {
                                            if (content.contains(": ")) {
                                                String[] parts = content.split(": ", 2);
                                                emergencyType = parts[0];
                                                sosDesc = parts[1];
                                            } else {
                                                emergencyType = content;
                                            }
                                        }

                                        double lat = receivedMessage.getLatitude() != null ? receivedMessage.getLatitude() : 0.0;
                                        double lng = receivedMessage.getLongitude() != null ? receivedMessage.getLongitude() : 0.0;

                                        java.text.SimpleDateFormat sdf =
                                                new java.text.SimpleDateFormat(
                                                        "dd MMM yyyy • hh:mm a",
                                                        java.util.Locale.getDefault()
                                                );
                                        String timestampStr = sdf.format(new java.util.Date(receivedMessage.getTimestamp()));

                                        String locName = (lat != 0.0 || lng != 0.0)
                                                ? String.format(java.util.Locale.US, "Lat: %.5f, Lng: %.5f", lat, lng)
                                                : "Location Unknown";

                                        SosEntity receivedSos = new SosEntity(
                                                emergencyType,
                                                sosDesc,
                                                locName,
                                                lat,
                                                lng,
                                                timestampStr,
                                                receivedMessage.getStatus() != null ? receivedMessage.getStatus() : MessageConstants.PENDING,
                                                false
                                        );

                                        sosDao.insertSos(receivedSos);
                                        Log.d(
                                                TAG,
                                                "Persisted received SOS into local sos_requests table: "
                                                        + receivedMessage.getMessageId()
                                        );

                                        if (internetGatewayManager != null && internetGatewayManager.isGatewayOnline()) {
                                            Log.d(
                                                    TAG,
                                                    "Gateway online! Immediately syncing received mesh SOS to cloud: "
                                                            + receivedMessage.getMessageId()
                                            );
                                            internetGatewayManager.triggerManualSync(null);
                                        }
                                    } catch (Exception e) {
                                        Log.e(
                                                TAG,
                                                "Failed to persist received SOS into sos_requests",
                                                e
                                        );
                                    }

                                    // Trigger tactical audio siren, SOS haptics, and heads-up alert
                                    EmergencyAlarmManager.getInstance(context).triggerAlarm(receivedMessage);
                                }
                            }

                            // -------------------------------------------------
                            // 3. Increase hop count
                            // -------------------------------------------------

                            receivedMessage.setHopCount(
                                    receivedMessage.getHopCount() + 1
                            );

                            // -------------------------------------------------
                            // 4. Forward the message
                            // -------------------------------------------------
                            Log.d(
                                    TAG,
                                    "Active connections available for forwarding: "
                                            + bluetoothService.getActiveConnections().size()
                            );

                            try {

                                String forwardedMessage =
                                        MessageSerializer.serialize(
                                                receivedMessage
                                        );

                                Log.d(
                                        TAG,
                                        "Forwarding message. Hop count = "
                                                + receivedMessage.getHopCount()
                                );

                                for (BluetoothConnectionThread connection :
                                        bluetoothService.getActiveConnections()) {

                                    BluetoothDevice targetDevice =
                                            connection.getDevice();

                                    /*
                                     * Do not send the message back to the
                                     * device from which we received it.
                                     */
                                    if (targetDevice.getAddress()
                                            .equals(device.getAddress())) {

                                        Log.d(
                                                TAG,
                                                "Skipping sender device: "
                                                        + targetDevice.getAddress()
                                        );

                                        continue;
                                    }

                                    Log.d(
                                            TAG,
                                            "Forwarding message to: "
                                                    + targetDevice.getAddress()
                                    );

                                    connection.sendMessage(
                                            forwardedMessage
                                    );
                                }

                            } catch (Exception e) {

                                Log.e(
                                        TAG,
                                        "Failed to forward message",
                                        e
                                );
                            }
                        }
                    }
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Invalid ResilientNet message",
                    e
            );
        }
    }

    @Override
    public void onConnecting(
            BluetoothDevice device) {

        Log.d(
                TAG,
                "Connecting to "
                        + device.getAddress()
        );
    }

    @Override
    public void onConnected(
            BluetoothDevice device) {

        Log.d(
                TAG,
                "Connected to "
                        + device.getAddress()
        );

        // Store-and-forward: flush any locally queued pending messages
        forwardPendingMessages();
    }

    private void forwardPendingMessages() {

        messageRepository.getPendingMessages(pendingMessages -> {

            if (pendingMessages == null || pendingMessages.isEmpty()) {
                return;
            }

            Log.d(
                    TAG,
                    "Peer connected! Dispatching "
                            + pendingMessages.size()
                            + " stored pending messages into mesh..."
            );

            for (MessageEntity pending : pendingMessages) {

                try {

                    String serializedMessage =
                            MessageSerializer.serialize(
                                    pending
                            );

                    Log.d(
                            TAG,
                            "Dispatching pending message: "
                                    + pending.getMessageId()
                    );

                    bluetoothService.sendMessage(
                            serializedMessage
                    );

                } catch (Exception e) {

                    Log.e(
                            TAG,
                            "Failed to serialize pending message",
                            e
                    );
                }
            }
        });
    }

    @Override
    public void onConnectionFailed(
            BluetoothDevice device,
            Exception exception) {

        Log.e(
                TAG,
                "Connection failed",
                exception
        );
    }

    @Override
    public void onDisconnected(
            BluetoothDevice device) {

        Log.d(
                TAG,
                "Disconnected from "
                        + device.getAddress()
        );
    }

    public void shutdown() {

        if (internetGatewayManager != null) {
            internetGatewayManager.stopGatewayMonitor();
        }

        if (backendSyncManager != null) {
            backendSyncManager.shutdown();
        }

        if (autoMeshManager != null) {
            autoMeshManager.stopAutoMesh();
        }

        bluetoothService.unregisterObserver(
                this
        );
    }

    public InternetGatewayManager getInternetGatewayManager() {
        return internetGatewayManager;
    }

    public BackendSyncManager getBackendSyncManager() {
        return backendSyncManager;
    }

    public AutoMeshManager getAutoMeshManager() {
        return autoMeshManager;
    }

    public BluetoothConnectionService getBluetoothService() {
        return bluetoothService;
    }

    public void startOrRefreshMesh() {
        if (bluetoothService != null) {
            bluetoothService.startServer();
        }
        if (autoMeshManager != null) {
            autoMeshManager.connectToBondedDevices();
        }
    }
}