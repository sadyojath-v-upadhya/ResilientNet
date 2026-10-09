package com.resilientnet.ui.chat;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.resilientnet.communication.CommunicationManager;
import com.resilientnet.data.local.entity.MessageEntity;
import com.resilientnet.data.repository.MessageRepository;
import com.resilientnet.utils.session.SessionManager;

import java.util.List;

public class ChatViewModel extends ViewModel {

    private final MessageRepository messageRepository;

    private final CommunicationManager communicationManager;

    private final SessionManager sessionManager;

    private final LiveData<List<MessageEntity>> messages;

    private final MutableLiveData<String> currentDeviceId =
            new MutableLiveData<>(null);

    public ChatViewModel(
            MessageRepository messageRepository,
            CommunicationManager communicationManager,
            Context context) {

        this.messageRepository =
                messageRepository;

        this.communicationManager =
                communicationManager;

        this.sessionManager =
                new SessionManager(context);

        messages =
                messageRepository.getAllMessages();

        // Get this device's unique ID
        currentDeviceId.setValue(
                sessionManager.getDeviceId()
        );
    }

    // ---------------------------------------------------------
    // MESSAGES
    // ---------------------------------------------------------

    public LiveData<List<MessageEntity>> getMessages() {

        return messages;
    }

    // ---------------------------------------------------------
    // CURRENT DEVICE ID
    // ---------------------------------------------------------

    public LiveData<String> getCurrentDeviceId() {

        return currentDeviceId;
    }

    public String getDeviceId() {

        return currentDeviceId.getValue();
    }

    // ---------------------------------------------------------
    // SEND MESSAGE
    // ---------------------------------------------------------

    public void sendMessage(
            MessageEntity message) {

        String deviceId =
                currentDeviceId.getValue();

        if (deviceId == null
                || deviceId.isEmpty()) {

            return;
        }

        // Attach human-readable sender identifier ("Name::deviceId")
        message.setSenderId(
                sessionManager.getSenderIdentifier()
        );

        communicationManager.sendMessage(
                message
        );
    }

    // ---------------------------------------------------------
    // CLEAR
    // ---------------------------------------------------------

    @Override
    protected void onCleared() {

        super.onCleared();
    }
}