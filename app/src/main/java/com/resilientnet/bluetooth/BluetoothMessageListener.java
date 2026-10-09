package com.resilientnet.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface BluetoothMessageListener {

    void onMessageReceived(
            BluetoothDevice device,
            String message
    );
}