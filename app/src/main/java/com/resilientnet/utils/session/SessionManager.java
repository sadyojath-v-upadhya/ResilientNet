package com.resilientnet.utils.session;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class SessionManager {

    private static final String PREF_NAME = "resilientnet_pref";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_EMAIL = "email";

    private static final String KEY_FIRST_LAUNCH = "first_launch";

    private final SharedPreferences preferences;

    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_BACKEND_URL = "backend_url";
    private static final String DEFAULT_BACKEND_URL = "http://10.0.2.2:8080/api/emergency/sync";

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    private static final String KEY_USER_NAME = "user_name";

    public void saveLogin(String email) {

        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public void saveLogin(String email, String userName) {

        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_EMAIL, email)
                .putString(KEY_USER_NAME, userName)
                .apply();
    }

    public void saveUserName(String userName) {
        preferences.edit()
                .putString(KEY_USER_NAME, userName)
                .apply();
    }

    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, null);
    }

    public String getUserDisplayName() {
        String name = preferences.getString(KEY_USER_NAME, null);
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }

        String email = getLoggedInEmail();
        if (email != null && !email.trim().isEmpty()) {
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                String prefix = email.substring(0, atIndex);
                if (!prefix.isEmpty()) {
                    return Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
                }
            }
            return email;
        }

        String model = android.os.Build.MODEL;
        if (model != null && !model.trim().isEmpty()) {
            return model.trim();
        }

        return "Peer Node";
    }

    public String getSenderIdentifier() {
        String name = getUserDisplayName();
        String deviceId = getDeviceId();
        if (name != null && !name.trim().isEmpty()) {
            return name + "::" + deviceId;
        }
        return deviceId;
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getLoggedInEmail() {
        return preferences.getString(KEY_EMAIL, null);
    }

    public boolean isFirstLaunch() {
        return preferences.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchCompleted() {
        preferences.edit()
                .putBoolean(KEY_FIRST_LAUNCH, false)
                .apply();
    }

    public String getDeviceId() {

        String deviceId =
                preferences.getString(KEY_DEVICE_ID, null);

        if (deviceId == null) {

            deviceId = UUID.randomUUID().toString();

            preferences.edit()
                    .putString(KEY_DEVICE_ID, deviceId)
                    .apply();
        }

        return deviceId;
    }

    public String getBackendApiUrl() {
        return preferences.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL);
    }

    public void setBackendApiUrl(String url) {
        preferences.edit()
                .putString(KEY_BACKEND_URL, url)
                .apply();
    }

    public void logout() {

        preferences.edit()
                .remove(KEY_LOGGED_IN)
                .remove(KEY_EMAIL)
                .remove(KEY_USER_NAME)
                .apply();
    }
}