package com.resilientnet.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.resilientnet.bluetooth.model.BluetoothDeviceModel;

public class BluetoothDiscoveryReceiver extends BroadcastReceiver {

    private final BluetoothDiscoveryListener listener;

    public BluetoothDiscoveryReceiver(BluetoothDiscoveryListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null)
            return;

        switch (intent.getAction()) {

            case BluetoothDevice.ACTION_FOUND:

                BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device == null)
                    return;

                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_CONNECT)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                String name;

                if (device.getName() == null || device.getName().trim().isEmpty()) {
                    name = "Nearby Bluetooth Device";
                } else {
                    name = device.getName();
                }
                BluetoothDeviceModel model =
                        new BluetoothDeviceModel(
                                name,
                                device.getAddress()
                        );

                if (name == null || name.trim().isEmpty()) {
                    name = "Unknown Device";
                }

                listener.onDeviceFound(
                        new BluetoothDeviceModel(
                                name,
                                device.getAddress()
                        )
                );

                break;

            case android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED:

                listener.onDiscoveryStarted();

                break;

            case android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED:

                listener.onDiscoveryFinished();

                break;
        }
    }
}