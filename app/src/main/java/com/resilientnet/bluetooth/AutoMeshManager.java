package com.resilientnet.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.resilientnet.bluetooth.model.BluetoothDeviceModel;
import com.resilientnet.permissions.BluetoothPermissionHelper;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Autonomous Mesh Connection Manager for ResilientNet.
 *
 * Automatically forms and maintains an ad-hoc Bluetooth mesh without requiring
 * manual user intervention during emergencies.
 *
 * 1. Automatically connects to known/bonded devices on launch.
 * 2. Runs periodic discovery sweeps to discover nearby ResilientNet nodes.
 * 3. Triggers emergency discovery bursts when an SOS packet is generated.
 */
public class AutoMeshManager implements BluetoothDiscoveryListener, BluetoothConnectionObserver {

    private static final String TAG = "ResilientNetAutoMesh";

    private final Context context;
    private final BluetoothConnectionService bluetoothService;
    private final BluetoothManager bluetoothManager;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // Cooldown map to avoid spamming connection attempts to unreachable devices
    private final Map<String, Long> recentAttempts = new ConcurrentHashMap<>();

    private static final long RETRY_COOLDOWN_MS = 10_000; // 10 seconds cooldown per device
    private static final long MESH_MAINTENANCE_INTERVAL_MS = 12_000; // Check mesh every 12 seconds
    private static final int TARGET_ACTIVE_CONNECTIONS = 2; // Desired active peer links

    private boolean isRunning = false;

    public AutoMeshManager(
            Context context,
            BluetoothConnectionService bluetoothService) {

        this.context = context.getApplicationContext();
        this.bluetoothService = bluetoothService;
        this.bluetoothManager = new BluetoothManager(this.context);
    }

    /**
     * Start the autonomous mesh engine.
     */
    public synchronized void startAutoMesh() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        Log.d(TAG, "Starting AutoMesh Manager...");

        // Register as Bluetooth connection observer for reactive self-healing
        bluetoothService.registerObserver(this);

        // 1. Immediately connect to all bonded/paired ResilientNet devices
        connectToBondedDevices();

        // 2. Schedule recurring mesh health maintenance
        scheduleMaintenance();
    }

    /**
     * Stop the autonomous mesh engine.
     */
    public synchronized void stopAutoMesh() {
        isRunning = false;
        bluetoothService.unregisterObserver(this);
        handler.removeCallbacksAndMessages(null);
        recentAttempts.clear();
        Log.d(TAG, "Stopped AutoMesh Manager");
    }

    /**
     * Periodic heartbeat: verifies mesh health and reconnects if peer link dropped.
     */
    private void scheduleMaintenance() {
        if (!isRunning) {
            return;
        }

        handler.postDelayed(() -> {
            if (!isRunning) {
                return;
            }

            int activeCount = bluetoothService.getActiveConnections().size();
            Log.d(TAG, "Mesh heartbeat check. Active connections: " + activeCount);

            // Re-verify links to all bonded mesh peers
            connectToBondedDevices();

            scheduleMaintenance();
        }, MESH_MAINTENANCE_INTERVAL_MS);
    }

    /**
     * Filters out non-peer hardware (Bluetooth earbuds, headphones, speakers, smart TVs,
     * car audio kits, smartwatches) to prevent radio saturation and socket timeouts.
     */
    public boolean isCandidateMeshDevice(BluetoothDevice device) {
        if (device == null) {
            return false;
        }

        if (!hasConnectPermission()) {
            return false;
        }

        try {
            // 1. Device class filtering
            android.bluetooth.BluetoothClass btClass = device.getBluetoothClass();
            if (btClass != null) {
                int majorClass = btClass.getMajorDeviceClass();
                if (majorClass == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO
                        || majorClass == android.bluetooth.BluetoothClass.Device.Major.PERIPHERAL
                        || majorClass == android.bluetooth.BluetoothClass.Device.Major.WEARABLE
                        || majorClass == android.bluetooth.BluetoothClass.Device.Major.IMAGING
                        || majorClass == android.bluetooth.BluetoothClass.Device.Major.TOY
                        || majorClass == android.bluetooth.BluetoothClass.Device.Major.HEALTH
                        || majorClass == android.bluetooth.BluetoothClass.Device.Major.COMPUTER) {
                    return false;
                }
            }

            // 2. Name keyword filtering for accessories that might misreport class
            String name = device.getName();
            if (name != null) {
                String lower = name.toLowerCase();
                String[] nonMeshKeywords = {
                        "buds", "airpod", "airpods", "airdopes", "headset", "headphone",
                        "speaker", "tv", "television", "soundbar", "earphone", "car kit",
                        "handsfree", "audio", "tws", "smartwatch", "watch", "band", "music",
                        "laptop", "desktop", "pc", "macbook", "windows"
                };
                for (String keyword : nonMeshKeywords) {
                    if (lower.contains(keyword)) {
                        return false;
                    }
                }
            }

            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error checking device candidate", e);
            return false;
        }
    }

    /**
     * Automatically attempts connection to all bonded/paired devices.
     * Uses deterministic role assignment (lexicographical name comparison) to prevent
     * simultaneous RFCOMM connection paging collision between peers.
     */
    public void connectToBondedDevices() {
        BluetoothAdapter adapter = bluetoothManager.getBluetoothAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            return;
        }

        if (!hasConnectPermission()) {
            Log.w(TAG, "Cannot connect to bonded devices: missing BLUETOOTH_CONNECT permission");
            return;
        }

        if (bluetoothService.getActiveConnections().size() >= TARGET_ACTIVE_CONNECTIONS) {
            Log.d(TAG, "Already reached target active peer links (" + TARGET_ACTIVE_CONNECTIONS + "). Skipping connection sweep.");
            return;
        }

        try {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            if (bondedDevices == null || bondedDevices.isEmpty()) {
                return;
            }

            String myName = adapter.getName();

            for (BluetoothDevice device : bondedDevices) {
                // Filter out non-peer accessories (earbuds, speakers, TVs, smartwatches)
                if (!isCandidateMeshDevice(device)) {
                    continue;
                }

                if (shouldAttemptConnection(device.getAddress())) {
                    String remoteName = device.getName();
                    int cmp = 0;
                    if (myName != null && remoteName != null) {
                        cmp = myName.compareToIgnoreCase(remoteName);
                    }

                    int delayMs;
                    if (cmp > 0) {
                        // Designated Primary Initiator: initiate connection quickly
                        delayMs = 150 + Math.abs(device.getAddress().hashCode() % 250);
                        Log.d(TAG, "Role: Primary Initiator -> connecting to " + remoteName + " in " + delayMs + "ms");
                    } else if (cmp < 0) {
                        // Designated Primary Listener: wait for incoming connection, fallback after 5s
                        delayMs = 5000 + Math.abs(device.getAddress().hashCode() % 500);
                        Log.d(TAG, "Role: Primary Listener -> waiting for " + remoteName + ", fallback in " + delayMs + "ms");
                    } else {
                        // Tie breaker using address hash jitter
                        delayMs = 200 + Math.abs(device.getAddress().hashCode() % 1500);
                    }

                    handler.postDelayed(() -> {
                        if (!isRunning) {
                            return;
                        }

                        if (shouldAttemptConnection(device.getAddress())) {
                            Log.d(
                                    TAG,
                                    "Auto-connecting to bonded mesh peer: "
                                            + device.getName()
                                            + " ["
                                            + device.getAddress()
                                            + "]"
                            );
                            recentAttempts.put(device.getAddress(), System.currentTimeMillis());
                            bluetoothService.connectToDevice(device);
                        }
                    }, delayMs);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error accessing bonded devices", e);
        }
    }

    /**
     * Called when an emergency SOS is dispatched: triggers an immediate connection sweep to bonded peers.
     */
    public void triggerEmergencyScan() {
        Log.w(TAG, ">>> EMERGENCY SCAN TRIGGERED: Connecting to bonded mesh peers immediately <<<");
        recentAttempts.clear();
        BluetoothAdapter adapter = bluetoothManager.getBluetoothAdapter();
        if (adapter == null || !adapter.isEnabled() || !hasConnectPermission()) {
            return;
        }

        try {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            if (bondedDevices == null) {
                return;
            }

            for (BluetoothDevice device : bondedDevices) {
                if (isCandidateMeshDevice(device) && shouldAttemptConnection(device.getAddress())) {
                    int jitterMs = Math.abs(device.getAddress().hashCode() % 200);
                    handler.postDelayed(() -> {
                        if (shouldAttemptConnection(device.getAddress())) {
                            Log.d(TAG, "Emergency connecting to mesh peer: " + device.getName());
                            recentAttempts.put(device.getAddress(), System.currentTimeMillis());
                            bluetoothService.connectToDevice(device);
                        }
                    }, jitterMs);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error during emergency scan", e);
        }
    }

    /**
     * Discovered device callback.
     * Strictly ignores all unbonded ambient devices and non-peer hardware.
     */
    @Override
    public void onDeviceFound(BluetoothDeviceModel deviceModel) {
        if (deviceModel == null || deviceModel.getAddress() == null) {
            return;
        }

        String address = deviceModel.getAddress();
        if (!shouldAttemptConnection(address)) {
            return;
        }

        BluetoothAdapter adapter = bluetoothManager.getBluetoothAdapter();
        if (adapter == null) {
            return;
        }

        try {
            BluetoothDevice remoteDevice = adapter.getRemoteDevice(address);

            /*
             * CRITICAL: Do NOT attempt connection to unbonded ambient devices
             * (smart TVs, headphones, smartwatches). Only connect if bonded.
             */
            if (remoteDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                return;
            }

            if (!isCandidateMeshDevice(remoteDevice)) {
                return;
            }

            Log.d(
                    TAG,
                    "AutoMesh connecting to bonded candidate node: "
                            + deviceModel.getName()
                            + " ["
                            + address
                            + "]"
            );

            recentAttempts.put(address, System.currentTimeMillis());
            bluetoothService.connectToDevice(remoteDevice);
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to device: " + address, e);
        }
    }

    /**
     * Checks whether an address should be targeted for connection.
     */
    private boolean shouldAttemptConnection(String address) {
        // Skip if already actively connected
        for (BluetoothConnectionThread connection : bluetoothService.getActiveConnections()) {
            if (connection.isConnectionAlive() && connection.getDevice() != null
                    && address.equalsIgnoreCase(connection.getDevice().getAddress())) {
                return false;
            }
        }

        // Skip if tried within recent cooldown period
        Long lastAttempt = recentAttempts.get(address);
        if (lastAttempt != null && (System.currentTimeMillis() - lastAttempt) < RETRY_COOLDOWN_MS) {
            return false;
        }

        return true;
    }

    // ---------------------------------------------------------
    // BLUETOOTH CONNECTION OBSERVER (SELF-HEALING)
    // ---------------------------------------------------------

    @Override
    public void onConnecting(BluetoothDevice device) {
        if (device != null) {
            Log.d(TAG, "Connecting to mesh node: " + device.getAddress());
        }
    }

    @Override
    public void onConnected(BluetoothDevice device) {
        if (device != null) {
            recentAttempts.remove(device.getAddress());
            Log.d(TAG, "Successfully connected to mesh node: " + device.getAddress());
        }
    }

    @Override
    public void onConnectionFailed(BluetoothDevice device, Exception exception) {
        if (device != null) {
            recentAttempts.put(device.getAddress(), System.currentTimeMillis());
            Log.w(TAG, "Connection failed to mesh node: " + device.getAddress()
                    + " (" + (exception != null ? exception.getMessage() : "unknown") + ")");
        }
    }

    @Override
    public void onDisconnected(BluetoothDevice device) {
        if (device != null) {
            recentAttempts.remove(device.getAddress());
            Log.w(TAG, "Mesh node disconnected: " + device.getAddress() + ". Scheduling auto-reconnect...");
            // Automatically attempt reconnect after 3 seconds to heal the mesh
            handler.postDelayed(() -> {
                if (isRunning) {
                    connectToBondedDevices();
                }
            }, 3000);
        }
    }

    @Override
    public void onMessageReceived(BluetoothDevice device, String message) {
        // Handled by CommunicationManager
    }

    @Override
    public void onDiscoveryStarted() {
        Log.d(TAG, "AutoMesh discovery sweep active");
    }

    @Override
    public void onDiscoveryFinished() {
        Log.d(TAG, "AutoMesh discovery sweep finished");
    }

    private boolean hasConnectPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}
