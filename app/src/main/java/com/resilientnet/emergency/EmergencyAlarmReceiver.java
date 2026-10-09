package com.resilientnet.emergency;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EmergencyAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "EmergencyAlarmReceiver";
    public static final String ACTION_SILENCE_ALARM = "com.resilientnet.ACTION_SILENCE_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        Log.d(TAG, "Received broadcast action: " + action);

        if (ACTION_SILENCE_ALARM.equals(action)) {
            EmergencyAlarmManager.getInstance(context).stopAlarm();
        }
    }
}
