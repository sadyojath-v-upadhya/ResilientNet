package com.resilientnet.emergency;

public class EmergencyAlertEvent {

    private final String messageId;
    private final String emergencyType;
    private final String description;
    private final Double latitude;
    private final Double longitude;
    private final int hopCount;
    private final String senderId;
    private final long timestamp;

    public EmergencyAlertEvent(
            String messageId,
            String emergencyType,
            String description,
            Double latitude,
            Double longitude,
            int hopCount,
            String senderId,
            long timestamp) {
        this.messageId = messageId != null ? messageId : "";
        this.emergencyType = emergencyType != null ? emergencyType : "Emergency Alert";
        this.description = description != null ? description : "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.hopCount = hopCount;
        this.senderId = senderId != null ? senderId : "";
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getEmergencyType() {
        return emergencyType;
    }

    public String getDescription() {
        return description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public int getHopCount() {
        return hopCount;
    }

    public String getSenderId() {
        return senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
