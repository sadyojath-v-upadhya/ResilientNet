package com.resilientnet.data.repository;

import androidx.lifecycle.LiveData;

import com.resilientnet.data.local.dao.MessageDao;
import com.resilientnet.data.local.dao.UserDao;
import com.resilientnet.data.local.entity.MessageEntity;
import com.resilientnet.data.local.entity.User;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageRepository {

    private final MessageDao messageDao;
    private final UserDao userDao;

    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    public MessageRepository(
            MessageDao messageDao,
            UserDao userDao) {

        this.messageDao = messageDao;
        this.userDao = userDao;
    }

    // ---------------------------------------------------------
    // GET ALL MESSAGES
    // ---------------------------------------------------------

    public LiveData<List<MessageEntity>> getAllMessages() {

        return messageDao.getAllMessages();
    }

    // ---------------------------------------------------------
    // GET MESSAGES BY SENDER
    // ---------------------------------------------------------

    public LiveData<List<MessageEntity>> getMessagesBySender(
            String senderId) {

        return messageDao.getMessagesBySender(senderId);
    }

    // ---------------------------------------------------------
    // GET MESSAGES BY RECEIVER
    // ---------------------------------------------------------

    public LiveData<List<MessageEntity>> getMessagesByReceiver(
            String receiverId) {

        return messageDao.getMessagesByReceiver(receiverId);
    }

    // ---------------------------------------------------------
    // INSERT MESSAGE
    // ---------------------------------------------------------

    public void insertMessage(MessageEntity message) {

        databaseExecutor.execute(() ->
                messageDao.insertMessage(message)
        );
    }

    // ---------------------------------------------------------
    // UPDATE MESSAGE
    // ---------------------------------------------------------

    public void updateMessage(MessageEntity message) {

        databaseExecutor.execute(() ->
                messageDao.updateMessage(message)
        );
    }

    // ---------------------------------------------------------
    // DELETE MESSAGE
    // ---------------------------------------------------------

    public void deleteMessage(MessageEntity message) {

        databaseExecutor.execute(() ->
                messageDao.deleteMessage(message)
        );
    }

    // ---------------------------------------------------------
    // DELETE ALL MESSAGES
    // ---------------------------------------------------------

    public void deleteAllMessages() {

        databaseExecutor.execute(() ->
                messageDao.deleteAllMessages()
        );
    }

    // ---------------------------------------------------------
    // GET USER ID BY EMAIL
    // ---------------------------------------------------------

    public void getUserIdByEmail(
            String email,
            UserIdCallback callback) {

        databaseExecutor.execute(() -> {

            User user =
                    userDao.getUserByEmail(email);

            int userId = -1;

            if (user != null) {
                userId = user.getId();
            }

            callback.onUserIdLoaded(userId);
        });
    }

    // ---------------------------------------------------------
    // INSERT RECEIVED MESSAGE
    // ---------------------------------------------------------

    /*
     * Insert a received message only if it is new.
     *
     * callback result:
     *
     * true  = new message
     * false = duplicate message
     */

    public void insertReceivedMessage(
            MessageEntity message,
            MessageReceiveCallback callback) {

        databaseExecutor.execute(() -> {

            android.util.Log.d(
                    "ResilientNetRepository",
                    "Checking received message: "
                            + message.getMessageId()
                            + " type="
                            + message.getMessageType()
            );

            MessageEntity existing =
                    messageDao.getMessageById(
                            message.getMessageId()
                    );

            if (existing != null) {

                android.util.Log.d(
                        "ResilientNetRepository",
                        "DUPLICATE message: "
                                + message.getMessageId()
                );

                if (callback != null) {
                    callback.onMessageProcessed(false);
                }

                return;
            }

            android.util.Log.d(
                    "ResilientNetRepository",
                    "NEW message. Inserting: "
                            + message.getMessageId()
            );

            messageDao.insertMessage(message);

            android.util.Log.d(
                    "ResilientNetRepository",
                    "Message inserted successfully: "
                            + message.getMessageId()
            );

            if (callback != null) {

                android.util.Log.d(
                        "ResilientNetRepository",
                        "Calling onMessageProcessed(true)"
                );

                callback.onMessageProcessed(true);
            }
        });
    }

    // ---------------------------------------------------------
    // GET PENDING MESSAGES
    // ---------------------------------------------------------

    public void getPendingMessages(PendingMessagesCallback callback) {

        databaseExecutor.execute(() -> {

            List<MessageEntity> pending =
                    messageDao.getPendingMessages();

            if (callback != null) {
                callback.onPendingLoaded(pending);
            }
        });
    }

    // ---------------------------------------------------------
    // SHUTDOWN
    // ---------------------------------------------------------

    public void shutdown() {

        databaseExecutor.shutdown();
    }

    // ---------------------------------------------------------
    // CALLBACKS
    // ---------------------------------------------------------

    public interface PendingMessagesCallback {

        void onPendingLoaded(
                List<MessageEntity> pendingMessages
        );
    }

    public interface MessageReceiveCallback {

        void onMessageProcessed(
                boolean isNewMessage
        );
    }

    public interface UserIdCallback {

        void onUserIdLoaded(int userId);
    }
}