package com.resilientnet.bluetooth;

import com.resilientnet.bluetooth.model.BluetoothDeviceModel;

public interface BluetoothDiscoveryListener {

    void onDeviceFound(BluetoothDeviceModel device);

    void onDiscoveryStarted();

    void onDiscoveryFinished();
}