package com.resilientnet.ui.bluetooth;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.resilientnet.bluetooth.model.BluetoothDeviceModel;
import com.resilientnet.databinding.ItemBluetoothDeviceBinding;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDeviceAdapter
        extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {

    public interface OnDeviceClickListener {

        void onDeviceClicked(
                BluetoothDeviceModel device
        );
    }

    private final List<BluetoothDeviceModel> devices =
            new ArrayList<>();

    private final OnDeviceClickListener listener;

    public BluetoothDeviceAdapter(
            OnDeviceClickListener listener) {

        this.listener = listener;
    }

    public void addDevice(
            BluetoothDeviceModel device) {

        if (device == null) {
            return;
        }

        /*
         * Avoid duplicate devices.
         *
         * Bluetooth discovery may report the same
         * device multiple times.
         */
        for (BluetoothDeviceModel existing : devices) {

            if (existing.getAddress()
                    .equals(device.getAddress())) {

                return;
            }
        }

        devices.add(device);

        notifyItemInserted(
                devices.size() - 1
        );
    }

    public void clearDevices() {

        devices.clear();

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        ItemBluetoothDeviceBinding binding =
                ItemBluetoothDeviceBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        BluetoothDeviceModel device =
                devices.get(position);

        /*
         * These IDs match item_bluetooth_device.xml
         */
        holder.binding.tvDeviceName.setText(
                device.getName()
        );

        holder.binding.tvDeviceAddress.setText(
                device.getAddress()
        );

        /*
         * Clicking a device starts the connection.
         */
        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {

                listener.onDeviceClicked(
                        device
                );
            }
        });
    }

    @Override
    public int getItemCount() {

        return devices.size();
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        final ItemBluetoothDeviceBinding binding;

        ViewHolder(
                ItemBluetoothDeviceBinding binding) {

            super(binding.getRoot());

            this.binding = binding;
        }
    }
}