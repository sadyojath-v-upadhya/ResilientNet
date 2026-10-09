package com.resilientnet.ui.map.model;

import android.graphics.Color;

import java.util.Locale;

public class IncidentMarkerModel {

    private final String id;
    private final String emergencyType;
    private final String description;
    private final String locationName;
    private final double latitude;
    private final double longitude;
    private final String timestamp;
    private final long rawTimestamp;
    private final int hopCount;
    private final String senderId;
    private final String status;
    private final boolean isLocal;
    private float distanceMeters = -1f;

    public IncidentMarkerModel(
            String id,
            String emergencyType,
            String description,
            String locationName,
            double latitude,
            double longitude,
            String timestamp,
            long rawTimestamp,
            int hopCount,
            String senderId,
            String status,
            boolean isLocal) {
        this.id = id;
        this.emergencyType = emergencyType != null && !emergencyType.isEmpty() ? emergencyType : "Emergency Alert";
        this.description = description != null ? description : "";
        this.locationName = locationName != null ? locationName : "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp != null ? timestamp : "";
        this.rawTimestamp = rawTimestamp;
        this.hopCount = hopCount;
        this.senderId = senderId != null ? senderId : "";
        this.status = status != null ? status : "PENDING";
        this.isLocal = isLocal;
    }

    public String getId() {
        return id;
    }

    public String getEmergencyType() {
        return emergencyType;
    }

    public String getDescription() {
        return description;
    }

    public String getLocationName() {
        return locationName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public long getRawTimestamp() {
        return rawTimestamp;
    }

    public int getHopCount() {
        return hopCount;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getStatus() {
        return status;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public float getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(float distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public String getFormattedDistance() {
        if (distanceMeters < 0) {
            return "Distance unknown";
        }
        if (distanceMeters < 1000) {
            return String.format(Locale.getDefault(), "%.0f m away", distanceMeters);
        } else {
            return String.format(Locale.getDefault(), "%.1f km away", distanceMeters / 1000.0f);
        }
    }

    public String getCategoryNormalized() {
        String typeLower = emergencyType.toLowerCase(Locale.ROOT);
        if (typeLower.contains("medical")) return "Medical";
        if (typeLower.contains("fire")) return "Fire";
        if (typeLower.contains("flood")) return "Flood";
        if (typeLower.contains("earthquake")) return "Earthquake";
        return "Other";
    }

    public int getColorInt() {
        String category = getCategoryNormalized();
        switch (category) {
            case "Medical":
                return Color.parseColor("#E53935"); // Vibrant Red
            case "Fire":
                return Color.parseColor("#F4511E"); // Deep Orange
            case "Flood":
                return Color.parseColor("#0288D1"); // Deep Cyan/Blue
            case "Earthquake":
                return Color.parseColor("#8D6E63"); // Deep Earth Brown
            default:
                return Color.parseColor("#E65100"); // Alert Amber-Orange
        }
    }
}
