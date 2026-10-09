package com.resilientnet.ui.contacts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resilientnet.R;
import com.resilientnet.databinding.DialogAddEmergencyContactBinding;
import com.resilientnet.databinding.FragmentEmergencyContactsBinding;
import com.resilientnet.ui.contacts.adapter.EmergencyContactsAdapter;
import com.resilientnet.ui.contacts.model.EmergencyContact;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactsFragment extends Fragment implements EmergencyContactsAdapter.OnContactActionListener {

    private FragmentEmergencyContactsBinding binding;
    private EmergencyContactsManager contactsManager;
    private EmergencyContactsAdapter adapter;
    private String currentFilter = "ALL";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEmergencyContactsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        contactsManager = new EmergencyContactsManager(requireContext());

        setupToolbar();
        setupRecyclerView();
        setupFilterChips();
        setupFab();
        loadContacts();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
    }

    private void setupRecyclerView() {
        adapter = new EmergencyContactsAdapter(this);
        binding.rvContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvContacts.setAdapter(adapter);
    }

    private void setupFilterChips() {
        binding.chipGroupContacts.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipNationalHelplines) {
                currentFilter = "NATIONAL";
            } else if (checkedId == R.id.chipPersonalContacts) {
                currentFilter = "PERSONAL";
            } else {
                currentFilter = "ALL";
            }
            loadContacts();
        });
    }

    private void setupFab() {
        binding.fabAddContact.setOnClickListener(v -> showAddContactDialog());
    }

    private void loadContacts() {
        List<EmergencyContact> list;
        if ("NATIONAL".equals(currentFilter)) {
            list = contactsManager.getPreloadedContacts();
        } else if ("PERSONAL".equals(currentFilter)) {
            list = contactsManager.getPersonalContacts();
        } else {
            list = contactsManager.getAllContacts();
        }

        adapter.setContacts(list);

        if (list.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvContacts.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvContacts.setVisibility(View.VISIBLE);
        }
    }

    private void showAddContactDialog() {
        DialogAddEmergencyContactBinding dialogBinding = DialogAddEmergencyContactBinding.inflate(
                LayoutInflater.from(requireContext())
        );

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Personal Emergency Contact")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Save", null) // Overridden below to prevent auto-close on validation failure
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
                String name = dialogBinding.etContactName.getText() != null
                        ? dialogBinding.etContactName.getText().toString().trim() : "";
                String phone = dialogBinding.etContactPhone.getText() != null
                        ? dialogBinding.etContactPhone.getText().toString().trim() : "";
                String role = dialogBinding.etContactRole.getText() != null
                        ? dialogBinding.etContactRole.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    dialogBinding.layoutContactName.setError("Name is required");
                    return;
                }
                dialogBinding.layoutContactName.setError(null);

                if (phone.isEmpty()) {
                    dialogBinding.layoutContactPhone.setError("Phone number is required");
                    return;
                }
                dialogBinding.layoutContactPhone.setError(null);

                contactsManager.addPersonalContact(
                        name,
                        phone,
                        "Personal Contact",
                        role.isEmpty() ? "Emergency Contact" : role
                );

                Toast.makeText(requireContext(), "Contact saved", Toast.LENGTH_SHORT).show();
                loadContacts();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    @Override
    public void onCall(EmergencyContact contact) {
        String phone = contact.getPhoneNumber();
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(requireContext(), "No phone number available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + phone));
            startActivity(dialIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open phone dialer", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDelete(EmergencyContact contact) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Contact")
                .setMessage("Remove \"" + contact.getName() + "\" from emergency contacts?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    contactsManager.deletePersonalContact(contact.getId());
                    loadContacts();
                    Toast.makeText(requireContext(), "Contact removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
