package com.resilientnet.ui.auth.login;

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
import com.resilientnet.R;
import com.resilientnet.databinding.FragmentLoginBinding;
import com.resilientnet.ui.auth.login.viewmodel.LoginViewModel;
import com.resilientnet.utils.session.SessionManager;
import com.resilientnet.utils.validation.ValidationUtils;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentLoginBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(this)
                .get(LoginViewModel.class);

        observeViewModel();

        setupClickListeners();

        return binding.getRoot();
    }

    private void observeViewModel() {

        viewModel.getLoginSuccess().observe(
                getViewLifecycleOwner(),
                user -> {

                    SessionManager sessionManager =
                            new SessionManager(requireContext());

                    sessionManager.saveLogin(user.getEmail(), user.getFullName());

                    Snackbar.make(
                            binding.getRoot(),
                            "Login Successful",
                            Snackbar.LENGTH_SHORT
                    ).show();

                    // We'll replace this with Home later
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_login_to_dashboard);

                });

        viewModel.getLoginError().observe(
                getViewLifecycleOwner(),
                error -> {

                    if ("Incorrect password.".equals(error)) {

                        binding.passwordLayout.setError(error);

                    } else if ("No account found with this email.".equals(error)) {

                        binding.emailLayout.setError(error);

                    } else {

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

        binding.txtRegister.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_login_to_register));

        binding.btnLogin.setOnClickListener(v -> {

            binding.emailLayout.setError(null);
            binding.passwordLayout.setError(null);

            String email =
                    binding.etEmail.getText().toString().trim();

            String password =
                    binding.etPassword.getText().toString();

            String error;

            error = ValidationUtils.validateEmail(email);

            if (error != null) {
                binding.emailLayout.setError(error);
                return;
            }

            error = ValidationUtils.validatePassword(password);

            if (error != null) {
                binding.passwordLayout.setError(error);
                return;
            }

            viewModel.login(email, password);

        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}