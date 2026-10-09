package com.resilientnet.ui.home.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.resilientnet.data.local.entity.SosEntity;
import com.resilientnet.databinding.ItemSosHistoryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SosHistoryAdapter extends RecyclerView.Adapter<SosHistoryAdapter.SosHistoryViewHolder> {

    private final List<SosEntity> alerts = new ArrayList<>();

    public void setAlerts(List<SosEntity> newAlerts) {
        alerts.clear();
        if (newAlerts != null) {
            alerts.addAll(newAlerts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SosHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSosHistoryBinding binding = ItemSosHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new SosHistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SosHistoryViewHolder holder, int position) {
        holder.bind(alerts.get(position));
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    static class SosHistoryViewHolder extends RecyclerView.ViewHolder {

        private final ItemSosHistoryBinding binding;

        SosHistoryViewHolder(ItemSosHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SosEntity entity) {
            String type = entity.getEmergencyType();
            binding.tvHistoryType.setText(type != null && !type.isEmpty() ? type : "Emergency Alert");

            // Format type badge color
            String typeLower = type != null ? type.toLowerCase(Locale.ROOT) : "";
            if (typeLower.contains("medical")) {
                binding.tvHistoryType.setTextColor(Color.parseColor("#E53935"));
            } else if (typeLower.contains("fire")) {
                binding.tvHistoryType.setTextColor(Color.parseColor("#F4511E"));
            } else if (typeLower.contains("flood")) {
                binding.tvHistoryType.setTextColor(Color.parseColor("#0288D1"));
            } else {
                binding.tvHistoryType.setTextColor(Color.parseColor("#E65100"));
            }

            // Status
            String status = entity.getStatus();
            binding.tvHistoryStatus.setText(status != null ? status : "ACTIVE");

            // Message / Description
            String msg = entity.getMessage();
            if (msg == null || msg.trim().isEmpty()) {
                msg = "Emergency assistance requested";
            }
            binding.tvHistoryMessage.setText(msg);

            // Coordinates / Location
            String loc = entity.getLocationName();
            if (loc == null || loc.isEmpty()) {
                if (entity.getLatitude() != 0.0 || entity.getLongitude() != 0.0) {
                    loc = String.format(Locale.US, "Lat: %.5f, Lng: %.5f", entity.getLatitude(), entity.getLongitude());
                } else {
                    loc = "Location Not Recorded";
                }
            }
            binding.tvHistoryLocation.setText(loc);

            // Timestamp
            binding.tvHistoryTimestamp.setText(entity.getTimestamp());
        }
    }
}
