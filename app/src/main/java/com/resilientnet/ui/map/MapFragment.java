package com.resilientnet.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.resilientnet.R;
import com.resilientnet.databinding.FragmentMapBinding;
import com.resilientnet.ui.map.model.IncidentMarkerModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    private MapViewModel viewModel;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private Marker userLocationMarker;
    private final FolderOverlay incidentsOverlay = new FolderOverlay();
    private Polyline vectorPolyline;

    private boolean isInitialCameraCentered = false;
    private Location currentGpsLocation = null;
    private IncidentMarkerModel currentlySelectedIncident = null;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            startLocationUpdates();
                        } else {
                            Toast.makeText(
                                    requireContext(),
                                    "Location permission needed to show your position on map",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure Osmdroid offline tile caching using app internal cache dir
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());

        File osmdroidBasePath = new File(ctx.getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(osmdroidBasePath);
        File osmdroidTileCache = new File(osmdroidBasePath, "tiles");
        Configuration.getInstance().setOsmdroidTileCache(osmdroidTileCache);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MapViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupMapView();
        setupControls();
        setupCategoryChips();
        observeViewModel();
        checkAndRequestLocation();
    }

    private void setupMapView() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.setMultiTouchControls(true);
        binding.mapView.setBuiltInZoomControls(false);

        // Default initial view: Bangalore coordinates (or any neutral center) until GPS arrives
        GeoPoint defaultCenter = new GeoPoint(12.9716, 77.5946);
        binding.mapView.getController().setZoom(14.0);
        binding.mapView.getController().setCenter(defaultCenter);

        // Add overlays
        binding.mapView.getOverlays().add(incidentsOverlay);
    }

    private void setupControls() {
        binding.fabZoomIn.setOnClickListener(v -> binding.mapView.getController().zoomIn());
        binding.fabZoomOut.setOnClickListener(v -> binding.mapView.getController().zoomOut());

        binding.fabMyLocation.setOnClickListener(v -> centerOnUserLocation());

        binding.btnClosePreview.setOnClickListener(v -> {
            binding.cardIncidentPreview.setVisibility(View.GONE);
            removeVectorPolyline();
        });

        binding.btnFocusIncident.setOnClickListener(v -> {
            if (currentlySelectedIncident != null) {
                GeoPoint target = new GeoPoint(
                        currentlySelectedIncident.getLatitude(),
                        currentlySelectedIncident.getLongitude()
                );
                binding.mapView.getController().setZoom(17.0);
                binding.mapView.getController().animateTo(target);
            }
        });

        binding.btnNavigateIncident.setOnClickListener(v -> {
            if (currentlySelectedIncident != null && currentGpsLocation != null) {
                drawVectorLineToIncident(currentlySelectedIncident);
            } else {
                Toast.makeText(
                        requireContext(),
                        "Acquiring user GPS coordinates for direct vector...",
                        Toast.LENGTH_SHORT
                ).show();
                checkAndRequestLocation();
            }
        });
    }

    private void setupCategoryChips() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);

            String category = "ALL";
            if (checkedId == R.id.chipFilterMedical) {
                category = "Medical";
            } else if (checkedId == R.id.chipFilterFire) {
                category = "Fire";
            } else if (checkedId == R.id.chipFilterFlood) {
                category = "Flood";
            } else if (checkedId == R.id.chipFilterEarthquake) {
                category = "Earthquake";
            } else if (checkedId == R.id.chipFilterOther) {
                category = "Other";
            }

            viewModel.setSelectedCategory(category);
        });
    }

    private void observeViewModel() {
        viewModel.getIncidentMarkers().observe(getViewLifecycleOwner(), incidents -> {
            if (incidents != null) {
                renderIncidentMarkers(incidents);
                binding.tvIncidentCountBadge.setText(
                        incidents.size() + (incidents.size() == 1 ? " Incident" : " Incidents")
                );
            }
        });

        viewModel.getSelectedIncident().observe(getViewLifecycleOwner(), incident -> {
            if (incident != null) {
                showIncidentPreview(incident);
            }
        });
    }

    private void renderIncidentMarkers(List<IncidentMarkerModel> incidents) {
        incidentsOverlay.getItems().clear();

        for (IncidentMarkerModel incident : incidents) {
            Marker marker = new Marker(binding.mapView);
            GeoPoint pos = new GeoPoint(incident.getLatitude(), incident.getLongitude());
            marker.setPosition(pos);
            marker.setTitle(incident.getEmergencyType());
            marker.setSnippet(incident.getDescription());

            Drawable markerDrawable = createIncidentMarkerDrawable(
                    incident.getColorInt()
            );
            marker.setIcon(markerDrawable);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            marker.setOnMarkerClickListener((m, mapView) -> {
                viewModel.setSelectedIncident(incident);
                mapView.getController().animateTo(pos);
                return true;
            });

            incidentsOverlay.add(marker);
        }

        binding.mapView.invalidate();
    }

    private void showIncidentPreview(IncidentMarkerModel incident) {
        currentlySelectedIncident = incident;

        binding.tvPreviewType.setText(incident.getEmergencyType());
        binding.tvPreviewType.setTextColor(incident.getColorInt());

        binding.tvPreviewDistance.setText(incident.getFormattedDistance());

        if (incident.getHopCount() == 0) {
            binding.tvPreviewHops.setText("Direct (0 hops)");
            binding.tvPreviewHops.setTextColor(ContextCompat.getColor(requireContext(), R.color.success));
        } else {
            binding.tvPreviewHops.setText(String.format(Locale.getDefault(), "Mesh: %d %s",
                    incident.getHopCount(), incident.getHopCount() == 1 ? "hop" : "hops"));
            binding.tvPreviewHops.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        }

        String desc = incident.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            desc = "Immediate emergency assistance required.";
        }
        binding.tvPreviewDescription.setText(desc);

        String locText = String.format(Locale.US, "Lat: %.5f, Lng: %.5f",
                incident.getLatitude(), incident.getLongitude());
        if (incident.getLocationName() != null && !incident.getLocationName().isEmpty()) {
            locText = incident.getLocationName() + " (" + locText + ")";
        }
        binding.tvPreviewLocation.setText(locText);

        binding.tvPreviewTimestamp.setText(incident.getTimestamp());

        binding.cardIncidentPreview.setVisibility(View.VISIBLE);
    }

    private void drawVectorLineToIncident(IncidentMarkerModel incident) {
        removeVectorPolyline();

        if (currentGpsLocation == null) return;

        vectorPolyline = new Polyline();
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(currentGpsLocation.getLatitude(), currentGpsLocation.getLongitude()));
        points.add(new GeoPoint(incident.getLatitude(), incident.getLongitude()));
        vectorPolyline.setPoints(points);

        vectorPolyline.setColor(Color.parseColor("#2563EB"));
        vectorPolyline.setWidth(6.0f);

        binding.mapView.getOverlays().add(vectorPolyline);
        binding.mapView.invalidate();

        // Fit bounds between both points
        GeoPoint midPoint = new GeoPoint(
                (currentGpsLocation.getLatitude() + incident.getLatitude()) / 2.0,
                (currentGpsLocation.getLongitude() + incident.getLongitude()) / 2.0
        );
        binding.mapView.getController().animateTo(midPoint);
    }

    private void removeVectorPolyline() {
        if (vectorPolyline != null) {
            binding.mapView.getOverlays().remove(vectorPolyline);
            vectorPolyline = null;
            binding.mapView.invalidate();
        }
    }

    private void checkAndRequestLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Fetch last known location first for immediate responsiveness
        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                updateUserLocation(loc);
            }
        });

        // Register recurring location updates
        locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000
        )
                .setMinUpdateIntervalMillis(1000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    updateUserLocation(loc);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    private void updateUserLocation(Location location) {
        currentGpsLocation = location;
        viewModel.setUserLocation(location);

        GeoPoint userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

        if (userLocationMarker == null) {
            userLocationMarker = new Marker(binding.mapView);
            userLocationMarker.setIcon(createUserLocationDrawable());
            userLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            userLocationMarker.setTitle("My Location");
            userLocationMarker.setSnippet(String.format(Locale.US, "Lat: %.5f, Lng: %.5f",
                    location.getLatitude(), location.getLongitude()));
            binding.mapView.getOverlays().add(userLocationMarker);
        }

        userLocationMarker.setPosition(userPoint);

        if (!isInitialCameraCentered) {
            isInitialCameraCentered = true;
            binding.mapView.getController().setZoom(16.0);
            binding.mapView.getController().animateTo(userPoint);
        }

        binding.mapView.invalidate();
    }

    private void centerOnUserLocation() {
        if (currentGpsLocation != null) {
            GeoPoint userPoint = new GeoPoint(
                    currentGpsLocation.getLatitude(),
                    currentGpsLocation.getLongitude()
            );
            binding.mapView.getController().setZoom(16.0);
            binding.mapView.getController().animateTo(userPoint);
        } else {
            Toast.makeText(
                    requireContext(),
                    "Acquiring GPS location...",
                    Toast.LENGTH_SHORT
            ).show();
            checkAndRequestLocation();
        }
    }

    private Drawable createIncidentMarkerDrawable(int colorInt) {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (38 * density);
        int totalHeight = (int) (48 * density);

        Bitmap bitmap = Bitmap.createBitmap(size, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Draw pin shadow
        paint.setColor(Color.parseColor("#33000000"));
        canvas.drawCircle(size / 2.0f, size / 2.0f + (3 * density), (size / 2.0f) - (2 * density), paint);

        // Draw pin body circle
        paint.setColor(colorInt);
        canvas.drawCircle(size / 2.0f, size / 2.0f, (size / 2.0f) - (3 * density), paint);

        // White border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f * density);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2.0f, size / 2.0f, (size / 2.0f) - (4 * density), paint);

        // Center white dot
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2.0f, size / 2.0f, 4 * density, paint);

        // Pin bottom pointer triangle
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo((size / 2.0f) - (6 * density), size - (6 * density));
        path.lineTo((size / 2.0f) + (6 * density), size - (6 * density));
        path.lineTo(size / 2.0f, totalHeight - (2 * density));
        path.close();
        paint.setColor(colorInt);
        canvas.drawPath(path, paint);

        return new BitmapDrawable(getResources(), bitmap);
    }

    private Drawable createUserLocationDrawable() {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (34 * density);

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Outer pulsing/radar ring
        paint.setColor(Color.parseColor("#442563EB"));
        canvas.drawCircle(size / 2.0f, size / 2.0f, size / 2.0f, paint);

        // Inner solid blue circle
        paint.setColor(Color.parseColor("#2563EB"));
        canvas.drawCircle(size / 2.0f, size / 2.0f, (size / 2.0f) - (5 * density), paint);

        // White border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f * density);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2.0f, size / 2.0f, (size / 2.0f) - (5 * density), paint);

        // Center white dot
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2.0f, size / 2.0f, 3 * density, paint);

        return new BitmapDrawable(getResources(), bitmap);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            binding.mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null) {
            binding.mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        if (locationCallback != null && fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (binding != null) {
            binding.mapView.onDetach();
            binding = null;
        }

        super.onDestroyView();
    }
}