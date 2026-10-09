package com.resilientnet.ui.chat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.resilientnet.communication.CommunicationManager;
import com.resilientnet.data.repository.MessageRepository;

public class ChatViewModelFactory
        implements ViewModelProvider.Factory {

    private final MessageRepository messageRepository;

    private final CommunicationManager communicationManager;

    private final Context context;

    public ChatViewModelFactory(
            MessageRepository messageRepository,
            CommunicationManager communicationManager,
            Context context) {

        this.messageRepository =
                messageRepository;

        this.communicationManager =
                communicationManager;

        this.context =
                context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(
            @NonNull Class<T> modelClass) {

        if (modelClass.isAssignableFrom(
                ChatViewModel.class)) {

            return (T) new ChatViewModel(
                    messageRepository,
                    communicationManager,
                    context
            );
        }

        throw new IllegalArgumentException(
                "Unknown ViewModel class"
        );
    }
}