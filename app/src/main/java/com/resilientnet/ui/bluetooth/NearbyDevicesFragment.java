package com.resilientnet.ui.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.resilientnet.R;
import com.resilientnet.bluetooth.BluetoothConnectionListener;
import com.resilientnet.bluetooth.BluetoothConnectionManager;
import com.resilientnet.bluetooth.BluetoothDiscoveryListener;
import com.resilientnet.bluetooth.BluetoothManager;
import com.resilientnet.bluetooth.BluetoothMessageListener;
import com.resilientnet.bluetooth.model.BluetoothDeviceModel;
import com.resilientnet.databinding.FragmentNearbyDevicesBinding;
import com.resilientnet.bluetooth.BluetoothMessageListener;
import com.resilientnet.bluetooth.BluetoothConnectionObserver;
import com.resilientnet.bluetooth.BluetoothConnectionService;

public class NearbyDevicesFragment extends Fragment
        implements BluetoothDiscoveryListener,
        BluetoothConnectionObserver {

    private static final String TAG =
            "ResilientNetBluetooth";

    private FragmentNearbyDevicesBinding binding;

    private BluetoothManager bluetoothManager;

    private BluetoothConnectionService connectionService;

    private BluetoothDeviceAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding =
                FragmentNearbyDevicesBinding.inflate(
                        inflater,
                        container,
                        false
                );

        /*
         * Bluetooth discovery manager.
         */
        bluetoothManager =
                new BluetoothManager(
                        requireContext()
                );

        /*
         * Bluetooth connection manager.
         *
         * The Fragment itself implements
         * BluetoothConnectionListener.
         */
        connectionService =
                BluetoothConnectionService.getInstance(
                        requireContext()
                );

        connectionService.registerObserver(this);

        setupRecyclerView();
        setupToolbar();
        setupScanButton();
        setupListenButton();
        setupWifiDirectButton();

        return binding.getRoot();
    }

    private void setupWifiDirectButton() {
        binding.cardWifiDirect.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_nearbyDevicesFragment_to_wifiDirectFragment)
        );
    }

    private void setupRecyclerView() {

        adapter =
                new BluetoothDeviceAdapter(
                        this::connectToDevice
                );

        binding.rvDevices.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()
                )
        );

        binding.rvDevices.setAdapter(adapter);
    }

    private void setupToolbar() {

        binding.toolbar.setNavigationOnClickListener(
                v -> NavHostFragment
                        .findNavController(this)
                        .navigateUp()
        );
    }

    private void setupScanButton() {

        binding.btnScan.setOnClickListener(
                v -> startScanning()
        );
    }

    private void setupListenButton() {

        int initialCount = connectionService.getActiveConnections().size();
        if (initialCount > 0) {
            binding.btnListen.setText(
                    initialCount > 1 ? "Connected (" + initialCount + ")" : "Connected"
            );
            binding.btnListen.setEnabled(false);
        }

        binding.btnListen.setOnClickListener(v -> {

            Log.d(
                    TAG,
                    "Starting RFCOMM server"
            );

            connectionService.startServer();

            binding.btnListen.setText(
                    "Listening..."
            );

            binding.btnListen.setEnabled(
                    false
            );
        });
    }

    private void startScanning() {

        if (bluetoothManager.isDiscovering()) {
            return;
        }

        Log.d(
                TAG,
                "Starting Bluetooth scan"
        );

        adapter.clearDevices();

        binding.layoutEmpty.setVisibility(
                View.GONE
        );

        binding.rvDevices.setVisibility(
                View.GONE
        );

        binding.progressScan.setVisibility(
                View.VISIBLE
        );

        bluetoothManager.registerDiscoveryReceiver(
                this
        );

        boolean started =
                bluetoothManager.startDiscovery();

        Log.d(
                TAG,
                "Bluetooth discovery started: "
                        + started
        );

        if (!started) {

            binding.progressScan.setVisibility(
                    View.GONE
            );

            binding.layoutEmpty.setVisibility(
                    View.VISIBLE
            );
        }
    }

    private void connectToDevice(
            BluetoothDeviceModel device) {

        Log.d(
                TAG,
                "Starting connection to: "
                        + device.getName()
                        + " - "
                        + device.getAddress()
        );

        try {

            BluetoothDevice bluetoothDevice =
                    bluetoothManager
                            .getBluetoothAdapter()
                            .getRemoteDevice(
                                    device.getAddress()
                            );

            connectionService.connectToDevice(
                    bluetoothDevice
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Unable to start connection",
                    e
            );
        }
    }

    // ---------------------------------------------------------
    // Bluetooth Discovery
    // ---------------------------------------------------------

    @Override
    public void onDeviceFound(
            BluetoothDeviceModel device) {

        Log.d(
                TAG,
                "Device found: "
                        + device.getName()
                        + " - "
                        + device.getAddress()
        );

        if (!isAdded() || binding == null) {
            return;
        }

        adapter.addDevice(device);

        binding.layoutEmpty.setVisibility(
                View.GONE
        );

        binding.rvDevices.setVisibility(
                View.VISIBLE
        );
    }

    @Override
    public void onDiscoveryStarted() {

        Log.d(
                TAG,
                "Discovery started callback received"
        );

        if (!isAdded() || binding == null) {
            return;
        }

        binding.progressScan.setVisibility(
                View.VISIBLE
        );
    }

    @Override
    public void onDiscoveryFinished() {

        Log.d(
                TAG,
                "Discovery finished"
        );

        if (!isAdded() || binding == null) {
            return;
        }

        binding.progressScan.setVisibility(
                View.GONE
        );

        if (adapter.getItemCount() == 0) {

            binding.rvDevices.setVisibility(
                    View.GONE
            );

            binding.layoutEmpty.setVisibility(
                    View.VISIBLE
            );

        } else {

            binding.rvDevices.setVisibility(
                    View.VISIBLE
            );
        }
    }

    // ---------------------------------------------------------
    // Bluetooth Connection
    // ---------------------------------------------------------

    @Override
    public void onConnecting(
            BluetoothDevice device) {

        Log.d(
                TAG,
                "Connecting to: "
                        + device.getAddress()
        );
    }

    @Override
    public void onConnected(
            BluetoothDevice device) {

        Log.d(
                TAG,
                "ResilientNet connection established: "
                        + device.getAddress()
        );

        if (!isAdded() || binding == null) {
            return;
        }

        int count = connectionService.getActiveConnections().size();
        binding.btnListen.setText(
                count > 1 ? "Connected (" + count + ")" : "Connected"
        );

        binding.btnListen.setEnabled(
                false
        );
    }

    @Override
    public void onConnectionFailed(
            BluetoothDevice device,
            Exception exception) {

        Log.e(
                TAG,
                "Connection failed: "
                        + exception.getMessage(),
                exception
        );

        if (!isAdded() || binding == null) {
            return;
        }

        int count = connectionService.getActiveConnections().size();
        if (count > 0) {
            binding.btnListen.setText(
                    count > 1 ? "Connected (" + count + ")" : "Connected"
            );
            binding.btnListen.setEnabled(
                    false
            );
        } else {
            binding.btnListen.setText(
                    "Start Listening"
            );
            binding.btnListen.setEnabled(
                    true
            );
        }
    }

    @Override
    public void onDisconnected(
            BluetoothDevice device) {

        Log.d(
                TAG,
                "Disconnected from: "
                        + device.getAddress()
        );

        if (!isAdded() || binding == null) {
            return;
        }

        int count = connectionService.getActiveConnections().size();
        if (count > 0) {
            binding.btnListen.setText(
                    count > 1 ? "Connected (" + count + ")" : "Connected"
            );
            binding.btnListen.setEnabled(
                    false
            );
        } else {
            binding.btnListen.setText(
                    "Start Listening"
            );
            binding.btnListen.setEnabled(
                    true
            );
        }
    }

    @Override
    public void onMessageReceived(
            BluetoothDevice device,
            String message) {

        Log.d(
                TAG,
                "Application message received from "
                        + device.getAddress()
                        + ": "
                        + message
        );
    }

    // ---------------------------------------------------------
    // Fragment Cleanup
    // ---------------------------------------------------------

    @Override
    public void onDestroyView() {

        if (bluetoothManager != null) {

            bluetoothManager.stopDiscovery();

            bluetoothManager
                    .unregisterDiscoveryReceiver();
        }

        if (connectionService != null) {

            connectionService.unregisterObserver(this);

            connectionService = null;
        }

        super.onDestroyView();

        binding = null;
    }



}