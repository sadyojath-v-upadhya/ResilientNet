package com.resilientnet.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.resilientnet.R;
import com.resilientnet.communication.CommunicationManager;
import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.local.entity.MessageEntity;
import com.resilientnet.data.repository.MessageRepository;
import com.resilientnet.databinding.FragmentChatBinding;

import java.util.UUID;
import com.resilientnet.MainActivity;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;

    private ChatViewModel chatViewModel;
    private MessageAdapter messageAdapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding =
                FragmentChatBinding.inflate(
                        inflater,
                        container,
                        false
                );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(
                view,
                savedInstanceState
        );

        // ---------------------------------------------------------
        // DATABASE
        // ---------------------------------------------------------

        MainActivity mainActivity =
                (MainActivity) requireActivity();

        CommunicationManager communicationManager =
                mainActivity.getCommunicationManager();

        AppDatabase database =
                AppDatabase.getInstance(requireContext());

        MessageRepository messageRepository =
                new MessageRepository(
                        database.messageDao(),
                        database.userDao()
                );

        // ---------------------------------------------------------
        // VIEWMODEL
        // ---------------------------------------------------------

        ChatViewModelFactory factory =
                new ChatViewModelFactory(
                        messageRepository,
                        communicationManager,
                        requireContext()
                );

        chatViewModel =
                new ViewModelProvider(
                        this,
                        factory
                ).get(ChatViewModel.class);

        // ---------------------------------------------------------
        // SETUP
        // ---------------------------------------------------------

        setupRecyclerView();
        observeMessages();
        observeCurrentDeviceId();
        setupSendButton();
    }

    // ---------------------------------------------------------
    // RECYCLER VIEW
    // ---------------------------------------------------------

    private void setupRecyclerView() {

        messageAdapter =
                new MessageAdapter();

        if (chatViewModel != null && chatViewModel.getDeviceId() != null) {
            messageAdapter.setCurrentDeviceId(
                    chatViewModel.getDeviceId()
            );
        }

        binding.messagesRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()
                )
        );

        binding.messagesRecyclerView.setAdapter(
                messageAdapter
        );
    }

    // ---------------------------------------------------------
    // OBSERVE MESSAGES
    // ---------------------------------------------------------

    private void observeMessages() {

        chatViewModel
                .getMessages()
                .observe(
                        getViewLifecycleOwner(),
                        messages -> {

                            messageAdapter.setMessages(
                                    messages
                            );

                            if (messages != null
                                    && !messages.isEmpty()) {

                                binding.messagesRecyclerView
                                        .scrollToPosition(
                                                messages.size() - 1
                                        );
                            }
                        }
                );
    }

    // ---------------------------------------------------------
    // OBSERVE CURRENT DEVICE ID
    // ---------------------------------------------------------

    private void observeCurrentDeviceId() {

        chatViewModel
                .getCurrentDeviceId()
                .observe(
                        getViewLifecycleOwner(),
                        deviceId -> {

                            if (deviceId != null
                                    && !deviceId.isEmpty()) {

                                android.util.Log.d(
                                        "ChatFragment",
                                        "Current device ID = "
                                                + deviceId
                                );

                                messageAdapter
                                        .setCurrentDeviceId(
                                                deviceId
                                        );
                            }
                        }
                );
    }

    // ---------------------------------------------------------
    // SEND BUTTON
    // ---------------------------------------------------------

    private void setupSendButton() {

        binding.sendButton.setOnClickListener(
                v -> sendMessage()
        );
    }

    // ---------------------------------------------------------
    // SEND MESSAGE
    // ---------------------------------------------------------

    private void sendMessage() {

        String content =
                binding.messageInput
                        .getText()
                        .toString()
                        .trim();

        if (content.isEmpty()) {
            return;
        }

        MessageEntity message =
                new MessageEntity();

        // Unique message ID
        message.setMessageId(
                UUID.randomUUID().toString()
        );

        message.setContent(
                content
        );

        message.setMessageType(
                "NORMAL_MESSAGE"
        );

        message.setStatus(
                "PENDING"
        );

        message.setPriority(
                0
        );

        message.setHopCount(
                0
        );

        message.setTimestamp(
                System.currentTimeMillis()
        );

        /*
         * Sender ID is NOT set here.
         *
         * ChatViewModel will set it using:
         *
         * SessionManager.getDeviceId()
         */

        chatViewModel.sendMessage(
                message
        );

        binding.messageInput.setText("");
    }

    // ---------------------------------------------------------
    // DESTROY
    // ---------------------------------------------------------

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        binding = null;
    }
}