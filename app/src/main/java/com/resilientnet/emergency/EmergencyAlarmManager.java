package com.resilientnet.emergency;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.resilientnet.MainActivity;
import com.resilientnet.R;
import com.resilientnet.data.local.entity.MessageEntity;

public class EmergencyAlarmManager {

    private static final String TAG = "EmergencyAlarmManager";
    public static final String CHANNEL_ID = "resilientnet_emergency_channel";
    private static final int NOTIFICATION_ID = 9999;
    private static final long AUTO_SILENCE_DELAY_MS = 30000; // 30 seconds max duration

    // SOS Morse Code: 3 Short (200ms), 3 Long (500ms), 3 Short (200ms), followed by 1s pause
    private static final long[] SOS_VIBRATION_PATTERN = new long[]{
            0,
            200, 100, 200, 100, 200, 300,
            500, 150, 500, 150, 500, 300,
            200, 100, 200, 100, 200, 1000
    };

    private static volatile EmergencyAlarmManager instance;

    private final Context context;
    private final Handler mainHandler;
    private final MutableLiveData<EmergencyAlertEvent> alertEventLiveData = new MutableLiveData<>(null);

    private MediaPlayer mediaPlayer;
    private ToneGenerator fallbackToneGen;
    private Vibrator vibrator;
    private boolean isAlarmActive = false;

    private final Runnable autoSilenceRunnable = this::stopAlarm;

    private EmergencyAlarmManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initVibrator();
        createNotificationChannel();
    }

    public static EmergencyAlarmManager getInstance(Context context) {
        if (instance == null) {
            synchronized (EmergencyAlarmManager.class) {
                if (instance == null) {
                    instance = new EmergencyAlarmManager(context);
                }
            }
        }
        return instance;
    }

    public LiveData<EmergencyAlertEvent> getAlertEventLiveData() {
        return alertEventLiveData;
    }

    public boolean isAlarmActive() {
        return isAlarmActive;
    }

    private void initVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                vibrator = vm.getDefaultVibrator();
            }
        }
        if (vibrator == null) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "ResilientNet Emergency Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Critical audio and haptic sirens for incoming emergency mesh SOS packets");
                channel.enableLights(true);
                channel.setLightColor(Color.RED);
                channel.enableVibration(true);
                channel.setVibrationPattern(SOS_VIBRATION_PATTERN);
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build();

                Uri alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alertUri == null) {
                    alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                channel.setSound(alertUri, audioAttributes);

                nm.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Trigger high-priority emergency audio, haptic, and visual alert from an incoming MessageEntity.
     */
    public void triggerAlarm(MessageEntity message) {
        if (message == null) return;

        String rawContent = message.getContent() != null ? message.getContent() : "SOS Emergency";
        String emergencyType = "SOS Emergency";
        String desc = rawContent;

        if (rawContent.contains(": ")) {
            String[] parts = rawContent.split(": ", 2);
            emergencyType = parts[0];
            desc = parts[1];
        }

        triggerAlarm(
                message.getMessageId(),
                emergencyType,
                desc,
                message.getLatitude(),
                message.getLongitude(),
                message.getHopCount(),
                message.getSenderId(),
                message.getTimestamp()
        );
    }

    /**
     * Trigger high-priority emergency audio, haptic, and visual alert with full arguments.
     */
    public void triggerAlarm(
            String messageId,
            String emergencyType,
            String description,
            Double latitude,
            Double longitude,
            int hopCount,
            String senderId,
            long timestamp) {

        mainHandler.post(() -> {
            Log.w(TAG, "🚨 TRIGGERING CRITICAL EMERGENCY ALARM: " + emergencyType + " [" + description + "]");

            isAlarmActive = true;

            // 1. Start audio siren
            startAudioSiren();

            // 2. Start SOS vibration
            startVibration();

            // 3. Post event for in-app dialog
            EmergencyAlertEvent event = new EmergencyAlertEvent(
                    messageId,
                    emergencyType,
                    description,
                    latitude,
                    longitude,
                    hopCount,
                    senderId,
                    timestamp
            );
            alertEventLiveData.setValue(event);

            // 4. Dispatch heads-up notification
            dispatchNotification(event);

            // 5. Schedule auto-silence timeout (30 seconds)
            mainHandler.removeCallbacks(autoSilenceRunnable);
            mainHandler.postDelayed(autoSilenceRunnable, AUTO_SILENCE_DELAY_MS);
        });
    }

    private void startAudioSiren() {
        stopAudio();

        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context, alarmUri);

            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            mediaPlayer.setAudioAttributes(aa);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.d(TAG, "Audio siren MediaPlayer started playing.");

        } catch (Exception e) {
            Log.e(TAG, "Failed to play alarm via MediaPlayer, falling back to ToneGenerator", e);
            try {
                if (fallbackToneGen == null) {
                    fallbackToneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                }
                fallbackToneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 15000);
            } catch (Exception toneEx) {
                Log.e(TAG, "ToneGenerator fallback also failed", toneEx);
            }
        }
    }

    private void startVibration() {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        try {
            vibrator.cancel();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createWaveform(SOS_VIBRATION_PATTERN, 0); // repeat from index 0
                AudioAttributes aa = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                vibrator.vibrate(effect, aa);
            } else {
                vibrator.vibrate(SOS_VIBRATION_PATTERN, 0);
            }
            Log.d(TAG, "SOS haptic vibration started.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start vibration", e);
        }
    }

    private void dispatchNotification(EmergencyAlertEvent event) {
        try {
            // Intent to open MainActivity and jump directly to Map
            Intent mapIntent = new Intent(context, MainActivity.class);
            mapIntent.setAction(Intent.ACTION_VIEW);
            mapIntent.putExtra("EXTRA_NAVIGATE_TO", "MAP");
            mapIntent.putExtra("EXTRA_LATITUDE", event.getLatitude() != null ? event.getLatitude() : 0.0);
            mapIntent.putExtra("EXTRA_LONGITUDE", event.getLongitude() != null ? event.getLongitude() : 0.0);
            mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingMapIntent = PendingIntent.getActivity(
                    context,
                    1001,
                    mapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Intent to silence alarm via broadcast receiver
            Intent silenceIntent = new Intent(context, EmergencyAlarmReceiver.class);
            silenceIntent.setAction(EmergencyAlarmReceiver.ACTION_SILENCE_ALARM);

            PendingIntent pendingSilenceIntent = PendingIntent.getBroadcast(
                    context,
                    1002,
                    silenceIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            String relayInfo = event.getHopCount() == 0 ? "Direct Alert" : ("Mesh: " + event.getHopCount() + " hops");
            String subText = event.getDescription().isEmpty()
                    ? "Immediate assistance required"
                    : event.getDescription();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_emergency)
                    .setContentTitle("🚨 EMERGENCY SOS: " + event.getEmergencyType())
                    .setContentText(subText + " (" + relayInfo + ")")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(subText + "\nOrigin: " + com.resilientnet.ui.chat.MessageAdapter.extractSenderDisplayName(event.getSenderId()) + " • " + relayInfo))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setColor(Color.parseColor("#DC2626"))
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setContentIntent(pendingMapIntent)
                    .addAction(R.drawable.ic_map, "VIEW ON MAP", pendingMapIntent)
                    .addAction(R.drawable.ic_close, "SILENCE ALARM", pendingSilenceIntent);

            NotificationManagerCompat nmc = NotificationManagerCompat.from(context);
            nmc.notify(NOTIFICATION_ID, builder.build());

        } catch (SecurityException se) {
            Log.w(TAG, "Notification permission not granted, heads-up notification skipped", se);
        } catch (Exception e) {
            Log.e(TAG, "Error posting emergency notification", e);
        }
    }

    /**
     * Stop and silence audio siren, vibration, and auto-timeout.
     */
    public void stopAlarm() {
        mainHandler.post(() -> {
            Log.d(TAG, "Silencing emergency alarm...");
            isAlarmActive = false;

            stopAudio();
            stopVibration();

            mainHandler.removeCallbacks(autoSilenceRunnable);

            // Remove or update ongoing notification
            try {
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
            } catch (Exception ignored) {
            }
        });
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }

        if (fallbackToneGen != null) {
            try {
                fallbackToneGen.stopTone();
                fallbackToneGen.release();
            } catch (Exception ignored) {
            }
            fallbackToneGen = null;
        }
    }

    private void stopVibration() {
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception ignored) {
            }
        }
    }
}
