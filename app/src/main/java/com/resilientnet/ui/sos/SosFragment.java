package com.resilientnet.ui.sos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.resilientnet.MainActivity;
import com.resilientnet.databinding.FragmentSosBinding;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resilientnet.data.local.entity.SosEntity;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import com.resilientnet.communication.CommunicationManager;
import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.local.entity.MessageEntity;
import com.resilientnet.data.model.MessageConstants;
import com.resilientnet.data.repository.MessageRepository;
import com.resilientnet.utils.session.SessionManager;

import java.util.UUID;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;

import com.google.android.gms.location.Priority;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;

public class SosFragment extends Fragment {

    private FragmentSosBinding binding;
    private SosViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    private double latitude = 0.0;
    private double longitude = 0.0;
    private LocationRequest locationRequest;

    private LocationCallback locationCallback;

    private boolean waitingToSaveSOS = false;
    private CommunicationManager communicationManager;
    private SessionManager sessionManager;
    private String locationName = "Unknown Location";
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentSosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SosViewModel.class);

        MainActivity mainActivity =
                (MainActivity) requireActivity();

        communicationManager =
                mainActivity.getCommunicationManager();

        sessionManager =
                new SessionManager(requireContext());
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireActivity());
        initializeLocationRequest();
        checkLocationPermission();

        initializeViews();
        setupEmergencyTypeDropdown();
        setupClickListeners();
        generateTimestamp();
        requestLocation();
    }

    private void fetchLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null && latitude == 0.0 && longitude == 0.0) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            if (binding != null) {
                                binding.tvLatitude.setText("Lat : " + latitude);
                                binding.tvLongitude.setText("Lng : " + longitude);
                                binding.tvGpsStatus.setText("GPS Connected");
                                getLocationName(latitude, longitude);
                            }
                        }
                    });
        }
    }

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {

                        if (isGranted) {

                            fetchLastKnownLocation();
                            requestLocation();

                        } else {

                            binding.tvGpsStatus.setText("Location Permission Denied");
                        }
                    });

    private void checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fetchLastKnownLocation();
            requestLocation();

        } else {

            locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION);

        }
    }

    private void initializeLocationRequest() {

        locationRequest =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        1000
                )
                        .setMinUpdateIntervalMillis(500)
                        .setWaitForAccurateLocation(true)
                        .setMaxUpdates(1)
                        .build();

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult result) {

                if (result == null)
                    return;

                Location location = result.getLastLocation();

                if (location == null)
                    return;

                latitude = location.getLatitude();
                longitude = location.getLongitude();

                binding.tvLatitude.setText("Lat : " + latitude);
                binding.tvLongitude.setText("Lng : " + longitude);

                binding.tvGpsStatus.setText("GPS Connected");

                getLocationName(latitude, longitude);

                if (waitingToSaveSOS) {

                    waitingToSaveSOS = false;

                    saveLocationAndSos();
                }

                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        };
    }

    private void requestLocation() {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        binding.tvGpsStatus.setText("Getting GPS...");

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    private void getLocationName(double latitude, double longitude) {

        Geocoder geocoder =
                new Geocoder(requireContext(), Locale.getDefault());

        try {

            List<Address> addresses =
                    geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {

                Address address = addresses.get(0);

                locationName = address.getAddressLine(0);

                binding.tvLocation.setText(locationName);

            }

        } catch (IOException e) {

            binding.tvLocation.setText("Location unavailable");
        }
    }
    private void initializeViews() {

        binding.tvLocation.setText("Searching for your location...");
        binding.tvLatitude.setText("Lat : --");
        binding.tvLongitude.setText("Lng : --");
        binding.tvGpsStatus.setText("Waiting for GPS...");
        binding.tvNetworkStatus.setText("Offline Ready");

    }

    private void setupEmergencyTypeDropdown() {

        String[] emergencyTypes = {
                "Medical Emergency",
                "Fire",
                "Flood",
                "Earthquake",
                "Cyclone",
                "Landslide",
                "Building Collapse",
                "Missing Person",
                "Other"
        };

        android.widget.ArrayAdapter<String> adapter =
                new android.widget.ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        emergencyTypes
                );

        binding.actEmergencyType.setAdapter(adapter);

    }

    private void clearForm() {

        binding.actEmergencyType.setText("");

        binding.etEmergencyMessage.setText("");

        generateTimestamp();

    }
    private void saveSOS() {

        generateTimestamp();

        if (latitude != 0.0 || longitude != 0.0) {
            saveLocationAndSos();
            return;
        }

        binding.btnSOS.setEnabled(false);

        waitingToSaveSOS = true;

        requestLocation();
    }

    private void requestFreshLocationAndSave() {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            binding.btnSOS.setEnabled(true);

            Toast.makeText(
                    requireContext(),
                    "Location permission required",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        binding.tvGpsStatus.setText("Getting current location...");

        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        new CancellationToken() {
                            @Override
                            public boolean isCancellationRequested() {
                                return false;
                            }

                            @NonNull
                            @Override
                            public CancellationToken onCanceledRequested(
                                    @NonNull OnTokenCanceledListener listener) {
                                return this;
                            }
                        }
                )
                .addOnSuccessListener(location -> {

                    if (location == null) {

                        binding.btnSOS.setEnabled(true);

                        Toast.makeText(
                                requireContext(),
                                "Unable to obtain current location",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    binding.tvLatitude.setText("Lat : " + latitude);
                    binding.tvLongitude.setText("Lng : " + longitude);

                    binding.tvGpsStatus.setText("GPS Connected");

                    saveLocationAndSos();

                })
                .addOnFailureListener(e -> {

                    binding.btnSOS.setEnabled(true);

                    Toast.makeText(
                            requireContext(),
                            "Failed to get GPS location",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void saveLocationAndSos() {

        String emergencyType =
                binding.actEmergencyType
                        .getText()
                        .toString()
                        .trim();

        String message =
                binding.etEmergencyMessage
                        .getText()
                        .toString()
                        .trim();

        String timestamp =
                binding.tvTimestamp
                        .getText()
                        .toString();

        // ---------------------------------------------------------
        // 1. Save SOS locally
        // ---------------------------------------------------------

        SosEntity sosEntity =
                new SosEntity(
                        emergencyType,
                        message,
                        locationName,
                        latitude,
                        longitude,
                        timestamp,
                        MessageConstants.PENDING,
                        false
                );

        viewModel.insert(sosEntity);

        // ---------------------------------------------------------
        // 2. Create network message
        // ---------------------------------------------------------

        MessageEntity sosMessage =
                new MessageEntity();

        sosMessage.setMessageId(
                UUID.randomUUID().toString()
        );

        sosMessage.setSenderId(
                sessionManager.getSenderIdentifier()
        );

        sosMessage.setMessageType(
                MessageConstants.SOS
        );

        String content;

        if (message.isEmpty()) {
            content = emergencyType;
        } else {
            content =
                    emergencyType
                            + ": "
                            + message;
        }

        sosMessage.setContent(content);

        sosMessage.setTimestamp(
                System.currentTimeMillis()
        );

        sosMessage.setLatitude(latitude);

        sosMessage.setLongitude(longitude);

        sosMessage.setPriority(
                MessageConstants.PRIORITY_EMERGENCY
        );

        sosMessage.setStatus(
                MessageConstants.PENDING
        );

        sosMessage.setHopCount(0);

        // ---------------------------------------------------------
        // 3. Send through EXISTING Bluetooth + multihop system
        // ---------------------------------------------------------

        communicationManager.sendMessage(
                sosMessage
        );

        binding.btnSOS.setEnabled(true);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("SOS Sent")
                .setMessage(
                        "Emergency SOS created successfully.\n\n" +
                                "The SOS has been stored locally and transmitted " +
                                "through the ResilientNet network."
                )
                .setPositiveButton("OK", null)
                .show();

        clearForm();
    }
    private void validateAndConfirmSOS() {

        String emergencyType =
                binding.actEmergencyType.getText().toString().trim();

        if (emergencyType.isEmpty()) {

            binding.layoutEmergencyType.setError(
                    "Please select an emergency type"
            );
            return;
        }
        String message =
                binding.etEmergencyMessage.getText().toString().trim();

        if (message.length() > 300) {

            binding.etEmergencyMessage.setError(
                    "Maximum 300 characters"
            );

            return;
        }

        binding.layoutEmergencyType.setError(null);

        String confirmationMessage = (latitude == 0.0 && longitude == 0.0)
                ? "GPS coordinates not yet acquired. Do you want to send this emergency alert anyway?"
                : "Are you sure you want to send this emergency alert?";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Send SOS")
                .setMessage(confirmationMessage)
                .setPositiveButton("Send", (dialog, which) -> saveSOS())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupClickListeners() {

        binding.btnSOS.setOnClickListener(v -> validateAndConfirmSOS());

    }

    private void generateTimestamp() {

        java.text.SimpleDateFormat formatter =
                new java.text.SimpleDateFormat(
                        "dd MMM yyyy • hh:mm a",
                        java.util.Locale.getDefault());

        binding.tvTimestamp.setText(
                formatter.format(new java.util.Date())
        );

    }

    @Override
    public void onDestroyView() {

        if (locationCallback != null && fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        super.onDestroyView();

        binding = null;
    }
}
