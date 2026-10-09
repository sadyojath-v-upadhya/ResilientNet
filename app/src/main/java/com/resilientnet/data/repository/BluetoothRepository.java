package com.resilientnet.data.repository;

import android.content.Context;
import android.content.Intent;

import com.resilientnet.bluetooth.BluetoothManager;
import com.resilientnet.bluetooth.BluetoothState;
import com.resilientnet.permissions.BluetoothPermissionHelper;

public class BluetoothRepository {

    private final BluetoothManager bluetoothManager;

    public BluetoothRepository(Context context) {
        bluetoothManager = new BluetoothManager(context);
    }

    public BluetoothState getBluetoothState(Context context) {

        if (!bluetoothManager.isBluetoothSupported())
            return BluetoothState.NOT_SUPPORTED;

        if (!BluetoothPermissionHelper.hasPermissions(context))
            return BluetoothState.MISSING_PERMISSION;

        if (!bluetoothManager.isBluetoothEnabled())
            return BluetoothState.DISABLED;

        return BluetoothState.READY;
    }

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public Intent getEnableBluetoothIntent() {
        return bluetoothManager.getEnableBluetoothIntent();
    }

}