package com.resilientnet.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.resilientnet.R;

public class Onboarding1Fragment extends Fragment {

    public Onboarding1Fragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_onboarding1,
                container,
                false);

        MaterialButton btnNext =
                view.findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_onboarding1_to_onboarding2));

        return view;
    }
}