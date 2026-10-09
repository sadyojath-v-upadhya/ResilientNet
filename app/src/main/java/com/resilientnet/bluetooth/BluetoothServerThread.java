package com.resilientnet.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BluetoothServerThread extends Thread {

    private static final String TAG =
            "ResilientNetBluetooth";

    private static final String SERVICE_NAME =
            "ResilientNet";

    private static final UUID SERVICE_UUID =
            UUID.fromString(
                    "8f5d8c3a-7e3b-4c0a-9d5a-1b2c3d4e5f60"
            );

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;

    private final BluetoothConnectionListener connectionListener;
    private final BluetoothMessageListener messageListener;

    private BluetoothServerSocket serverSocket;
    private final BluetoothConnectionManager connectionManager;
    private BluetoothConnectionThread connectionThread;

    public BluetoothServerThread(
            Context context,
            BluetoothAdapter bluetoothAdapter,
            BluetoothConnectionListener connectionListener,
            BluetoothMessageListener messageListener,
            BluetoothConnectionManager connectionManager) {

        this.context =
                context.getApplicationContext();

        this.bluetoothAdapter =
                bluetoothAdapter;

        this.connectionListener =
                connectionListener;

        this.messageListener =
                messageListener;

        this.connectionManager = connectionManager;
    }

    @Override
    public void run() {

        if (bluetoothAdapter == null) {

            Log.e(
                    TAG,
                    "Bluetooth adapter is null"
            );

            return;
        }

        if (!hasConnectPermission()) {

            Log.e(
                    TAG,
                    "Bluetooth connect permission not granted"
            );

            return;
        }

        try {

            Log.d(
                    TAG,
                    "Creating RFCOMM server socket"
            );

            serverSocket =
                    bluetoothAdapter
                            .listenUsingRfcommWithServiceRecord(
                                    SERVICE_NAME,
                                    SERVICE_UUID
                            );

            Log.d(
                    TAG,
                    "RFCOMM server is listening"
            );

            /*
             * Continuously accept incoming connections so multiple nodes
             * (e.g. Realme and Samsung simultaneously on Moto) can connect.
             */
            while (!isInterrupted()) {
                BluetoothSocket socket;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    if (isInterrupted()) {
                        Log.d(TAG, "RFCOMM server socket closed (interrupted)");
                    } else {
                        Log.d(TAG, "RFCOMM server accept ended: " + e.getMessage());
                    }
                    break;
                }

                if (socket == null) {
                    continue;
                }

                Log.d(
                        TAG,
                        "RFCOMM connection accepted from: "
                                + (socket.getRemoteDevice() != null ? socket.getRemoteDevice().getAddress() : "unknown")
                );

                /*
                 * Handle handshake on a dedicated worker thread so the server
                 * immediately loops back to accept() for the next incoming node.
                 */
                final BluetoothSocket acceptedSocket = socket;
                new Thread(() -> handleAcceptedSocket(acceptedSocket), "ResilientNet-ServerWorker").start();
            }

        } catch (IOException | SecurityException e) {

            if (!isInterrupted()) {

                Log.e(
                        TAG,
                        "RFCOMM server error: "
                                + e.getMessage(),
                        e
                );
            }

        } finally {

            closeServerSocket();
        }
    }

    private void handleAcceptedSocket(BluetoothSocket socket) {
        try {
            /*
             * Send ResilientNet handshake.
             * Client expects HELLO first.
             */
            OutputStream outputStream =
                    socket.getOutputStream();

            outputStream.write(
                    (
                            BluetoothProtocol.HELLO
                                    + "\n"
                    ).getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            outputStream.flush();

            Log.d(
                    TAG,
                    "Sent: "
                            + BluetoothProtocol.HELLO
            );

            /*
             * The client must respond with HELLO_ACK.
             */
            java.io.BufferedReader reader =
                    new java.io.BufferedReader(
                            new java.io.InputStreamReader(
                                    socket.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    );

            String response =
                    reader.readLine();

            Log.d(
                    TAG,
                    "Received handshake: "
                            + response
            );

            if (!BluetoothProtocol.HELLO_ACK.equals(response)) {
                Log.e(
                        TAG,
                        "Invalid ResilientNet handshake response from "
                                + (socket.getRemoteDevice() != null ? socket.getRemoteDevice().getAddress() : "unknown")
                );
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                return;
            }

            Log.d(
                    TAG,
                    "ResilientNet handshake successful with "
                            + (socket.getRemoteDevice() != null ? socket.getRemoteDevice().getAddress() : "unknown")
            );

            /*
             * Give the connected socket to BluetoothConnectionThread.
             */
            BluetoothConnectionThread thread =
                    new BluetoothConnectionThread(
                            socket,
                            connectionListener,
                            messageListener,
                            connectionManager
                    );

            connectionThread = thread;

            connectionManager.registerConnection(
                    thread
            );

            thread.start();

        } catch (IOException | SecurityException e) {
            Log.e(
                    TAG,
                    "Accepted socket error: "
                            + e.getMessage(),
                    e
            );
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public BluetoothConnectionThread
    getConnectionThread() {

        return connectionThread;
    }

    public void cancel() {

        interrupt();

        if (connectionThread != null) {

            connectionThread.cancel();

            connectionThread = null;
        }

        closeServerSocket();
    }

    private void closeServerSocket() {

        if (serverSocket == null) {
            return;
        }

        try {

            serverSocket.close();

        } catch (IOException ignored) {
        }

        serverSocket = null;
    }

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