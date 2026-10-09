package com.resilientnet.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.resilientnet.R;

import java.util.ArrayList;
import java.util.List;

public class WifiP2pPeerAdapter extends RecyclerView.Adapter<WifiP2pPeerAdapter.PeerViewHolder> {

    private final List<WifiP2pDevice> peers = new ArrayList<>();
    private final OnPeerClickListener listener;

    public interface OnPeerClickListener {
        void onConnect(WifiP2pDevice device);
    }

    public WifiP2pPeerAdapter(OnPeerClickListener listener) {
        this.listener = listener;
    }

    public void setPeers(List<WifiP2pDevice> newPeers) {
        peers.clear();
        if (newPeers != null) {
            peers.addAll(newPeers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wifi_p2p_peer, parent, false);
        return new PeerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PeerViewHolder holder, int position) {
        WifiP2pDevice device = peers.get(position);
        holder.tvPeerName.setText(device.deviceName != null && !device.deviceName.isEmpty() ? device.deviceName : device.deviceAddress);
        holder.tvPeerStatus.setText(getStatusString(device.status));

        holder.btnConnectPeer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConnect(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return peers.size();
    }

    private String getStatusString(int status) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE: return "Available (Tap Connect)";
            case WifiP2pDevice.INVITED: return "Invitation Pending...";
            case WifiP2pDevice.CONNECTED: return "Connected";
            case WifiP2pDevice.FAILED: return "Failed";
            case WifiP2pDevice.UNAVAILABLE: return "Unavailable";
            default: return "Status: " + status;
        }
    }

    static class PeerViewHolder extends RecyclerView.ViewHolder {
        final TextView tvPeerName;
        final TextView tvPeerStatus;
        final MaterialButton btnConnectPeer;

        PeerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPeerName = itemView.findViewById(R.id.tvPeerName);
            tvPeerStatus = itemView.findViewById(R.id.tvPeerStatus);
            btnConnectPeer = itemView.findViewById(R.id.btnConnectPeer);
        }
    }
}
