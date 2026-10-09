package com.resilientnet.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sos_requests")
public class SosEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    @ColumnInfo(name = "emergency_type")
    private String emergencyType;

    @ColumnInfo(name = "message")
    private String message;

    @ColumnInfo(name = "location_name")
    private String locationName;

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "longitude")
    private double longitude;

    @NonNull
    @ColumnInfo(name = "timestamp")
    private String timestamp;

    @NonNull
    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "is_synced")
    private boolean synced;

    public SosEntity(
            @NonNull String emergencyType,
            String message,
            String locationName,
            double latitude,
            double longitude,
            @NonNull String timestamp,
            @NonNull String status,
            boolean synced
    ) {
        this.emergencyType = emergencyType;
        this.message = message;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.status = status;
        this.synced = synced;
    }

    // ---------- Getters ----------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getEmergencyType() {
        return emergencyType;
    }

    public void setEmergencyType(@NonNull String emergencyType) {
        this.emergencyType = emergencyType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @NonNull
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@NonNull String timestamp) {
        this.timestamp = timestamp;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }
}