package com.resilientnet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resilientnet.communication.CommunicationManager;
import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.repository.MessageRepository;
import com.resilientnet.emergency.EmergencyAlarmManager;
import com.resilientnet.emergency.EmergencyAlertEvent;
import com.resilientnet.ui.dashboard.DashboardFragment;

public class MainActivity extends AppCompatActivity {

    private CommunicationManager communicationManager;
    private androidx.appcompat.app.AlertDialog currentEmergencyDialog;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        // Granted or denied, notifications will be attempted
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        AppDatabase database =
                AppDatabase.getInstance(this);

        MessageRepository messageRepository =
                new MessageRepository(
                        database.messageDao(),
                        database.userDao()
                );

        communicationManager =
                new CommunicationManager(
                        getApplicationContext(),
                        messageRepository
                );

        checkNotificationPermission();
        observeEmergencyAlerts();
        handleIncomingIntent(getIntent());

        // Cache user full name from local database into SessionManager if logged in
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            com.resilientnet.utils.session.SessionManager sm =
                    new com.resilientnet.utils.session.SessionManager(getApplicationContext());
            String email = sm.getLoggedInEmail();
            if (email != null && (sm.getUserName() == null || sm.getUserName().trim().isEmpty())) {
                com.resilientnet.data.local.entity.User user = database.userDao().getUserByEmail(email);
                if (user != null && user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
                    sm.saveUserName(user.getFullName().trim());
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;

        if ("MAP".equals(intent.getStringExtra("EXTRA_NAVIGATE_TO"))) {
            // Silence alarm if active
            EmergencyAlarmManager.getInstance(this).stopAlarm();
            navigateToMap();
        }
    }

    private void observeEmergencyAlerts() {
        EmergencyAlarmManager.getInstance(this).getAlertEventLiveData().observe(this, event -> {
            if (event != null && !isFinishing() && !isDestroyed()) {
                showInAppEmergencyDialog(event);
            }
        });
    }

    private void showInAppEmergencyDialog(EmergencyAlertEvent event) {
        if (currentEmergencyDialog != null && currentEmergencyDialog.isShowing()) {
            currentEmergencyDialog.dismiss();
        }

        String relayText = event.getHopCount() == 0 ? "Direct Alert" : (event.getHopCount() + " Hops over Mesh");
        String originName = com.resilientnet.ui.chat.MessageAdapter.extractSenderDisplayName(event.getSenderId());
        String messageBody = "A critical emergency alert was received across the ResilientNet mesh network:\n\n"
                + "• Emergency: " + event.getEmergencyType() + "\n"
                + "• Message: " + (event.getDescription().isEmpty() ? "Immediate help requested" : event.getDescription()) + "\n"
                + "• Origin: " + originName + "\n"
                + "• Relay Status: " + relayText;

        currentEmergencyDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("🚨 CRITICAL SOS ALERT RECEIVED")
                .setMessage(messageBody)
                .setIcon(R.drawable.ic_emergency)
                .setCancelable(false)
                .setPositiveButton("View on Map", (dialog, which) -> {
                    EmergencyAlarmManager.getInstance(this).stopAlarm();
                    navigateToMap();
                })
                .setNegativeButton("Silence Alarm", (dialog, which) -> {
                    EmergencyAlarmManager.getInstance(this).stopAlarm();
                })
                .show();
    }

    public void navigateToMap() {
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            for (Fragment f : navHostFragment.getChildFragmentManager().getFragments()) {
                if (f instanceof DashboardFragment) {
                    ((DashboardFragment) f).selectTab(R.id.mapFragment);
                    return;
                }
            }
        }
    }

    public CommunicationManager getCommunicationManager() {
        return communicationManager;
    }

    @Override
    protected void onDestroy() {
        if (currentEmergencyDialog != null && currentEmergencyDialog.isShowing()) {
            currentEmergencyDialog.dismiss();
        }

        if (communicationManager != null) {
            communicationManager.shutdown();
        }

        super.onDestroy();
    }
}