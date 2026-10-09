package com.resilientnet.ui.contacts.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.resilientnet.R;
import com.resilientnet.databinding.ItemEmergencyContactBinding;
import com.resilientnet.ui.contacts.model.EmergencyContact;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder> {

    public interface OnContactActionListener {
        void onCall(EmergencyContact contact);
        void onDelete(EmergencyContact contact);
    }

    private final List<EmergencyContact> contacts = new ArrayList<>();
    private final OnContactActionListener listener;

    public EmergencyContactsAdapter(OnContactActionListener listener) {
        this.listener = listener;
    }

    public void setContacts(List<EmergencyContact> newContacts) {
        contacts.clear();
        if (newContacts != null) {
            contacts.addAll(newContacts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEmergencyContactBinding binding = ItemEmergencyContactBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ContactViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        holder.bind(contacts.get(position));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    class ContactViewHolder extends RecyclerView.ViewHolder {

        private final ItemEmergencyContactBinding binding;

        ContactViewHolder(ItemEmergencyContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(EmergencyContact contact) {
            binding.tvContactName.setText(contact.getName());
            binding.tvContactNumber.setText(contact.getPhoneNumber());
            binding.tvContactDescription.setText(contact.getDescription());

            if (contact.isPreloaded()) {
                binding.btnDeleteContact.setVisibility(View.GONE);
                binding.ivContactIcon.setImageResource(R.drawable.ic_emergency);
                binding.ivContactIcon.setColorFilter(
                        ContextCompat.getColor(itemView.getContext(), R.color.sos_red)
                );
            } else {
                binding.btnDeleteContact.setVisibility(View.VISIBLE);
                binding.ivContactIcon.setImageResource(R.drawable.ic_person);
                binding.ivContactIcon.setColorFilter(
                        ContextCompat.getColor(itemView.getContext(), R.color.primary)
                );
                binding.btnDeleteContact.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDelete(contact);
                    }
                });
            }

            binding.btnCallContact.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCall(contact);
                }
            });
        }
    }
}
