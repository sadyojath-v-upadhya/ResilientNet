package com.resilientnet.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.resilientnet.R;
import com.resilientnet.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentDashboardBinding.inflate(inflater, container, false);

        setupBottomNavigation();

        setupBackPress();

        return binding.getRoot();
    }

    private void setupBottomNavigation() {

        NavHostFragment navHostFragment =
                (NavHostFragment) getChildFragmentManager()
                        .findFragmentById(R.id.dashboardNavHost);

        if (navHostFragment != null) {

            NavigationUI.setupWithNavController(
                    binding.bottomNavigation,
                    navHostFragment.getNavController()
            );

        }

    }

    public void selectTab(int menuItemId) {
        if (binding != null && binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(menuItemId);
        }
    }

    private void setupBackPress() {

        NavHostFragment navHostFragment =
                (NavHostFragment) getChildFragmentManager()
                        .findFragmentById(R.id.dashboardNavHost);

        if (navHostFragment == null)
            return;

        NavController navController = navHostFragment.getNavController();

        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        new OnBackPressedCallback(true) {

                            @Override
                            public void handleOnBackPressed() {

                                if (!navController.popBackStack()) {

                                    requireActivity().finish();

                                }

                            }

                        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}