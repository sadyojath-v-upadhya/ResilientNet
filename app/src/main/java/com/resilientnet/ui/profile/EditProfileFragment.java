package com.resilientnet.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.resilientnet.R;
import com.resilientnet.data.local.entity.User;
import com.resilientnet.databinding.FragmentEditProfileBinding;
import com.resilientnet.utils.session.SessionManager;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;

    private EditProfileViewModel viewModel;

    private SessionManager sessionManager;

    private User currentUser;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentEditProfileBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(this)
                .get(EditProfileViewModel.class);

        sessionManager = new SessionManager(requireContext());

        setupDropdowns();

        loadUser();

        setupSaveButton();

        setupBackButton();

        return binding.getRoot();
    }

    private void setupDropdowns() {

        String[] bloodGroups = {
                "A+",
                "A-",
                "B+",
                "B-",
                "AB+",
                "AB-",
                "O+",
                "O-"
        };

        ArrayAdapter<String> bloodAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        bloodGroups
                );

        binding.personalDetails.actBloodGroup.setAdapter(bloodAdapter);

        String[] genders = {
                "Male",
                "Female",
                "Other"
        };

        ArrayAdapter<String> genderAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        genders
                );

        binding.personalDetails.actGender.setAdapter(genderAdapter);
    }

    private void loadUser() {

        String email = sessionManager.getLoggedInEmail();

        if (email != null) {

            viewModel.loadUser(email);

        }

        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {

            if (user == null) return;

            currentUser = user;

            binding.personalInformation.etFullName.setText(user.getFullName());

            binding.personalInformation.etEmail.setText(user.getEmail());

            binding.personalInformation.etPhone.setText(user.getPhoneNumber());

            binding.personalDetails.actBloodGroup.setText(
                    user.getBloodGroup(),
                    false
            );

            if (user.getAge() != null) {

                binding.personalDetails.etAge.setText(
                        String.valueOf(user.getAge())
                );

            }

            binding.personalDetails.actGender.setText(
                    user.getGender(),
                    false
            );

            binding.emergencyInformation.etEmergencyContact
                    .setText(user.getEmergencyContact());

            binding.emergencyInformation.etLocation
                    .setText(user.getHomeBase());

        });

    }

    private void setupBackButton() {

        binding.header.toolbar.setNavigationOnClickListener(v ->

                NavHostFragment.findNavController(this).navigateUp()

        );

    }

    private void setupSaveButton() {

        binding.btnSaveChanges.setOnClickListener(v -> {

            if (currentUser == null) {
                return;
            }

            String fullName = binding.personalInformation.etFullName
                    .getText()
                    .toString()
                    .trim();

            String phone = binding.personalInformation.etPhone
                    .getText()
                    .toString()
                    .trim();

            String bloodGroup = binding.personalDetails.actBloodGroup
                    .getText()
                    .toString()
                    .trim();

            String ageText = binding.personalDetails.etAge
                    .getText()
                    .toString()
                    .trim();

            String gender = binding.personalDetails.actGender
                    .getText()
                    .toString()
                    .trim();

            String emergencyContact = binding.emergencyInformation.etEmergencyContact
                    .getText()
                    .toString()
                    .trim();

            String location = binding.emergencyInformation.etLocation
                    .getText()
                    .toString()
                    .trim();

            // Basic validation
            if (fullName.isEmpty()) {
                binding.personalInformation.layoutFullName.setError("Full name is required");
                return;
            } else {
                binding.personalInformation.layoutFullName.setError(null);
            }

            if (phone.isEmpty()) {
                binding.personalInformation.layoutPhone.setError("Phone number is required");
                return;
            } else {
                binding.personalInformation.layoutPhone.setError(null);
            }

            Integer age = null;
            if (!ageText.isEmpty()) {
                age = Integer.parseInt(ageText);
            }

            currentUser.setFullName(fullName);
            currentUser.setPhoneNumber(phone);
            currentUser.setBloodGroup(bloodGroup);
            currentUser.setAge(age);
            currentUser.setGender(gender);
            currentUser.setEmergencyContact(emergencyContact);
            currentUser.setHomeBase(location);

            boolean updated = viewModel.updateUser(currentUser);

            if (updated) {

                sessionManager.saveUserName(currentUser.getFullName());

                Toast.makeText(
                        requireContext(),
                        "Profile updated successfully",
                        Toast.LENGTH_SHORT
                ).show();

                NavHostFragment.findNavController(this).navigateUp();

            } else {

                Toast.makeText(
                        requireContext(),
                        "Failed to update profile",
                        Toast.LENGTH_SHORT
                ).show();

            }

        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}