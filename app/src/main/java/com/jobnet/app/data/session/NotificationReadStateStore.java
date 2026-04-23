package com.jobnet.app.data.session;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationReadStateStore {

    private static final String PREF_NAME = "JobNetNotificationRead";

    private final SharedPreferences prefs;
    private final String userScope;

    public NotificationReadStateStore(Context context) {
        Context appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SessionManager session = new SessionManager(appContext);
        String userId = session.getUserId();
        if (userId == null || userId.isBlank()) {
            userId = "anonymous";
        }
        this.userScope = userId;
    }

    public boolean isRead(String notificationKey) {
        if (notificationKey == null || notificationKey.isBlank()) {
            return false;
        }
        return prefs.getBoolean(scopedKey(notificationKey), false);
    }

    public void markRead(String notificationKey) {
        if (notificationKey == null || notificationKey.isBlank()) {
            return;
        }
        prefs.edit().putBoolean(scopedKey(notificationKey), true).apply();
    }

    public boolean isHidden(String notificationKey) {
        if (notificationKey == null || notificationKey.isBlank()) {
            return false;
        }
        return prefs.getBoolean(scopedHiddenKey(notificationKey), false);
    }

    public void markHidden(String notificationKey) {
        if (notificationKey == null || notificationKey.isBlank()) {
            return;
        }
        prefs.edit().putBoolean(scopedHiddenKey(notificationKey), true).apply();
    }

    public void clearHidden() {
        String prefix = userScope + "|HIDDEN|";
        removeByPrefix(prefix);
    }

    public void clearRead() {
        String prefix = userScope + "|";
        removeByPrefix(prefix);
    }

    public boolean isSystemNotified(String notificationKey) {
        if (notificationKey == null || notificationKey.isBlank()) {
            return false;
        }
        return prefs.getBoolean(scopedNotifiedKey(notificationKey), false);
    }

    public void markSystemNotified(String notificationKey) {
        if (notificationKey == null || notificationKey.isBlank()) {
            return;
        }
        prefs.edit().putBoolean(scopedNotifiedKey(notificationKey), true).apply();
    }

    public static String buildKey(String type,
                                  String applicationId,
                                  String jobId,
                                  String status,
                                  String updatedAt,
                                  String appliedAt) {
        String safeType = normalize(type);
        String safeAppId = normalize(applicationId);
        String safeJobId = normalize(jobId);
        String safeStatus = normalize(status);
        String safeTimestamp = normalize(updatedAt);
        if (safeTimestamp.isEmpty()) {
            safeTimestamp = normalize(appliedAt);
        }
        return safeType + "|" + safeAppId + "|" + safeJobId + "|" + safeStatus + "|" + safeTimestamp;
    }

    private String scopedKey(String key) {
        return userScope + "|" + key;
    }

    private String scopedHiddenKey(String key) {
        return userScope + "|HIDDEN|" + key;
    }

    private String scopedNotifiedKey(String key) {
        return userScope + "|NOTIFIED|" + key;
    }

    private void removeByPrefix(String prefix) {
        Map<String, ?> all = prefs.getAll();
        if (all == null || all.isEmpty()) {
            return;
        }
        List<String> keysToRemove = new ArrayList<>();
        for (String key : all.keySet()) {
            if (key != null && key.startsWith(prefix)) {
                keysToRemove.add(key);
            }
        }
        if (keysToRemove.isEmpty()) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : keysToRemove) {
            editor.remove(key);
        }
        editor.apply();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
