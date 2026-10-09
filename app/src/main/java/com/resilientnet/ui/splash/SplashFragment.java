package com.resilientnet.ui.splash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.resilientnet.R;
import com.resilientnet.utils.session.SessionManager;

public class SplashFragment extends Fragment {

    private SessionManager sessionManager;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_splash,
                container,
                false
        );

        sessionManager = new SessionManager(requireContext());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            if (sessionManager.isLoggedIn()) {

                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_splash_to_dashboard);

            } else if (sessionManager.isFirstLaunch()) {

                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_splash_to_onboarding);

            } else {

                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_splash_to_login);

            }

        }, 2000);

        return view;
    }
}