package com.resilientnet.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.WpsInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.local.entity.MessageEntity;
import com.resilientnet.data.local.entity.SosEntity;
import com.resilientnet.data.model.MessageConstants;
import com.resilientnet.data.model.MessageSerializer;
import com.resilientnet.emergency.EmergencyAlarmManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WifiDirectManager {

    private static volatile WifiDirectManager instance;

    private static final String TAG = "WifiDirectManager";
    public static final int TCP_PORT = 8888;

    private final Context context;
    private final WifiP2pManager p2pManager;
    private final WifiP2pManager.Channel channel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final StringBuilder logHistory = new StringBuilder();

    private WifiDirectListener listener;
    private BroadcastReceiver receiver;
    private boolean isReceiverRegistered = false;

    private ServerSocket serverSocket;
    private String currentGroupOwnerIp = null;
    private String peerClientIp = null;
    private boolean isGroupOwner = false;
    private boolean isConnected = false;
    private boolean isAutoConnecting = false;
    private boolean isConnecting = false;
    private WifiP2pDevice thisDevice = null;

    public interface WifiDirectListener {
        void onStatusChanged(String status);
        void onPeersDiscovered(List<WifiP2pDevice> peers);
        void onConnected(boolean isGroupOwner, String groupOwnerAddress);
        void onDisconnected();
        void onMessageReceived(MessageEntity message);
        void onLog(String logEntry);
    }

    public static synchronized WifiDirectManager getInstance(Context context) {
        if (instance == null) {
            instance = new WifiDirectManager(context.getApplicationContext());
        }
        return instance;
    }

    public WifiDirectManager(Context context) {
        this(context, null);
    }

    public WifiDirectManager(Context context, WifiDirectListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;

        p2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (p2pManager != null) {
            channel = p2pManager.initialize(this.context, context.getMainLooper(), null);
        } else {
            channel = null;
        }
    }

    public void setListener(WifiDirectListener listener) {
        this.listener = listener;
    }

    public String getLogHistory() {
        synchronized (logHistory) {
            return logHistory.toString();
        }
    }

    public boolean isSupported() {
        return p2pManager != null && channel != null;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isGroupOwner() {
        return isGroupOwner;
    }

    public String getCurrentGroupOwnerIp() {
        return currentGroupOwnerIp;
    }

    public String getTargetIp() {
        return isGroupOwner ? peerClientIp : currentGroupOwnerIp;
    }

    // ---------------------------------------------------------
    // REGISTRATION
    // ---------------------------------------------------------

    public void register() {
        if (isReceiverRegistered || p2pManager == null || channel == null) {
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    boolean isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED;
                    log("Wi-Fi Direct radio state: " + (isEnabled ? "ENABLED" : "DISABLED"));
                    if (!isEnabled) {
                        notifyStatus("Wi-Fi Direct is disabled. Please turn ON Wi-Fi.");
                    }
                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    handlePeersChanged();
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    handleConnectionChanged(intent);
                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                    WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                    if (device != null) {
                        thisDevice = device;
                        log("This device: " + device.deviceName + " [" + getDeviceStatusName(device.status) + "]");
                    }
                }
            }
        };

        try {
            context.registerReceiver(receiver, filter);
            isReceiverRegistered = true;
            log("Wi-Fi Direct receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Error registering receiver", e);
        }
    }

    public void unregister() {
        if (isReceiverRegistered && receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Exception ignored) {
            }
            isReceiverRegistered = false;
        }
    }

    // ---------------------------------------------------------
    // DISCOVERY & AUTO-LINK
    // ---------------------------------------------------------

    public void startDiscovery() {
        if (!isSupported()) {
            notifyStatus("Wi-Fi Direct not supported on this device");
            return;
        }

        register();
        notifyStatus("Scanning for Wi-Fi Direct peers...");
        log("Scanning for nearby peers...");

        try {
            p2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    log("Peer discovery initiated successfully");
                }

                @Override
                public void onFailure(int reason) {
                    String error = getFailureReason(reason);
                    log("Discovery note: " + error);
                    notifyStatus("Discovery note: " + error);
                }
            });
        } catch (SecurityException se) {
            log("Missing Wi-Fi/Location permission");
            notifyStatus("Permission required for Wi-Fi Direct");
        }
    }

    public void startAutoLink() {
        if (!isSupported()) {
            notifyStatus("Wi-Fi Direct not supported on this device");
            return;
        }

        register();
        isAutoConnecting = true;
        notifyStatus("Scanning for Wi-Fi Direct peers...");
        log("Starting auto-link discovery...");

        // Clear previous group only if one was active
        if (isConnected) {
            try {
                p2pManager.removeGroup(channel, null);
            } catch (Exception ignored) {
            }
        }

        mainHandler.postDelayed(() -> {
            try {
                p2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        log("Peer discovery initiated successfully");
                    }

                    @Override
                    public void onFailure(int reason) {
                        isAutoConnecting = false;
                        String error = getFailureReason(reason);
                        log("Discovery note: " + error);
                        notifyStatus("Discovery note: " + error);
                    }
                });
            } catch (SecurityException se) {
                isAutoConnecting = false;
                log("Missing Wi-Fi/Location permission");
                notifyStatus("Permission required for Wi-Fi Direct");
            }
        }, 200);
    }

    private boolean shouldInitiateConnection(WifiP2pDevice targetPeer) {
        if (thisDevice == null || targetPeer == null) return true;

        if (thisDevice.deviceAddress != null && targetPeer.deviceAddress != null
                && !thisDevice.deviceAddress.equalsIgnoreCase(targetPeer.deviceAddress)) {
            return thisDevice.deviceAddress.compareToIgnoreCase(targetPeer.deviceAddress) > 0;
        }

        if (thisDevice.deviceName != null && targetPeer.deviceName != null) {
            return thisDevice.deviceName.compareToIgnoreCase(targetPeer.deviceName) > 0;
        }

        return true;
    }

    private void handlePeersChanged() {
        if (p2pManager == null || channel == null) return;

        try {
            p2pManager.requestPeers(channel, (WifiP2pDeviceList peerList) -> {
                List<WifiP2pDevice> peers = new ArrayList<>(peerList.getDeviceList());
                log("Discovered " + peers.size() + " peer(s) nearby");

                if (listener != null) {
                    mainHandler.post(() -> listener.onPeersDiscovered(peers));
                }

                if (isAutoConnecting && !isConnected && !isConnecting && !peers.isEmpty()) {
                    WifiP2pDevice targetPeer = null;
                    for (WifiP2pDevice d : peers) {
                        if (d.status == WifiP2pDevice.AVAILABLE) {
                            targetPeer = d;
                            break;
                        }
                    }

                    if (targetPeer == null) {
                        targetPeer = peers.get(0);
                    }

                    if (shouldInitiateConnection(targetPeer)) {
                        isAutoConnecting = false;
                        log("Auto-linking to peer: " + targetPeer.deviceName + " (" + targetPeer.deviceAddress + ")");
                        connectToPeer(targetPeer);
                    } else {
                        log("Listening for incoming invitation from: " + targetPeer.deviceName);
                        notifyStatus("Waiting for " + targetPeer.deviceName + " to connect...");
                    }
                }
            });
        } catch (SecurityException se) {
            log("SecurityException in requestPeers: " + se.getMessage());
        }
    }

    public void connectToPeer(WifiP2pDevice device) {
        if (p2pManager == null || channel == null || device == null) return;
        if (isConnecting || isConnected) {
            log("Connection already in progress or connected");
            return;
        }

        isConnecting = true;
        isAutoConnecting = false;

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        notifyStatus("Inviting " + device.deviceName + "...");
        log("Sending connection request to " + device.deviceName + " (Accept prompt on peer)...");

        try {
            p2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    log("Connection invitation sent! Tap ACCEPT on " + device.deviceName);
                    notifyStatus("Invitation sent! Tap Accept on " + device.deviceName);
                }

                @Override
                public void onFailure(int reason) {
                    isConnecting = false;
                    String err = getFailureReason(reason);
                    log("Connection attempt returned: " + err);

                    if (reason == WifiP2pManager.BUSY) {
                        log("Framework busy, retrying in 1.5s...");
                        mainHandler.postDelayed(() -> {
                            if (!isConnected && !isConnecting) {
                                connectToPeer(device);
                            }
                        }, 1500);
                    } else {
                        notifyStatus("Connect failed: " + err);
                    }
                }
            });
        } catch (SecurityException se) {
            isConnecting = false;
            log("SecurityException in connect: " + se.getMessage());
        }
    }

    // ---------------------------------------------------------
    // CONNECTION HANDLING
    // ---------------------------------------------------------

    private void handleConnectionChanged(Intent intent) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        if (networkInfo != null && networkInfo.isConnected()) {
            p2pManager.requestConnectionInfo(channel, (WifiP2pInfo info) -> {
                if (info != null && info.groupFormed) {
                    isConnected = true;
                    isAutoConnecting = false;
                    isGroupOwner = info.isGroupOwner;
                    currentGroupOwnerIp = info.groupOwnerAddress != null ? info.groupOwnerAddress.getHostAddress() : "192.168.49.1";

                    String role = isGroupOwner ? "GROUP OWNER (AP)" : "CLIENT";
                    log("Group Formed! Role: " + role + " | GO IP: " + currentGroupOwnerIp);
                    notifyStatus("Connected (" + role + ") @ " + currentGroupOwnerIp);

                    // Both Group Owner and Client run TCP Server so either can receive
                    startTcpServer();

                    if (listener != null) {
                        mainHandler.post(() -> listener.onConnected(isGroupOwner, currentGroupOwnerIp));
                    }

                    if (!isGroupOwner) {
                        // Client sends initial handshake to tell Group Owner its IP
                        mainHandler.postDelayed(this::sendHandshake, 800);
                    }
                }
            });
        } else {
            if (isConnected) {
                isConnected = false;
                isAutoConnecting = false;
                peerClientIp = null;
                closeSockets();
                log("Wi-Fi Direct disconnected");
                notifyStatus("Disconnected / Idle");
                if (listener != null) {
                    mainHandler.post(() -> listener.onDisconnected());
                }
            }
        }
    }

    private void sendHandshake() {
        executor.execute(() -> {
            try {
                Socket socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(currentGroupOwnerIp, TCP_PORT), 4000);
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                writer.println("WIFI_DIRECT_HANDSHAKE");
                writer.flush();
                socket.close();
                log("Handshake sent to Group Owner (" + currentGroupOwnerIp + ")");
            } catch (Exception e) {
                log("Handshake note: " + e.getMessage());
            }
        });
    }

    // ---------------------------------------------------------
    // TCP SOCKET DATA TRANSFER
    // ---------------------------------------------------------

    private void startTcpServer() {
        executor.execute(() -> {
            try {
                closeSockets();
                serverSocket = new ServerSocket(TCP_PORT);
                log("TCP Server listening on port " + TCP_PORT + " (Ready for messages & SOS)");

                while (isConnected && serverSocket != null && !serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    String incomingIp = socket.getInetAddress().getHostAddress();
                    if (isGroupOwner && incomingIp != null && !incomingIp.isEmpty()) {
                        peerClientIp = incomingIp;
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    String line = reader.readLine();
                    socket.close();

                    if (line != null) {
                        if ("WIFI_DIRECT_HANDSHAKE".equals(line)) {
                            log("Handshake received from peer @ " + incomingIp);
                        } else {
                            processReceivedPayload(line);
                        }
                    }
                }
            } catch (Exception e) {
                if (isConnected) {
                    log("TCP Server stopped: " + e.getMessage());
                }
            }
        });
    }

    private void processReceivedPayload(String rawPayload) {
        try {
            MessageEntity message = MessageSerializer.deserialize(rawPayload);
            log("Received " + message.getMessageType() + " from: " + message.getSenderId());

            // 1. Insert into local Room database
            AppDatabase.getInstance(context).messageDao().insertMessage(message);

            // 2. If SOS, save into SosDao and trigger siren alarm!
            if (MessageConstants.SOS.equals(message.getMessageType())) {
                String content = message.getContent();
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

                SosEntity sos = new SosEntity(
                        emergencyType,
                        sosDesc,
                        "Wi-Fi Direct P2P",
                        message.getLatitude() != null ? message.getLatitude() : 0.0,
                        message.getLongitude() != null ? message.getLongitude() : 0.0,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()),
                        "ACTIVE",
                        false
                );

                AppDatabase.getInstance(context).sosDao().insertSos(sos);

                // Trigger siren and haptics
                EmergencyAlarmManager.getInstance(context).triggerAlarm(message);
                log("🚨 SOS Alarm Siren Triggered on this device!");
            }

            if (listener != null) {
                mainHandler.post(() -> listener.onMessageReceived(message));
            }

        } catch (Exception e) {
            log("Received raw packet: " + rawPayload);
        }
    }

    // ---------------------------------------------------------
    // SEND MESSAGE / SOS PACKET
    // ---------------------------------------------------------

    public void sendMessagePacket(MessageEntity message, Runnable onSuccess, Runnable onFailure) {
        String targetIp = getTargetIp();
        if (!isConnected || targetIp == null) {
            log("Cannot send: Tunnel not ready or peer IP unknown");
            if (onFailure != null) mainHandler.post(onFailure);
            return;
        }

        executor.execute(() -> {
            try {
                // Save locally first so sender's Chat tab has the message
                AppDatabase.getInstance(context).messageDao().insertMessage(message);

                String jsonPayload = MessageSerializer.serialize(message);
                log("Transmitting " + message.getMessageType() + " via TCP to " + targetIp + ":" + TCP_PORT + "...");

                Socket socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(targetIp, TCP_PORT), 5000);

                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                writer.println(jsonPayload);
                writer.flush();
                socket.close();

                log("Packet delivered successfully over Wi-Fi Direct (250 Mbps)!");
                if (onSuccess != null) mainHandler.post(onSuccess);

            } catch (Exception e) {
                log("Transmit error: " + e.getMessage());
                Log.e(TAG, "Socket transmission error", e);
                if (onFailure != null) mainHandler.post(onFailure);
            }
        });
    }

    // ---------------------------------------------------------
    // DISCONNECT & CLEANUP
    // ---------------------------------------------------------

    public void disconnect() {
        isAutoConnecting = false;
        isConnecting = false;
        closeSockets();

        if (p2pManager != null && channel != null) {
            try {
                p2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        log("Wi-Fi Direct group removed");
                        isConnected = false;
                        peerClientIp = null;
                        notifyStatus("Disconnected / Idle");
                        if (listener != null) {
                            mainHandler.post(() -> listener.onDisconnected());
                        }
                    }

                    @Override
                    public void onFailure(int reason) {
                        log("Remove group note: " + getFailureReason(reason));
                        isConnected = false;
                        peerClientIp = null;
                        notifyStatus("Disconnected / Idle");
                        if (listener != null) {
                            mainHandler.post(() -> listener.onDisconnected());
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error removing group", e);
                isConnected = false;
                peerClientIp = null;
            }
        }
    }

    private void closeSockets() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
        serverSocket = null;
    }

    public void destroy() {
        disconnect();
        unregister();
        executor.shutdownNow();
    }

    // ---------------------------------------------------------
    // HELPERS & LOGGING
    // ---------------------------------------------------------

    private void notifyStatus(String status) {
        if (listener != null) {
            mainHandler.post(() -> listener.onStatusChanged(status));
        }
    }

    private void log(String message) {
        String timestamp = timeFormat.format(new Date());
        String logEntry = "[" + timestamp + "] " + message;
        Log.d(TAG, logEntry);
        synchronized (logHistory) {
            logHistory.append(logEntry).append("\n");
        }
        if (listener != null) {
            mainHandler.post(() -> listener.onLog(logEntry));
        }
    }

    private String getDeviceStatusName(int status) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE: return "Available";
            case WifiP2pDevice.INVITED: return "Invited";
            case WifiP2pDevice.CONNECTED: return "Connected";
            case WifiP2pDevice.FAILED: return "Failed";
            case WifiP2pDevice.UNAVAILABLE: return "Unavailable";
            default: return "Unknown (" + status + ")";
        }
    }

    private String getFailureReason(int reason) {
        switch (reason) {
            case WifiP2pManager.P2P_UNSUPPORTED: return "P2P Unsupported";
            case WifiP2pManager.ERROR: return "Internal Error";
            case WifiP2pManager.BUSY: return "Framework Busy";
            default: return "Code " + reason;
        }
    }
}
