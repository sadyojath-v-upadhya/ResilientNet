package com.resilientnet.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BluetoothClientThread extends Thread {

    private static final String TAG =
            "ResilientNetBluetooth";

    private static final UUID SERVICE_UUID =
            UUID.fromString(
                    "8f5d8c3a-7e3b-4c0a-9d5a-1b2c3d4e5f60"
            );

    private final Context context;
    private final BluetoothDevice device;

    private final BluetoothConnectionListener connectionListener;
    private final BluetoothMessageListener messageListener;

    private BluetoothSocket socket;

    private BluetoothConnectionThread connectionThread;
    private final BluetoothConnectionManager connectionManager;
    public BluetoothClientThread(
            Context context,
            BluetoothDevice device,
            BluetoothConnectionListener connectionListener,
            BluetoothMessageListener messageListener,
            BluetoothConnectionManager connectionManager) {

        this.context =
                context.getApplicationContext();

        this.device = device;

        this.connectionListener =
                connectionListener;

        this.messageListener =
                messageListener;

        this.connectionManager =
                connectionManager;
    }

    @Override
    public void run() {

        if (!hasConnectPermission()) {

            if (connectionListener != null) {

                connectionListener.onConnectionFailed(
                        device,
                        new SecurityException(
                                "Bluetooth connect permission not granted"
                        )
                );
            }

            return;
        }

        if (connectionListener != null) {

            connectionListener.onConnecting(
                    device
            );
        }

        try {

            Log.d(
                    TAG,
                    "Creating RFCOMM socket for: "
                            + device.getAddress()
            );

            socket =
                    device.createRfcommSocketToServiceRecord(
                            SERVICE_UUID
                    );

            BluetoothAdapter adapter =
                    BluetoothAdapter.getDefaultAdapter();

            if (adapter != null
                    && adapter.isDiscovering()) {

                adapter.cancelDiscovery();
            }

            Log.d(
                    TAG,
                    "Connecting to: "
                            + device.getAddress()
            );

            socket.connect();

            Log.d(
                    TAG,
                    "RFCOMM socket connected"
            );

            /*
             * -------------------------------------------------
             * ResilientNet handshake
             * -------------------------------------------------
             *
             * Server sends:
             *
             * RESILIENTNET_HELLO
             *
             * Client responds:
             *
             * RESILIENTNET_HELLO_ACK
             */

            InputStream inputStream =
                    socket.getInputStream();

            OutputStream outputStream =
                    socket.getOutputStream();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    StandardCharsets.UTF_8
                            )
                    );

            String message =
                    reader.readLine();

            Log.d(
                    TAG,
                    "Received from server: "
                            + message
            );

            /*
             * Verify server handshake.
             */
            if (!BluetoothProtocol.HELLO.equals(message)) {

                throw new IOException(
                        "Invalid ResilientNet handshake"
                );
            }

            /*
             * Send acknowledgement.
             */
            outputStream.write(
                    (
                            BluetoothProtocol.HELLO_ACK
                                    + "\n"
                    ).getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            outputStream.flush();

            Log.d(
                    TAG,
                    "Sent: "
                            + BluetoothProtocol.HELLO_ACK
            );

            Log.d(
                    TAG,
                    "ResilientNet handshake successful"
            );

            /*
             * -------------------------------------------------
             * Hand over socket to BluetoothConnectionThread
             * -------------------------------------------------
             *
             * From this point onward, the connection thread
             * handles application-level messages.
             */

            connectionThread =
                    new BluetoothConnectionThread(
                            socket,
                            connectionListener,
                            messageListener,
                            connectionManager
                    );

            connectionManager.registerConnection(
                    connectionThread
            );

            socket = null;

            connectionThread.start();

        } catch (IOException | SecurityException e) {

            Log.e(
                    TAG,
                    "Connection failed: "
                            + e.getMessage(),
                    e
            );

            closeSocket();

            if (connectionListener != null) {

                connectionListener.onConnectionFailed(
                        device,
                        e
                );
            }
        }
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    /**
     * Returns the active application-level
     * Bluetooth connection thread.
     */
    public BluetoothConnectionThread
    getConnectionThread() {

        return connectionThread;
    }

    /**
     * Cancel the client connection.
     */
    public void cancel() {

        interrupt();

        if (connectionThread != null) {

            connectionThread.cancel();

            connectionThread = null;
        }

        closeSocket();
    }

    /**
     * Safely close the RFCOMM socket owned
     * by this client thread.
     */
    private void closeSocket() {

        if (socket == null) {
            return;
        }

        try {

            socket.close();

        } catch (IOException ignored) {
        }

        socket = null;
    }

    /**
     * Check Bluetooth CONNECT permission
     * on Android 12+.
     */
    private boolean hasConnectPermission() {

        if (android.os.Build.VERSION.SDK_INT
                >= android.os.Build.VERSION_CODES.S) {

            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }
}