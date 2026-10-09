package com.resilientnet.ui.map;

import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.resilientnet.data.local.dao.MessageDao;
import com.resilientnet.data.local.dao.SosDao;
import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.local.entity.MessageEntity;
import com.resilientnet.data.local.entity.SosEntity;
import com.resilientnet.ui.map.model.IncidentMarkerModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapViewModel extends AndroidViewModel {

    private final SosDao sosDao;
    private final MessageDao messageDao;

    private final LiveData<List<SosEntity>> sosWithLocation;
    private final LiveData<List<MessageEntity>> sosMessagesWithLocation;

    private final MutableLiveData<Location> userLocation = new MutableLiveData<>(null);
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("ALL");
    private final MutableLiveData<IncidentMarkerModel> selectedIncident = new MutableLiveData<>(null);

    private final MediatorLiveData<List<IncidentMarkerModel>> incidentMarkers = new MediatorLiveData<>();

    public MapViewModel(@NonNull Application application) {
        super(application);

        AppDatabase db = AppDatabase.getInstance(application);
        sosDao = db.sosDao();
        messageDao = db.messageDao();

        sosWithLocation = sosDao.getSosWithLocation();
        sosMessagesWithLocation = messageDao.getSosMessagesWithLocation();

        incidentMarkers.addSource(sosWithLocation, sosList -> recomputeIncidents());
        incidentMarkers.addSource(sosMessagesWithLocation, msgList -> recomputeIncidents());
        incidentMarkers.addSource(userLocation, loc -> recomputeIncidents());
        incidentMarkers.addSource(selectedCategory, cat -> recomputeIncidents());
    }

    public LiveData<List<IncidentMarkerModel>> getIncidentMarkers() {
        return incidentMarkers;
    }

    public LiveData<Location> getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(Location location) {
        userLocation.setValue(location);
    }

    public LiveData<String> getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String category) {
        selectedCategory.setValue(category != null ? category : "ALL");
    }

    public LiveData<IncidentMarkerModel> getSelectedIncident() {
        return selectedIncident;
    }

    public void setSelectedIncident(IncidentMarkerModel incident) {
        selectedIncident.setValue(incident);
    }

    private void recomputeIncidents() {
        List<SosEntity> localList = sosWithLocation.getValue();
        List<MessageEntity> msgList = sosMessagesWithLocation.getValue();
        Location currentLoc = userLocation.getValue();
        String currentCat = selectedCategory.getValue();
        if (currentCat == null) currentCat = "ALL";

        List<IncidentMarkerModel> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault());

        // 1. Process SosEntity records
        if (localList != null) {
            for (SosEntity entity : localList) {
                if (entity.getLatitude() == 0.0 && entity.getLongitude() == 0.0) {
                    continue;
                }

                IncidentMarkerModel model = new IncidentMarkerModel(
                        "sos_" + entity.getId(),
                        entity.getEmergencyType(),
                        entity.getMessage(),
                        entity.getLocationName(),
                        entity.getLatitude(),
                        entity.getLongitude(),
                        entity.getTimestamp(),
                        System.currentTimeMillis(),
                        0,
                        "Local Device",
                        entity.getStatus(),
                        true
                );
                result.add(model);
            }
        }

        // 2. Process MessageEntity records (Mesh forwarded)
        if (msgList != null) {
            for (MessageEntity msg : msgList) {
                Double lat = msg.getLatitude();
                Double lng = msg.getLongitude();
                if (lat == null || lng == null || (lat == 0.0 && lng == 0.0)) {
                    continue;
                }

                // Check for deduplication with existing local incident within ~15 meters (~0.00015 deg)
                boolean alreadyAdded = false;
                for (IncidentMarkerModel existing : result) {
                    double dLat = Math.abs(existing.getLatitude() - lat);
                    double dLng = Math.abs(existing.getLongitude() - lng);
                    if (dLat < 0.00015 && dLng < 0.00015) {
                        alreadyAdded = true;
                        break;
                    }
                }

                if (!alreadyAdded) {
                    String rawContent = msg.getContent() != null ? msg.getContent() : "SOS";
                    String emergencyType = "SOS Emergency";
                    String desc = "";

                    if (rawContent.contains(": ")) {
                        String[] parts = rawContent.split(": ", 2);
                        emergencyType = parts[0];
                        desc = parts[1];
                    } else {
                        emergencyType = rawContent;
                    }

                    String timeStr = sdf.format(new Date(msg.getTimestamp()));
                    String locName = String.format(Locale.US, "Lat: %.5f, Lng: %.5f", lat, lng);

                    IncidentMarkerModel model = new IncidentMarkerModel(
                            msg.getMessageId(),
                            emergencyType,
                            desc,
                            locName,
                            lat,
                            lng,
                            timeStr,
                            msg.getTimestamp(),
                            msg.getHopCount(),
                            msg.getSenderId(),
                            msg.getStatus(),
                            msg.getHopCount() == 0
                    );
                    result.add(model);
                }
            }
        }

        // 3. Compute distance to current user GPS location
        if (currentLoc != null) {
            float[] dist = new float[1];
            for (IncidentMarkerModel incident : result) {
                Location.distanceBetween(
                        currentLoc.getLatitude(),
                        currentLoc.getLongitude(),
                        incident.getLatitude(),
                        incident.getLongitude(),
                        dist
                );
                incident.setDistanceMeters(dist[0]);
            }
        }

        // 4. Apply category filter
        List<IncidentMarkerModel> filtered = new ArrayList<>();
        for (IncidentMarkerModel incident : result) {
            if ("ALL".equalsIgnoreCase(currentCat)) {
                filtered.add(incident);
            } else if (incident.getCategoryNormalized().equalsIgnoreCase(currentCat)) {
                filtered.add(incident);
            }
        }

        // 5. Sort by distance (if available) or by ID/timestamp
        Collections.sort(filtered, (a, b) -> {
            if (a.getDistanceMeters() >= 0 && b.getDistanceMeters() >= 0) {
                return Float.compare(a.getDistanceMeters(), b.getDistanceMeters());
            }
            return Long.compare(b.getRawTimestamp(), a.getRawTimestamp());
        });

        incidentMarkers.setValue(filtered);
    }
}
