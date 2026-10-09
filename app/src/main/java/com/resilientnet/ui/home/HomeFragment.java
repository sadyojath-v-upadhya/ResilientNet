package com.resilientnet.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.resilientnet.R;
import com.resilientnet.databinding.FragmentHomeBinding;
import com.resilientnet.ui.home.adapter.QuickActionAdapter;
import com.resilientnet.ui.home.model.QuickAction;
import com.resilientnet.utils.session.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import androidx.navigation.fragment.NavHostFragment;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.widget.Toast;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.resilientnet.MainActivity;
import com.resilientnet.bluetooth.BluetoothState;
import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.databinding.BottomSheetSosHistoryBinding;
import com.resilientnet.gateway.InternetGatewayManager;
import com.resilientnet.gateway.SyncCallback;
import com.resilientnet.permissions.BluetoothPermissionHelper;
import com.resilientnet.ui.bluetooth.BluetoothViewModel;
import com.resilientnet.ui.contacts.EmergencyContactsManager;
import com.resilientnet.ui.dashboard.DashboardFragment;
import com.resilientnet.ui.home.adapter.SosHistoryAdapter;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private SessionManager sessionManager;

    private BluetoothViewModel bluetoothViewModel;
    private QuickActionAdapter quickActionAdapter;
    private int currentMessageCount = 0;

    private ActivityResultLauncher<String[]> bluetoothPermissionLauncher;

    private ActivityResultLauncher<Intent> enableBluetoothLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        bluetoothViewModel =
                new ViewModelProvider(this).get(BluetoothViewModel.class);

        sessionManager = new SessionManager(requireContext());

        loadUser();

        setGreeting();

        setupQuickActions();
        observeBluetoothState();
        observeGatewayState();
        setupNotificationListener();

        bluetoothViewModel.refreshBluetoothState();
        binding.btnSOS.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_homeFragment_to_sosFragment)
        );
        bluetoothPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> {
                            bluetoothViewModel.refreshBluetoothState();
                            triggerMeshConnection();
                        }
                );
        enableBluetoothLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            bluetoothViewModel.refreshBluetoothState();
                            triggerMeshConnection();
                        }
                );
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        bluetoothViewModel.refreshBluetoothState();
        triggerMeshConnection();
        refreshGatewayState();
        refreshQuickActionMetrics();
    }

    private void triggerMeshConnection() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity.getCommunicationManager() != null) {
                mainActivity.getCommunicationManager().startOrRefreshMesh();
            }
        }
    }

    private void refreshGatewayState() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity.getCommunicationManager() != null
                    && mainActivity.getCommunicationManager().getInternetGatewayManager() != null) {
                mainActivity.getCommunicationManager()
                        .getInternetGatewayManager()
                        .refreshGatewayState();
            }
        }
    }

    private void observeBluetoothState() {

        bluetoothViewModel.getBluetoothState().observe(
                getViewLifecycleOwner(),
                state -> {

                    switch (state) {

                        case NOT_SUPPORTED:

                            binding.txtBluetoothStatus.setText("Not Supported");
                            break;

                        case MISSING_PERMISSION:

                            binding.txtBluetoothStatus.setText("Permission Required");

                            bluetoothPermissionLauncher.launch(
                                    BluetoothPermissionHelper.getRequiredPermissions()
                            );
                            break;

                        case DISABLED:

                            binding.txtBluetoothStatus.setText("Disabled");

                            enableBluetoothLauncher.launch(
                                    bluetoothViewModel
                                            .getRepository()
                                            .getEnableBluetoothIntent()
                            );
                            break;

                        case READY:

                            binding.txtBluetoothStatus.setText("Ready");
                            triggerMeshConnection();
                            break;

                        default:

                            binding.txtBluetoothStatus.setText(state.name());

                            break;
                    }

                });

    }
    private void loadUser() {

        String email = sessionManager.getLoggedInEmail();

        if (email == null) {
            return;
        }

        viewModel.loadUser(email);

        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {

            if (user != null) {

                binding.txtUserName.setText(user.getFullName());

            }

        });
    }

    private void setGreeting() {

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {

            binding.txtGreeting.setText("Good Morning");

        } else if (hour < 17) {

            binding.txtGreeting.setText("Good Afternoon");

        } else {

            binding.txtGreeting.setText("Good Evening");

        }
    }

    private void setupQuickActions() {
        quickActionAdapter = new QuickActionAdapter(buildQuickActionList(), action -> {
            switch (action.getTitle()) {
                case "Nearby Devices":
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_homeFragment_to_nearbyDevicesFragment);
                    break;

                case "Messages":
                    if (getParentFragment() instanceof NavHostFragment) {
                        Fragment parent = getParentFragment().getParentFragment();
                        if (parent instanceof DashboardFragment) {
                            ((DashboardFragment) parent).selectTab(R.id.chatFragment);
                        }
                    }
                    break;

                case "Resources":
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_homeFragment_to_resourcesFragment);
                    break;

                case "Emergency Contacts":
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_homeFragment_to_emergencyContactsFragment);
                    break;
            }
        });

        binding.rvQuickActions.setLayoutManager(
                new GridLayoutManager(requireContext(), 2));
        binding.rvQuickActions.setAdapter(quickActionAdapter);

        // Dynamically update messages count when Room updates
        try {
            AppDatabase.getInstance(requireContext())
                    .messageDao()
                    .getAllMessages()
                    .observe(getViewLifecycleOwner(), messages -> {
                        currentMessageCount = messages != null ? messages.size() : 0;
                        refreshQuickActionMetrics();
                    });
        } catch (Exception ignored) {
        }
    }

    private List<QuickAction> buildQuickActionList() {
        List<QuickAction> actions = new ArrayList<>();

        int connectedPeers = 0;
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity.getCommunicationManager() != null
                    && mainActivity.getCommunicationManager().getBluetoothService() != null) {
                connectedPeers = mainActivity.getCommunicationManager()
                        .getBluetoothService()
                        .getActiveConnections()
                        .size();
            }
        }

        String peerSubtext = connectedPeers > 0
                ? (connectedPeers + (connectedPeers == 1 ? " Connected" : " Connected"))
                : "Scan Peers";

        String messageSubtext = currentMessageCount > 0
                ? (currentMessageCount + " Messages")
                : "Open Chat";

        int savedContactsCount = 0;
        try {
            savedContactsCount = new EmergencyContactsManager(requireContext()).getAllContacts().size();
        } catch (Exception ignored) {
        }
        String contactsSubtext = savedContactsCount > 0
                ? (savedContactsCount + " Helplines")
                : "Disaster Helplines";

        actions.add(new QuickAction(
                R.drawable.ic_devices,
                "Nearby Devices",
                peerSubtext));

        actions.add(new QuickAction(
                R.drawable.ic_chat,
                "Messages",
                messageSubtext));

        actions.add(new QuickAction(
                R.drawable.ic_resources,
                "Resources",
                "Relief Directory"));

        actions.add(new QuickAction(
                R.drawable.ic_contacts,
                "Emergency Contacts",
                contactsSubtext));

        return actions;
    }

    private void refreshQuickActionMetrics() {
        if (quickActionAdapter != null) {
            quickActionAdapter.updateActions(buildQuickActionList());
        }
    }

    private void observeGatewayState() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity.getCommunicationManager() != null) {
                mainActivity.getCommunicationManager()
                        .getInternetGatewayManager()
                        .getIsGatewayNodeLiveData()
                        .observe(getViewLifecycleOwner(), isGateway -> {
                            if (!isAdded() || binding == null) {
                                return;
                            }

                            if (Boolean.TRUE.equals(isGateway)) {
                                binding.txtGatewayStatus.setText("Online (Syncing)");
                                binding.txtGatewayStatus.setTextColor(
                                        ContextCompat.getColor(requireContext(), R.color.status_connected)
                                );
                            } else {
                                binding.txtGatewayStatus.setText("Offline (Mesh)");
                                binding.txtGatewayStatus.setTextColor(0xFF888888);
                            }
                        });
            }
        }

        binding.txtGatewayStatus.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity.getCommunicationManager() != null) {
                    InternetGatewayManager gatewayManager =
                            mainActivity.getCommunicationManager().getInternetGatewayManager();
                    if (gatewayManager != null) {
                        gatewayManager.refreshGatewayState();
                        if (!gatewayManager.isGatewayOnline()) {
                            Toast.makeText(requireContext(), "Offline: Operating in Mesh Mode", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Toast.makeText(requireContext(), "Syncing with cloud gateway...", Toast.LENGTH_SHORT).show();
                        gatewayManager.triggerManualSync(new SyncCallback() {
                                @Override
                                public void onSyncStarted() {
                                    if (isAdded() && binding != null) {
                                        binding.txtGatewayStatus.setText("Syncing...");
                                    }
                                }

                                @Override
                                public void onSyncSuccess(int syncedSosCount, int syncedMessageCount) {
                                    if (isAdded() && binding != null) {
                                        binding.txtGatewayStatus.setText(
                                                syncedSosCount > 0 ? "Synced (" + syncedSosCount + ")" : "Online (Sync)"
                                        );
                                    }
                                    Toast.makeText(
                                            requireContext(),
                                            "Sync complete: " + syncedSosCount + " SOS uploaded",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                                @Override
                                public void onSyncFailed(String errorMessage) {
                                    if (isAdded() && binding != null) {
                                        binding.txtGatewayStatus.setText("Sync Error");
                                    }
                                    Toast.makeText(
                                            requireContext(),
                                            "Sync notice: " + errorMessage,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });
                    }
                }
            }
        });
    }

    private void setupNotificationListener() {
        AppDatabase.getInstance(requireContext())
                .sosDao()
                .getAllSosRequests()
                .observe(getViewLifecycleOwner(), sosList -> {
                    if (binding != null && binding.viewNotificationBadge != null) {
                        if (sosList != null && !sosList.isEmpty()) {
                            binding.viewNotificationBadge.setVisibility(View.VISIBLE);
                        } else {
                            binding.viewNotificationBadge.setVisibility(View.GONE);
                        }
                    }
                });

        View.OnClickListener clickListener = v -> showPreviousSosHistoryBottomSheet();
        binding.layoutNotification.setOnClickListener(clickListener);
        binding.imgNotification.setOnClickListener(clickListener);
    }

    private void showPreviousSosHistoryBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        BottomSheetSosHistoryBinding sheetBinding = BottomSheetSosHistoryBinding.inflate(
                LayoutInflater.from(requireContext())
        );
        bottomSheetDialog.setContentView(sheetBinding.getRoot());

        SosHistoryAdapter adapter = new SosHistoryAdapter();
        sheetBinding.rvSosHistory.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        sheetBinding.rvSosHistory.setAdapter(adapter);

        sheetBinding.btnCloseHistory.setOnClickListener(v -> bottomSheetDialog.dismiss());

        AppDatabase.getInstance(requireContext())
                .sosDao()
                .getAllSosRequests()
                .observe(getViewLifecycleOwner(), sosList -> {
                    if (sosList != null && !sosList.isEmpty()) {
                        adapter.setAlerts(sosList);
                        sheetBinding.tvHistoryCount.setText(
                                sosList.size() + (sosList.size() == 1 ? " Alert" : " Alerts")
                        );
                        sheetBinding.layoutEmptyHistory.setVisibility(View.GONE);
                        sheetBinding.rvSosHistory.setVisibility(View.VISIBLE);
                    } else {
                        adapter.setAlerts(null);
                        sheetBinding.tvHistoryCount.setText("0 Alerts");
                        sheetBinding.layoutEmptyHistory.setVisibility(View.VISIBLE);
                        sheetBinding.rvSosHistory.setVisibility(View.GONE);
                    }
                });

        bottomSheetDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}