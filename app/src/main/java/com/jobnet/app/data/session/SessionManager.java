package com.jobnet.app.data.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "JobNetSession";
    private static final String LEGACY_PREF_NAME = "JobNetPrefs";
    private static final String KEY_TOKEN = "jwtToken";
    private static final String KEY_FALLBACK_TOKEN = "token";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ROLE = "userRole";

    private final SharedPreferences prefs;
    private final SharedPreferences legacyPrefs;

    public SessionManager(Context context) {
        Context appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.legacyPrefs = appContext.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getAuthToken() {
        String token = prefs.getString(KEY_TOKEN, null);
        if (token == null || token.isBlank()) {
            token = prefs.getString(KEY_FALLBACK_TOKEN, null);
        }
        if (token == null || token.isBlank()) {
            token = legacyPrefs.getString(KEY_TOKEN, null);
        }
        if (token == null || token.isBlank()) {
            token = legacyPrefs.getString(KEY_FALLBACK_TOKEN, null);
        }
        return token;
    }

    public String getUserId() {
        String userId = prefs.getString(KEY_USER_ID, null);
        if (userId == null || userId.isBlank()) {
            userId = legacyPrefs.getString(KEY_USER_ID, null);
        }
        return userId;
    }

    public boolean hasSession() {
        String token = getAuthToken();
        return token != null && !token.isBlank();
    }

    public void saveAuthToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_FALLBACK_TOKEN, token).apply();
        legacyPrefs.edit().putString(KEY_TOKEN, token).putString(KEY_FALLBACK_TOKEN, token).apply();
    }

    public void saveUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        prefs.edit().putString(KEY_USER_ID, userId).apply();
        legacyPrefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    public void saveUserIdentity(String userName, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        SharedPreferences.Editor legacyEditor = legacyPrefs.edit();

        if (userName != null && !userName.isBlank()) {
            editor.putString(KEY_USER_NAME, userName);
            legacyEditor.putString(KEY_USER_NAME, userName);
        }
        if (email != null && !email.isBlank()) {
            editor.putString(KEY_USER_EMAIL, email);
            legacyEditor.putString(KEY_USER_EMAIL, email);
        }

        editor.apply();
        legacyEditor.apply();
    }

    public String getUserName() {
        String value = prefs.getString(KEY_USER_NAME, null);
        if (value == null || value.isBlank()) {
            value = legacyPrefs.getString(KEY_USER_NAME, null);
        }
        return value;
    }

    public String getUserEmail() {
        String value = prefs.getString(KEY_USER_EMAIL, null);
        if (value == null || value.isBlank()) {
            value = legacyPrefs.getString(KEY_USER_EMAIL, null);
        }
        return value;
    }

    public void saveUserRole(String role) {
        if (role == null || role.isBlank()) {
            return;
        }
        prefs.edit().putString(KEY_USER_ROLE, role).apply();
        legacyPrefs.edit().putString(KEY_USER_ROLE, role).apply();
    }

    public String getUserRole() {
        String value = prefs.getString(KEY_USER_ROLE, null);
        if (value == null || value.isBlank()) {
            value = legacyPrefs.getString(KEY_USER_ROLE, null);
        }
        return value;
    }

    public void clear() {
        prefs.edit().clear().apply();
        legacyPrefs.edit().clear().apply();
    }
}
