package com.resilientnet.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.resilientnet.R;
import com.resilientnet.utils.session.SessionManager;
public class Onboarding3Fragment extends Fragment {

    public Onboarding3Fragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_onboarding3,
                container,
                false);

        MaterialButton btnGetStarted = view.findViewById(R.id.btnNext);

        btnGetStarted.setOnClickListener(v -> {

            SessionManager sessionManager = new SessionManager(requireContext());

            // Mark onboarding as completed
            sessionManager.setFirstLaunchCompleted();

            NavHostFragment.findNavController(Onboarding3Fragment.this)
                    .navigate(R.id.action_onboarding3_to_login);

        });

        return view;
    }
}