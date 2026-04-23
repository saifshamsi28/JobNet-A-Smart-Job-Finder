package com.jobnet.app.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.jobnet.app.R;
import com.jobnet.app.data.network.ApiClient;
import com.jobnet.app.data.network.JobNetApiService;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.session.NotificationReadStateStore;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.ui.main.SplashActivity;

import java.util.List;
import java.util.Locale;

import retrofit2.Response;

public class NotificationSyncWorker extends Worker {

    private static final String PREFS_NAME = "JobNetNotificationSync";

    public NotificationSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SessionManager session = new SessionManager(context);

        if (!session.hasSession()) {
            return Result.success();
        }

        String role = session.getUserRole();
        if (role != null && role.toUpperCase(Locale.ROOT).contains("RECRUITER")) {
            return Result.success();
        }

        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return Result.success();
        }

        String token = session.getAuthToken();
        String userId = session.getUserId();
        if (token == null || token.isBlank() || userId == null || userId.isBlank()) {
            return Result.success();
        }

        ApiClient.initialize(context);
        JobNetApiService api = ApiClient.getApiService();
        NotificationReadStateStore readStateStore = new NotificationReadStateStore(context);

        try {
            Response<List<ApplicationDto>> response = api.getMyApplications("Bearer " + token, userId).execute();
            if (!response.isSuccessful() || response.body() == null) {
                return Result.success();
            }

            List<ApplicationDto> applications = response.body();
            if (applications.isEmpty()) {
                return Result.success();
            }

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String seedKey = "seeded_" + userId;
            boolean seeded = prefs.getBoolean(seedKey, false);

            if (!seeded) {
                for (ApplicationDto app : applications) {
                    String status = normalizeStatus(app.status);
                    String readKey = NotificationReadStateStore.buildKey(
                            "SEEKER_STATUS",
                            app.id,
                            app.jobId,
                            status,
                            app.updatedAt,
                            app.appliedAt
                    );
                    readStateStore.markSystemNotified(readKey);
                }
                prefs.edit().putBoolean(seedKey, true).apply();
                return Result.success();
            }

            ensureChannel(context);
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);

            for (ApplicationDto app : applications) {
                String status = normalizeStatus(app.status);
                String readKey = NotificationReadStateStore.buildKey(
                        "SEEKER_STATUS",
                        app.id,
                        app.jobId,
                        status,
                        app.updatedAt,
                        app.appliedAt
                );

                if (readStateStore.isHidden(readKey) || readStateStore.isSystemNotified(readKey)) {
                    continue;
                }

                Intent intent = new Intent(context, SplashActivity.class);
                intent.putExtra(NotificationConstants.EXTRA_OPEN_APPLICATION_TIMELINE, true);
                intent.putExtra(NotificationConstants.EXTRA_APPLICATION_ID, app.id);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context,
                        readKey.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                String title = "APPLIED".equals(status)
                        ? context.getString(R.string.notification_application_received)
                        : context.getString(R.string.notification_status_changed);

                String body = context.getString(
                        R.string.notification_seeker_message,
                        valueOrDefault(app.jobTitle, context.getString(R.string.notification_unknown_job)),
                        status.replace('_', ' ')
                );

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID_UPDATES)
                        .setSmallIcon(R.drawable.ic_bell)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

                manager.notify(readKey.hashCode(), builder.build());
                readStateStore.markSystemNotified(readKey);
            }
        } catch (Exception ignored) {
            return Result.success();
        }

        return Result.success();
    }

    private void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel existing = manager.getNotificationChannel(NotificationConstants.CHANNEL_ID_UPDATES);
        if (existing != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                NotificationConstants.CHANNEL_ID_UPDATES,
                NotificationConstants.CHANNEL_NAME_UPDATES,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Job application status changes and recruiter updates");
        manager.createNotificationChannel(channel);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "APPLIED";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("UNDER_REVIEW".equals(normalized) || "IN_REVIEW".equals(normalized)) {
            return "REVIEWED";
        }
        return normalized;
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
