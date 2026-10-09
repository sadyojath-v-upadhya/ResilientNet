package com.resilientnet.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    private String messageId;

    @ColumnInfo(name = "sender_id")
    private String senderId;

    @ColumnInfo(name = "receiver_id")
    private String receiverId;

    @ColumnInfo(name = "message_type")
    private String messageType;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "latitude")
    private Double latitude;

    @ColumnInfo(name = "longitude")
    private Double longitude;

    @ColumnInfo(name = "priority")
    private int priority;

    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "hop_count")
    private int hopCount;

    public MessageEntity() {
    }

    // -------------------------
    // Message ID
    // -------------------------

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(@NonNull String messageId) {
        this.messageId = messageId;
    }

    // -------------------------
    // Sender ID
    // -------------------------

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    // -------------------------
    // Receiver ID
    // -------------------------

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    // -------------------------
    // Message Type
    // -------------------------

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    // -------------------------
    // Content
    // -------------------------

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // -------------------------
    // Timestamp
    // -------------------------

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // -------------------------
    // Latitude
    // -------------------------

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    // -------------------------
    // Longitude
    // -------------------------

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    // -------------------------
    // Priority
    // -------------------------

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    // -------------------------
    // Status
    // -------------------------

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // -------------------------
    // Hop Count
    // -------------------------

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }
}