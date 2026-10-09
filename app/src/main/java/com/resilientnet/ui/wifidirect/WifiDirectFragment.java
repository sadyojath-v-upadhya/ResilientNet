package com.resilientnet.ui.wifidirect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.resilientnet.R;
import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.local.entity.MessageEntity;
import com.resilientnet.data.local.entity.SosEntity;
import com.resilientnet.data.model.MessageConstants;
import com.resilientnet.databinding.FragmentWifiDirectBinding;
import com.resilientnet.utils.session.SessionManager;
import com.resilientnet.wifidirect.WifiDirectManager;
import com.resilientnet.wifidirect.WifiP2pPeerAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

public class WifiDirectFragment extends Fragment implements WifiDirectManager.WifiDirectListener {

    private FragmentWifiDirectBinding binding;
    private WifiDirectManager wifiDirectManager;
    private WifiP2pPeerAdapter peerAdapter;
    private SessionManager sessionManager;
    private final StringBuilder logBuilder = new StringBuilder();
    private boolean isAutoLinkRequested = false;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (granted == null || !granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    if (wifiDirectManager != null) {
                        if (isAutoLinkRequested) {
                            wifiDirectManager.startAutoLink();
                        } else {
                            wifiDirectManager.startDiscovery();
                        }
                    }
                } else {
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(), "Wi-Fi & Location permissions required for Wi-Fi Direct", Snackbar.LENGTH_LONG).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWifiDirectBinding.inflate(inflater, container, false);

        sessionManager = new SessionManager(requireContext());
        wifiDirectManager = WifiDirectManager.getInstance(requireContext());
        wifiDirectManager.setListener(this);
        wifiDirectManager.register();

        setupToolbar();
        setupRecyclerView();
        setupButtons();

        // Restore console log history if any
        String history = wifiDirectManager.getLogHistory();
        if (history != null && !history.isEmpty()) {
            binding.tvConsoleLog.setText(history);
            binding.scrollConsole.post(() -> binding.scrollConsole.fullScroll(View.FOCUS_DOWN));
        }

        // If already connected, immediately show connected state
        if (wifiDirectManager.isConnected()) {
            onConnected(wifiDirectManager.isGroupOwner(), wifiDirectManager.getCurrentGroupOwnerIp());
        } else {
            // Prompt permissions and discover peers so nearby devices appear
            checkPermissionsAndDiscover();
        }

        return binding.getRoot();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
    }

    private void setupRecyclerView() {
        peerAdapter = new WifiP2pPeerAdapter(device -> {
            if (wifiDirectManager != null) {
                wifiDirectManager.connectToPeer(device);
            }
        });
        binding.rvPeers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPeers.setAdapter(peerAdapter);
    }

    private void setupButtons() {
        binding.btnAutoLink.setOnClickListener(v -> checkPermissionsAndAutoLink());

        binding.btnSendChat.setOnClickListener(v -> sendChatMessage());

        binding.btnSendSos.setOnClickListener(v -> sendSosAlert());

        binding.btnDisconnect.setOnClickListener(v -> {
            if (wifiDirectManager != null) {
                wifiDirectManager.disconnect();
            }
        });
    }

    private void sendChatMessage() {
        if (wifiDirectManager == null || !wifiDirectManager.isConnected()) {
            Snackbar.make(binding.getRoot(), "Wi-Fi Direct tunnel not connected", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String input = "";
        if (binding.etWifiDirectMessage.getText() != null) {
            input = binding.etWifiDirectMessage.getText().toString().trim();
        }
        if (input.isEmpty()) {
            input = "Hello over Wi-Fi Direct high-speed tunnel!";
        }

        MessageEntity message = new MessageEntity();
        message.setMessageId(UUID.randomUUID().toString());
        message.setSenderId(sessionManager.getSenderIdentifier());
        message.setContent(input);
        message.setMessageType("NORMAL_MESSAGE");
        message.setTimestamp(System.currentTimeMillis());
        message.setStatus("DELIVERED");
        message.setHopCount(0);

        hideKeyboard();
        wifiDirectManager.sendMessagePacket(
                message,
                () -> {
                    if (binding != null) {
                        binding.etWifiDirectMessage.setText("");
                        Snackbar.make(binding.getRoot(), "💬 Chat sent over Wi-Fi Direct!", Snackbar.LENGTH_SHORT).show();
                    }
                },
                () -> {
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(), "Failed to deliver packet", Snackbar.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void sendSosAlert() {
        if (wifiDirectManager == null || !wifiDirectManager.isConnected()) {
            Snackbar.make(binding.getRoot(), "Wi-Fi Direct tunnel not connected", Snackbar.LENGTH_SHORT).show();
            return;
        }

        hideKeyboard();
        String customMsg = "";
        if (binding.etWifiDirectMessage.getText() != null) {
            customMsg = binding.etWifiDirectMessage.getText().toString().trim();
        }
        if (customMsg.isEmpty()) {
            customMsg = "Immediate assistance required in disaster zone";
        }

        double lat = 0.0;
        double lng = 0.0;
        try {
            Location loc = getLastKnownLocation();
            if (loc != null) {
                lat = loc.getLatitude();
                lng = loc.getLongitude();
            }
        } catch (Exception ignored) {
        }

        MessageEntity sosMessage = new MessageEntity();
        sosMessage.setMessageId(UUID.randomUUID().toString());
        sosMessage.setSenderId(sessionManager.getSenderIdentifier());
        sosMessage.setMessageType(MessageConstants.SOS);
        sosMessage.setContent("CRITICAL SOS: " + customMsg);
        sosMessage.setPriority(1);
        sosMessage.setHopCount(0);
        sosMessage.setLatitude(lat);
        sosMessage.setLongitude(lng);
        sosMessage.setTimestamp(System.currentTimeMillis());
        sosMessage.setStatus("DELIVERED");

        // Record in local SOS history as well
        double finalLat = lat;
        double finalLng = lng;
        String finalCustomMsg = customMsg;
        Executors.newSingleThreadExecutor().execute(() -> {
            SosEntity sos = new SosEntity(
                    "CRITICAL SOS",
                    finalCustomMsg,
                    "Wi-Fi Direct P2P",
                    finalLat,
                    finalLng,
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()),
                    "ACTIVE",
                    false
            );
            AppDatabase.getInstance(requireContext()).sosDao().insertSos(sos);
        });

        wifiDirectManager.sendMessagePacket(
                sosMessage,
                () -> {
                    if (binding != null) {
                        binding.etWifiDirectMessage.setText("");
                        Snackbar.make(binding.getRoot(), "🚨 Emergency SOS sent over Wi-Fi Direct!", Snackbar.LENGTH_LONG).show();
                    }
                },
                () -> {
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(), "Failed to broadcast SOS", Snackbar.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void hideKeyboard() {
        View view = getView();
        if (view != null) {
            try {
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        try {
            LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) {
                    loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                return loc;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void checkPermissionsAndDiscover() {
        isAutoLinkRequested = false;
        checkPermissionsAndProceed();
    }

    private void checkPermissionsAndAutoLink() {
        isAutoLinkRequested = true;
        checkPermissionsAndProceed();
    }

    private void checkPermissionsAndProceed() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        boolean needRequest = false;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (needRequest) {
            permissionLauncher.launch(permissions.toArray(new String[0]));
        } else {
            if (isAutoLinkRequested) {
                wifiDirectManager.startAutoLink();
            } else {
                wifiDirectManager.startDiscovery();
            }
        }
    }

    // ---------------------------------------------------------
    // WifiDirectManager.WifiDirectListener Callbacks
    // ---------------------------------------------------------

    @Override
    public void onStatusChanged(String status) {
        if (!isAdded() || binding == null) return;
        binding.tvConnectionStatus.setText(status);
    }

    @Override
    public void onPeersDiscovered(List<WifiP2pDevice> peers) {
        if (!isAdded() || binding == null) return;
        if (peerAdapter != null && peers != null && !peers.isEmpty()) {
            peerAdapter.setPeers(peers);
            binding.cardPeers.setVisibility(View.VISIBLE);
            binding.tvConnectionStatus.setText("Discovered " + peers.size() + " peer(s)");
        } else {
            binding.cardPeers.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConnected(boolean isGroupOwner, String groupOwnerAddress) {
        if (!isAdded() || binding == null) return;

        binding.tvConnectionStatus.setText("Connected (250 Mbps Active)");
        binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success));
        binding.tvRole.setText(isGroupOwner ? "Group Owner (Soft-AP)" : "Client Peer");
        binding.tvIpPort.setText((groupOwnerAddress != null ? groupOwnerAddress : "192.168.49.1") + ":" + WifiDirectManager.TCP_PORT);

        binding.cardPeers.setVisibility(View.GONE);
        binding.cardTransmission.setVisibility(View.VISIBLE);
        binding.btnDisconnect.setVisibility(View.VISIBLE);
        binding.btnAutoLink.setEnabled(false);
        binding.btnAutoLink.setText("⚡ Link Established");

        Snackbar.make(binding.getRoot(), "High-speed Wi-Fi Direct tunnel established!", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnected() {
        if (!isAdded() || binding == null) return;

        binding.tvConnectionStatus.setText("Disconnected / Idle");
        binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        binding.tvRole.setText("Unassigned");
        binding.tvIpPort.setText("TCP Port 8888");

        binding.cardPeers.setVisibility(View.GONE);
        binding.cardTransmission.setVisibility(View.GONE);
        binding.btnDisconnect.setVisibility(View.GONE);
        binding.btnAutoLink.setEnabled(true);
        binding.btnAutoLink.setText("⚡ Auto-Link Wi-Fi Direct");
    }

    @Override
    public void onMessageReceived(MessageEntity message) {
        if (!isAdded() || binding == null) return;

        if (MessageConstants.SOS.equals(message.getMessageType())) {
            Snackbar.make(binding.getRoot(), "🚨 SOS Alert received via Wi-Fi Direct!", Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(binding.getRoot(), "💬 Chat received: " + message.getContent(), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLog(String logEntry) {
        if (!isAdded() || binding == null) return;

        logBuilder.append(logEntry).append("\n");
        binding.tvConsoleLog.setText(logBuilder.toString());
        binding.scrollConsole.post(() -> binding.scrollConsole.fullScroll(View.FOCUS_DOWN));
    }

    // ---------------------------------------------------------
    // Lifecycle Cleanup
    // ---------------------------------------------------------

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (wifiDirectManager != null) {
            wifiDirectManager.setListener(null);
        }
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
