package com.resilientnet.ui.auth.register;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.resilientnet.R;
import com.resilientnet.databinding.FragmentRegisterBinding;
import com.resilientnet.ui.auth.register.viewmodel.RegisterViewModel;
import com.resilientnet.utils.validation.ValidationUtils;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;

    public RegisterFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentRegisterBinding.inflate(inflater, container, false);

        initializeViewModel();
        setupObservers();
        setupClickListeners();

        return binding.getRoot();
    }

    private void initializeViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(RegisterViewModel.class);
    }

    private void setupObservers() {

        viewModel.getRegistrationSuccess().observe(
                getViewLifecycleOwner(),
                success -> {

                    if (Boolean.TRUE.equals(success)) {

                        Snackbar.make(
                                binding.getRoot(),
                                "Registration successful.",
                                Snackbar.LENGTH_SHORT
                        ).show();

                        NavHostFragment.findNavController(this)
                                .navigate(R.id.action_register_to_login);
                    }
                });

        viewModel.getRegistrationError().observe(
                getViewLifecycleOwner(),
                error -> {

                    if (error == null) return;

                    switch (error) {

                        case "Email already registered.":
                            binding.emailLayout.setError(error);
                            binding.etEmail.requestFocus();
                            break;

                        case "Phone number already registered.":
                            binding.phoneLayout.setError(error);
                            binding.etPhone.requestFocus();
                            break;

                        default:
                            Snackbar.make(
                                    binding.getRoot(),
                                    error,
                                    Snackbar.LENGTH_LONG
                            ).show();

                    }

                });

    }

    private void setupClickListeners() {

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigateUp());

        binding.txtLogin.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_register_to_login));

        binding.btnCreateAccount.setOnClickListener(v -> {

            if (!validateInputs()) {
                return;
            }

            viewModel.registerUser(
                    binding.etFullName.getText().toString().trim(),
                    binding.etEmail.getText().toString().trim(),
                    binding.etPhone.getText().toString().trim(),
                    binding.etPassword.getText().toString(),
                    binding.etEmergencyContact.getText().toString().trim()
            );

        });

    }

    private boolean validateInputs() {

        clearErrors();

        String error;

        error = ValidationUtils.validateName(
                binding.etFullName.getText().toString().trim());

        if (showError(binding.fullNameLayout,
                binding.etFullName,
                error)) return false;

        error = ValidationUtils.validateEmail(
                binding.etEmail.getText().toString().trim());

        if (showError(binding.emailLayout,
                binding.etEmail,
                error)) return false;

        error = ValidationUtils.validatePhone(
                binding.etPhone.getText().toString().trim());

        if (showError(binding.phoneLayout,
                binding.etPhone,
                error)) return false;

        String password =
                binding.etPassword.getText().toString();

        error = ValidationUtils.validatePassword(password);

        if (showError(binding.passwordLayout,
                binding.etPassword,
                error)) return false;

        error = ValidationUtils.validateConfirmPassword(
                password,
                binding.etConfirmPassword.getText().toString());

        if (showError(binding.confirmPasswordLayout,
                binding.etConfirmPassword,
                error)) return false;

        error = ValidationUtils.validateEmergencyContact(
                binding.etEmergencyContact.getText().toString().trim());

        return !showError(binding.emergencyContactLayout,
                binding.etEmergencyContact,
                error);
    }

    private boolean showError(TextInputLayout layout,
                              TextInputEditText editText,
                              String error) {

        if (error != null) {

            layout.setError(error);
            editText.requestFocus();

            return true;
        }

        return false;
    }

    private void clearErrors() {

        binding.fullNameLayout.setError(null);
        binding.emailLayout.setError(null);
        binding.phoneLayout.setError(null);
        binding.passwordLayout.setError(null);
        binding.confirmPasswordLayout.setError(null);
        binding.emergencyContactLayout.setError(null);

        binding.fullNameLayout.setErrorEnabled(false);
        binding.emailLayout.setErrorEnabled(false);
        binding.phoneLayout.setErrorEnabled(false);
        binding.passwordLayout.setErrorEnabled(false);
        binding.confirmPasswordLayout.setErrorEnabled(false);
        binding.emergencyContactLayout.setErrorEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}