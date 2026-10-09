package com.resilientnet.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

public class BluetoothPermissionHelper {

    /**
     * Returns the runtime permissions required
     * for the current Android version.
     */
    public static String[] getRequiredPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            return new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };

        } else {

            return new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

        }
    }

    /**
     * Checks whether all required permissions
     * have already been granted.
     */
    public static boolean hasPermissions(Context context) {

        for (String permission : getRequiredPermissions()) {

            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
            ) != PackageManager.PERMISSION_GRANTED) {

                return false;

            }
        }

        return true;
    }
}