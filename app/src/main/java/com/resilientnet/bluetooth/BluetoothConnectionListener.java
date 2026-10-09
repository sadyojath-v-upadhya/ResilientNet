package com.resilientnet.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface BluetoothConnectionListener {

    void onConnecting(BluetoothDevice device);

    void onConnected(BluetoothDevice device);

    void onConnectionFailed(
            BluetoothDevice device,
            Exception exception
    );

    void onDisconnected(BluetoothDevice device);

    void onMessageReceived(
            BluetoothDevice device,
            String message
    );
}