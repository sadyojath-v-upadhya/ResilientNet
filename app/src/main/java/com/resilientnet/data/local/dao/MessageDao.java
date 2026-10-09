package com.resilientnet.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.resilientnet.data.local.entity.MessageEntity;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(MessageEntity message);

    @Update
    void updateMessage(MessageEntity message);

    @Delete
    void deleteMessage(MessageEntity message);

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    LiveData<List<MessageEntity>> getAllMessages();

    @Query("SELECT * FROM messages WHERE message_id = :messageId LIMIT 1")
    MessageEntity getMessageById(String messageId);

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE message_id = :messageId)")
    boolean messageExists(String messageId);

    @Query("SELECT * FROM messages WHERE sender_id = :senderId ORDER BY timestamp ASC")
    LiveData<List<MessageEntity>> getMessagesBySender(String senderId);

    @Query("SELECT * FROM messages WHERE receiver_id = :receiverId ORDER BY timestamp ASC")
    LiveData<List<MessageEntity>> getMessagesByReceiver(String receiverId);

    @Query("SELECT * FROM messages WHERE status = 'PENDING' ORDER BY timestamp ASC")
    List<MessageEntity> getPendingMessages();

    @Query("SELECT * FROM messages WHERE message_type = 'SOS' AND latitude IS NOT NULL AND longitude IS NOT NULL AND latitude != 0.0 AND longitude != 0.0 ORDER BY timestamp DESC")
    LiveData<List<MessageEntity>> getSosMessagesWithLocation();

    @Query("DELETE FROM messages")
    void deleteAllMessages();
}