package com.resilientnet.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BluetoothConnectionManager {

    private static final String TAG =
            "ResilientNetBluetooth";

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;

    private final BluetoothConnectionListener connectionListener;
    private final BluetoothMessageListener messageListener;

    private BluetoothServerThread serverThread;

    /*
     * Multiple client connections are now supported.
     */
    private final List<BluetoothClientThread> clientThreads =
            new CopyOnWriteArrayList<>();

    /*
     * Multiple active application-level connections.
     */
    private final List<BluetoothConnectionThread>
            connectionThreads =
            new CopyOnWriteArrayList<>();

    public BluetoothConnectionManager(
            Context context,
            BluetoothConnectionListener connectionListener,
            BluetoothMessageListener messageListener) {

        this.context =
                context.getApplicationContext();

        this.bluetoothAdapter =
                BluetoothAdapter.getDefaultAdapter();

        this.connectionListener =
                connectionListener;

        this.messageListener =
                messageListener;
    }

    // ---------------------------------------------------------
    // BLUETOOTH
    // ---------------------------------------------------------

    public boolean isBluetoothAvailable() {

        return bluetoothAdapter != null;
    }

    public boolean isConnected() {

        cleanupDeadConnections();

        return !connectionThreads.isEmpty();
    }

    // ---------------------------------------------------------
    // SERVER
    // ---------------------------------------------------------

    public synchronized void startServer() {

        if (bluetoothAdapter == null) {

            Log.e(
                    TAG,
                    "Bluetooth adapter is unavailable"
            );

            return;
        }

        if (serverThread != null && serverThread.isAlive()) {

            Log.d(
                    TAG,
                    "Bluetooth server is already running"
            );

            return;
        }

        Log.d(
                TAG,
                "Starting RFCOMM server"
        );

        serverThread =
                new BluetoothServerThread(
                        context,
                        bluetoothAdapter,
                        connectionListener,
                        messageListener,
                        this
                );

        serverThread.start();
    }

    // ---------------------------------------------------------
    // CLIENT
    // ---------------------------------------------------------

    public synchronized void connectToDevice(
            BluetoothDevice device) {

        if (device == null) {

            Log.e(
                    TAG,
                    "Cannot connect: device is null"
            );

            return;
        }

        cleanupDeadConnections();

        /*
         * Check whether we are already connected
         * to this device.
         */
        if (isConnectedToDevice(device)) {

            Log.d(
                    TAG,
                    "Already connected to: "
                            + device.getAddress()
            );

            return;
        }

        /*
         * Check whether a client thread is already
         * connecting to this device.
         */
        for (BluetoothClientThread client : clientThreads) {
            if (client.isAlive() && client.getDevice() != null
                    && client.getDevice().getAddress().equals(device.getAddress())) {
                Log.d(
                        TAG,
                        "Already connecting to: "
                                + device.getAddress()
                );
                return;
            }
        }

        Log.d(
                TAG,
                "Starting connection to: "
                        + device.getAddress()
        );

        BluetoothClientThread clientThread =
                new BluetoothClientThread(
                        context,
                        device,
                        connectionListener,
                        messageListener,
                        this
                );

        clientThreads.add(clientThread);

        clientThread.start();
    }

    // ---------------------------------------------------------
    // REGISTER CONNECTION
    // ---------------------------------------------------------

    /*
     * Called by the server/client side when a
     * BluetoothConnectionThread becomes available.
     */
    public void registerConnection(
            BluetoothConnectionThread connection) {

        if (connection == null) {
            return;
        }

        cleanupDeadConnections();

        /*
         * Verify we do not already hold an active connection
         * to this device address.
         */
        for (BluetoothConnectionThread existing : connectionThreads) {
            if (existing.isConnectionAlive() && existing.getDevice() != null
                    && existing.getDevice().getAddress().equals(connection.getDevice().getAddress())) {
                Log.d(
                        TAG,
                        "Already have active connection to: "
                                + connection.getDevice().getAddress()
                                + ". Closing redundant connection."
                );
                connection.cancel();
                return;
            }
        }

        if (!connectionThreads.contains(connection)) {

            connectionThreads.add(connection);

            Log.d(
                    TAG,
                    "REGISTERING CONNECTION: "
                            + connection.getDevice().getAddress()
            );

            Log.d(
                    TAG,
                    "Active Bluetooth connections: "
                            + connectionThreads.size()
            );
        }
    }

    // ---------------------------------------------------------
    // REMOVE CONNECTION
    // ---------------------------------------------------------

    public void removeConnection(
            BluetoothConnectionThread connection) {

        if (connection == null) {
            return;
        }

        connectionThreads.remove(connection);

        Log.d(
                TAG,
                "Active Bluetooth connections: "
                        + connectionThreads.size()
        );
    }

    // ---------------------------------------------------------
    // GET CONNECTIONS
    // ---------------------------------------------------------

    public List<BluetoothConnectionThread>
    getActiveConnections() {

        cleanupDeadConnections();

        return new ArrayList<>(
                connectionThreads
        );
    }

    // ---------------------------------------------------------
    // CHECK DEVICE CONNECTION
    // ---------------------------------------------------------

    private boolean isConnectedToDevice(
            BluetoothDevice device) {

        for (BluetoothConnectionThread connection :
                connectionThreads) {

            if (!connection.isConnectionAlive()) {
                continue;
            }

            try {

                BluetoothDevice connectedDevice =
                        connection.getDevice();

                if (connectedDevice != null
                        && connectedDevice.getAddress()
                        .equals(device.getAddress())) {

                    return true;
                }

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Unable to check connected device",
                        e
                );
            }
        }

        return false;
    }

    // ---------------------------------------------------------
    // CLEANUP DEAD CONNECTIONS
    // ---------------------------------------------------------

    private void cleanupDeadConnections() {

        for (BluetoothConnectionThread connection :
                new ArrayList<>(connectionThreads)) {

            if (!connection.isConnectionAlive()) {

                connectionThreads.remove(
                        connection
                );
            }
        }

        for (BluetoothClientThread client :
                new ArrayList<>(clientThreads)) {

            if (!client.isAlive()) {

                clientThreads.remove(
                        client
                );
            }
        }
    }

    // ---------------------------------------------------------
    // SEND MESSAGE
    // ---------------------------------------------------------

    public void sendMessage(
            String message) {

        if (message == null
                || message.trim().isEmpty()) {

            return;
        }

        cleanupDeadConnections();
        Log.d(
                TAG,
                "Active connection count: "
                        + connectionThreads.size()
        );

        if (connectionThreads.isEmpty()) {

            Log.e(
                    TAG,
                    "Cannot send message: "
                            + "no active Bluetooth connections"
            );

            return;
        }

        /*
         * Send to all currently connected devices.
         *
         * This is important for the future
         * multi-hop architecture.
         */
        for (BluetoothConnectionThread connection :
                connectionThreads) {

            if (!connection.isConnectionAlive()) {
                continue;
            }

            try {

                Log.d(
                        TAG,
                        "Sending message to: "
                                + connection
                                .getDevice()
                                .getAddress()
                );

                connection.sendMessage(
                        message
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Failed to send message",
                        e
                );
            }
        }
    }

    // ---------------------------------------------------------
    // STOP SERVER
    // ---------------------------------------------------------

    public synchronized void stopServer() {

        if (serverThread == null) {
            return;
        }

        Log.d(
                TAG,
                "Stopping RFCOMM server"
        );

        serverThread.cancel();

        serverThread = null;
    }

    // ---------------------------------------------------------
    // STOP ALL CLIENTS
    // ---------------------------------------------------------

    public synchronized void stopClients() {

        Log.d(
                TAG,
                "Stopping all Bluetooth clients"
        );

        for (BluetoothClientThread client :
                clientThreads) {

            client.cancel();
        }

        clientThreads.clear();
    }

    // ---------------------------------------------------------
    // DISCONNECT ALL
    // ---------------------------------------------------------

    public synchronized void disconnect() {

        Log.d(
                TAG,
                "Disconnecting all Bluetooth connections"
        );

        /*
         * Stop client connections.
         */
        for (BluetoothClientThread client :
                clientThreads) {

            client.cancel();
        }

        clientThreads.clear();

        /*
         * Stop server.
         */
        if (serverThread != null) {

            serverThread.cancel();

            serverThread = null;
        }

        /*
         * Stop active application-level
         * connections.
         */
        for (BluetoothConnectionThread connection :
                connectionThreads) {

            connection.cancel();
        }

        connectionThreads.clear();
    }

    // ---------------------------------------------------------
    // SHUTDOWN
    // ---------------------------------------------------------

    public void shutdown() {

        Log.d(
                TAG,
                "Shutting down Bluetooth connection manager"
        );

        disconnect();
    }
}