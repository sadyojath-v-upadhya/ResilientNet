package com.resilientnet.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resilientnet.R;
import com.resilientnet.databinding.FragmentProfileBinding;
import com.resilientnet.utils.session.SessionManager;

import androidx.navigation.fragment.NavHostFragment;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        sessionManager = new SessionManager(requireContext());

        String email = sessionManager.getLoggedInEmail();

        if (email != null) {
            viewModel.loadUser(email);
        }

        observeUser();

        setupLogout();

        setupEditProfile();

        return binding.getRoot();
    }

    private void observeUser() {

        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {

            if (user == null) return;

            binding.profileHeader.tvName.setText(user.getFullName());
            binding.profileHeader.tvEmail.setText(user.getEmail());
            binding.profileHeader.tvPhone.setText(user.getPhoneNumber());

            binding.personalInformation.tvBloodGroup.setText(
                    user.getBloodGroup() != null ? user.getBloodGroup() : "Not Set"
            );

            binding.personalInformation.tvAge.setText(
                    user.getAge() != null ? String.valueOf(user.getAge()) : "Not Set"
            );

            binding.personalInformation.tvGender.setText(
                    user.getGender() != null ? user.getGender() : "Not Set"
            );

            binding.emergencyInformation.tvEmergencyContact.setText(
                    user.getEmergencyContact() != null ? user.getEmergencyContact() : "Not Set"
            );

            binding.emergencyInformation.tvHomeBase.setText(
                    user.getHomeBase() != null ? user.getHomeBase() : "Not Set"
            );

        });

    }

    private void setupEditProfile() {

        binding.account.layoutEditProfile.setOnClickListener(v -> {

            NavHostFragment.findNavController(ProfileFragment.this)
                    .navigate(R.id.action_profileFragment_to_editProfileFragment);

        });

    }

    private void setupLogout() {

        binding.account.layoutLogout.setOnClickListener(v -> {

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {

                        sessionManager.logout();

                        NavController navController =
                                Navigation.findNavController(
                                        requireActivity(),
                                        R.id.nav_host_fragment);

                        NavOptions options =
                                new NavOptions.Builder()
                                        .setPopUpTo(R.id.nav_graph, true)
                                        .build();

                        navController.navigate(
                                R.id.loginFragment,
                                null,
                                options
                        );

                    })
                    .setNegativeButton("Cancel", null)
                    .show();

        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}