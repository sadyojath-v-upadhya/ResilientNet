package com.resilientnet.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
public class BluetoothManager {

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothDiscoveryReceiver discoveryReceiver;
    private final Context context;
    public BluetoothManager(Context context) {

        this.context = context;

        android.bluetooth.BluetoothManager manager =
                (android.bluetooth.BluetoothManager)
                        context.getSystemService(Context.BLUETOOTH_SERVICE);

        if (manager != null) {
            bluetoothAdapter = manager.getAdapter();
        } else {
            bluetoothAdapter = null;
        }
    }

    private boolean hasScanPermission() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {

            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED;

        }

        return true;
    }

    public void registerDiscoveryReceiver(
            BluetoothDiscoveryListener listener
    ) {

        if (discoveryReceiver != null)
            return;

        discoveryReceiver = new BluetoothDiscoveryReceiver(listener);

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);

        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        context.registerReceiver(discoveryReceiver, filter);

    }


    public void unregisterDiscoveryReceiver() {

        if (discoveryReceiver == null)
            return;

        try {

            context.unregisterReceiver(discoveryReceiver);

        } catch (IllegalArgumentException ignored) {

        }

        discoveryReceiver = null;

    }
    public boolean isDiscovering() {

        if (bluetoothAdapter == null) {
            return false;
        }

        if (!hasScanPermission()) {
            return false;
        }

        return bluetoothAdapter.isDiscovering();
    }
    public boolean startDiscovery() {

        if (bluetoothAdapter == null)
            return false;

        if (!hasScanPermission())
            return false;

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        return bluetoothAdapter.startDiscovery();
    }
    public void stopDiscovery() {

        if (bluetoothAdapter == null)
            return;

        if (!hasScanPermission())
            return;

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }
    public Intent getEnableBluetoothIntent() {
        return new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    }
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public String getBluetoothName() {

        if (bluetoothAdapter == null)
            return null;

        return bluetoothAdapter.getName();
    }

    public String getBluetoothAddress() {

        if (bluetoothAdapter == null)
            return null;

        return bluetoothAdapter.getAddress();
    }

}