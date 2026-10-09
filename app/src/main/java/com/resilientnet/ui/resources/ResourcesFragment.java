package com.resilientnet.ui.resources;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.resilientnet.MainActivity;
import com.resilientnet.databinding.FragmentResourcesBinding;
import com.resilientnet.gateway.InternetGatewayManager;

public class ResourcesFragment extends Fragment {

    private FragmentResourcesBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentResourcesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar();
        setupSyncCheck();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
    }

    private void setupSyncCheck() {
        binding.btnCheckSync.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity.getCommunicationManager() != null) {
                    InternetGatewayManager gateway = mainActivity.getCommunicationManager().getInternetGatewayManager();
                    if (gateway != null) {
                        gateway.refreshGatewayState();
                        if (gateway.isGatewayOnline()) {
                            Toast.makeText(
                                    requireContext(),
                                    "Gateway Online: Ready to sync live relief camps from website portal.",
                                    Toast.LENGTH_LONG
                            ).show();
                        } else {
                            Toast.makeText(
                                    requireContext(),
                                    "Mesh Mode (Offline): Operating with local disaster resource guidelines.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                        return;
                    }
                }
            }
            Toast.makeText(requireContext(), "Gateway check unavailable", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
