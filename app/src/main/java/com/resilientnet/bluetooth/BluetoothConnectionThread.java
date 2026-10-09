package com.resilientnet.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class BluetoothConnectionThread extends Thread {

    private static final String TAG =
            "ResilientNetBluetooth";

    private final BluetoothSocket socket;
    private final BluetoothDevice device;

    private final BluetoothConnectionListener connectionListener;
    private final BluetoothMessageListener messageListener;
    private final BluetoothConnectionManager connectionManager;
    private InputStream inputStream;
    private OutputStream outputStream;

    public BluetoothConnectionThread(
            BluetoothSocket socket,
            BluetoothConnectionListener connectionListener,
            BluetoothMessageListener messageListener,
            BluetoothConnectionManager connectionManager) {

        this.socket = socket;
        this.device = socket.getRemoteDevice();

        this.connectionListener =
                connectionListener;

        this.messageListener =
                messageListener;

        this.connectionManager =
                connectionManager;
    }

    @Override
    public void run() {

        try {

            inputStream =
                    socket.getInputStream();

            outputStream =
                    socket.getOutputStream();

            Log.d(
                    TAG,
                    "RFCOMM streams opened"
            );

            if (connectionListener != null) {

                connectionListener.onConnected(
                        device
                );
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    StandardCharsets.UTF_8
                            )
                    );

            while (!isInterrupted()
                    && socket.isConnected()) {

                String message =
                        reader.readLine();

                if (message == null) {
                    break;
                }

                Log.d(
                        TAG,
                        "Received message: "
                                + message
                );

                if (messageListener != null) {
                    messageListener.onMessageReceived(device, message);
                }
            }

        } catch (IOException | SecurityException e) {

            if (!isInterrupted()) {

                Log.e(
                        TAG,
                        "Connection error: "
                                + e.getMessage(),
                        e
                );

                if (connectionListener != null) {

                    connectionListener.onConnectionFailed(
                            device,
                            e
                    );
                }
            }

        } finally {

            closeSocket();

            if (connectionManager != null) {

                connectionManager.removeConnection(
                        this
                );
            }

            if (connectionListener != null) {

                connectionListener.onDisconnected(
                        device
                );
            }
        }
    }

    public synchronized void sendMessage(
            String message) throws IOException {

        if (outputStream == null) {

            throw new IOException(
                    "Bluetooth output stream is not available"
            );
        }

        if (message == null) {
            return;
        }

        String formattedMessage =
                message + "\n";

        outputStream.write(
                formattedMessage.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        outputStream.flush();

        Log.d(
                TAG,
                "Sent message: "
                        + message
        );
    }

    public BluetoothDevice getDevice() {

        return device;
    }

    public boolean isConnectionAlive() {

        return socket != null
                && socket.isConnected()
                && isAlive();
    }

    public void cancel() {

        interrupt();

        closeSocket();
    }

    private void closeSocket() {

        try {

            socket.close();

        } catch (IOException ignored) {
        }

        inputStream = null;
        outputStream = null;
    }
}