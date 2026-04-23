package com.jobnet.app.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.data.session.NotificationReadStateStore;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.util.DateTimeUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private JobNetRepository repository;
    private NotificationsAdapter adapter;
    private NotificationReadStateStore readStateStore;
    private boolean recruiterMode;
    private boolean skipNextResumeRefresh = true;
    private int notificationsRequestVersion = 0;
    private final List<NotificationItem> currentNotifications = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());
        readStateStore = new NotificationReadStateStore(requireContext());

        SessionManager session = new SessionManager(requireContext());
        String role = session.getUserRole();
        recruiterMode = role != null && role.toUpperCase(Locale.ROOT).contains("RECRUITER");

        // Mode label
        TextView modeLabel = view.findViewById(R.id.tv_notifications_mode_label);
        modeLabel.setText(recruiterMode
                ? getString(R.string.notifications_mode_recruiter)
                : getString(R.string.notifications_mode_seeker));

        // Subtitle
        TextView subtitle = view.findViewById(R.id.tv_notifications_subtitle);
        subtitle.setText(recruiterMode
                ? R.string.notifications_subtitle_recruiter
                : R.string.notifications_subtitle_seeker);

        // Back
        view.findViewById(R.id.btn_back_notifications).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        view.findViewById(R.id.btn_notifications_mark_read).setOnClickListener(v -> markAllAsRead());
        view.findViewById(R.id.btn_notifications_clear_all).setOnClickListener(v -> clearAllNotifications());

        // RecyclerView
        RecyclerView recycler = view.findViewById(R.id.recycler_notifications);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationsAdapter(this::handleNotificationClick);
        recycler.setAdapter(recycler.getAdapter() == null ? adapter : recycler.getAdapter());
        recycler.setAdapter(adapter);

        loadNotifications(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        View root = getView();
        if (root != null) {
            loadNotifications(root);
        }
    }

    private void loadNotifications(View root) {
        final int requestVersion = ++notificationsRequestVersion;
        View empty   = root.findViewById(R.id.layout_notifications_empty);
        RecyclerView recycler = root.findViewById(R.id.recycler_notifications);
        TextView emptyTitle   = root.findViewById(R.id.tv_notifications_empty_title);
        TextView emptyMsg     = root.findViewById(R.id.tv_notifications_empty_message);

        empty.setVisibility(View.GONE);
        recycler.setVisibility(View.VISIBLE);
        adapter.showSkeleton(true);

        if (recruiterMode) {
            emptyTitle.setText(R.string.notifications_empty_title);
            emptyMsg.setText(R.string.notifications_empty_recruiter);
            loadRecruiterNotifications(root, requestVersion);
        } else {
            emptyTitle.setText(R.string.notifications_empty_title);
            emptyMsg.setText(R.string.notifications_empty_seeker);
            loadSeekerNotifications(root, requestVersion);
        }
    }

    // ── Seeker ────────────────────────────────────────────────────────────────

    private void loadSeekerNotifications(View root, int requestVersion) {
        View empty    = root.findViewById(R.id.layout_notifications_empty);
        RecyclerView recycler = root.findViewById(R.id.recycler_notifications);

        repository.loadMyApplications(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<ApplicationDto> data) {
                if (!isActiveNotificationRequest(root, requestVersion)) {
                    return;
                }
                List<NotificationItem> notifications = new ArrayList<>();
                if (data != null) {
                    for (ApplicationDto app : data) {
                        String status  = normalizeStatus(app.status);
                        String title   = "APPLIED".equals(status)
                                ? getString(R.string.notification_application_received)
                                : getString(R.string.notification_status_changed);
                        String readKey = NotificationReadStateStore.buildKey(
                            NotificationItem.TYPE_SEEKER_STATUS,
                            app.id,
                            app.jobId,
                            status,
                            app.updatedAt,
                            app.appliedAt
                        );
                        if (readStateStore.isHidden(readKey)) {
                            continue;
                        }
                        boolean unread = !readStateStore.isRead(readKey);
                        String message = getString(R.string.notification_seeker_message,
                                defaultIfBlank(app.jobTitle, getString(R.string.notification_unknown_job)),
                                status.replace('_', ' '));
                        notifications.add(new NotificationItem(app.id, NotificationItem.TYPE_SEEKER_STATUS,
                                title, message, status,
                                formatTimestamp(app.updatedAt, app.appliedAt),
                            parseSortTime(app.updatedAt, app.appliedAt),
                            app.jobId, app.jobTitle, app.company,
                            readKey, unread));
                    }
                }
                sortNotifications(notifications);
                deliver(notifications, empty, recycler);
            }

            @Override
            public void onError(Throwable t) {
                if (!isActiveNotificationRequest(root, requestVersion)) {
                    return;
                }
                deliver(Collections.emptyList(), empty, recycler);
            }
        });
    }

    // ── Recruiter ─────────────────────────────────────────────────────────────

    private void loadRecruiterNotifications(View root, int requestVersion) {
        View empty    = root.findViewById(R.id.layout_notifications_empty);
        RecyclerView recycler = root.findViewById(R.id.recycler_notifications);

        repository.loadRecruiterPostedJobs(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Job> jobs) {
                if (!isActiveNotificationRequest(root, requestVersion)) {
                    return;
                }
                if (jobs == null || jobs.isEmpty()) {
                    deliver(Collections.emptyList(), empty, recycler);
                    return;
                }
                Map<String, String> jobTitleById = new HashMap<>();
                for (Job j : jobs) {
                    jobTitleById.put(j.getId(),
                            defaultIfBlank(j.getTitle(), getString(R.string.notification_unknown_job)));
                }
                List<NotificationItem> notifications = new ArrayList<>();
                int[] pending = {jobs.size()};
                for (Job job : jobs) {
                    repository.loadJobApplicants(job.getId(), new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(List<ApplicationDto> data) {
                            if (!isActiveNotificationRequest(root, requestVersion)) {
                                return;
                            }
                            if (data != null) {
                                for (ApplicationDto app : data) {
                                    String title   = getString(R.string.notification_new_applicant);
                                    String status  = normalizeStatus(app.status);
                                    String readKey = NotificationReadStateStore.buildKey(
                                        NotificationItem.TYPE_RECRUITER_APPLICANT,
                                        app.id,
                                        app.jobId,
                                        status,
                                        app.updatedAt,
                                        app.appliedAt
                                    );
                                    if (readStateStore.isHidden(readKey)) {
                                        continue;
                                    }
                                    boolean unread = !readStateStore.isRead(readKey);
                                    String message = getString(R.string.notification_recruiter_message,
                                            defaultIfBlank(jobTitleById.get(app.jobId),
                                                    getString(R.string.notification_unknown_job)));
                                    notifications.add(new NotificationItem(app.id,
                                            NotificationItem.TYPE_RECRUITER_APPLICANT,
                                        title, message, status,
                                            formatTimestamp(app.updatedAt, app.appliedAt),
                                        parseSortTime(app.updatedAt, app.appliedAt),
                                        app.jobId, jobTitleById.get(app.jobId), app.company,
                                        readKey, unread));
                                }
                            }
                            finalizeBatch(notifications, pending, empty, recycler, root, requestVersion);
                        }

                        @Override
                        public void onError(Throwable t) {
                            if (!isActiveNotificationRequest(root, requestVersion)) {
                                return;
                            }
                            finalizeBatch(notifications, pending, empty, recycler, root, requestVersion);
                        }
                    });
                }
            }

            @Override
            public void onError(Throwable t) {
                if (!isActiveNotificationRequest(root, requestVersion)) {
                    return;
                }
                deliver(Collections.emptyList(), empty, recycler);
            }
        });
    }

    private void finalizeBatch(List<NotificationItem> notifications, int[] pending,
                               View empty, RecyclerView recycler, View root, int requestVersion) {
        if (!isActiveNotificationRequest(root, requestVersion)) {
            return;
        }
        pending[0]--;
        if (pending[0] > 0) return;
        sortNotifications(notifications);
        deliver(notifications, empty, recycler);
    }

    private boolean isActiveNotificationRequest(View expectedRoot, int requestVersion) {
        return isAdded() && getView() == expectedRoot && requestVersion == notificationsRequestVersion;
    }

    private void deliver(List<NotificationItem> notifications, View empty, RecyclerView recycler) {
        currentNotifications.clear();
        currentNotifications.addAll(notifications);
        adapter.submitItems(notifications);
        boolean isEmpty = notifications.isEmpty();
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // ── Click ─────────────────────────────────────────────────────────────────

    private void handleNotificationClick(NotificationItem item) {
        if (item == null) return;
        if (item.unread && item.readKey != null && !item.readKey.isBlank()) {
            readStateStore.markRead(item.readKey);
            item.unread = false;
            adapter.notifyDataSetChanged();
        }
        if (NotificationItem.TYPE_RECRUITER_APPLICANT.equals(item.type)) {
            Bundle args = new Bundle();
            args.putString("jobId", item.jobId == null ? "" : item.jobId);
            args.putString("jobTitle", defaultIfBlank(item.jobTitle, getString(R.string.notification_unknown_job)));
            NavOptions options = new NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build();
            View root = getView();
            if (root != null) {
                Navigation.findNavController(root)
                        .navigate(R.id.recruiterApplicantsFragment, args, options);
            }
            return;
        }
        openSeekerTimeline(item);
    }

    private void openSeekerTimeline(NotificationItem item) {
        repository.loadMyApplications(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<ApplicationDto> data) {
                if (!isAdded()) {
                    return;
                }
                if (data != null) {
                    for (ApplicationDto app : data) {
                        if (app != null && item.id != null && item.id.equals(app.id)) {
                            Bundle args = new Bundle();
                            args.putSerializable("application", app);
                            NavOptions options = new NavOptions.Builder()
                                    .setEnterAnim(R.anim.slide_in_right)
                                    .setExitAnim(R.anim.slide_out_left)
                                    .setPopEnterAnim(R.anim.slide_in_left)
                                    .setPopExitAnim(R.anim.slide_out_right)
                                    .build();
                                View root = getView();
                                if (root != null) {
                                Navigation.findNavController(root)
                                    .navigate(R.id.applicationTimelineFragment, args, options);
                                }
                            return;
                        }
                    }
                }
                openJobDetails(item);
            }

            @Override
            public void onError(Throwable t) {
                if (!isAdded()) {
                    return;
                }
                openJobDetails(item);
            }
        });
    }

    private void openJobDetails(NotificationItem item) {
        if (!isAdded()) {
            return;
        }
        Job seed = new Job();
        seed.setId(item.jobId);
        seed.setTitle(defaultIfBlank(item.jobTitle, getString(R.string.notification_unknown_job)));
        seed.setCompany(defaultIfBlank(item.company, getString(R.string.notification_unknown_company)));
        Bundle args = new Bundle();
        args.putSerializable("job", seed);
        NavOptions options = new NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build();
        View root = getView();
        if (root != null) {
            Navigation.findNavController(root)
                    .navigate(R.id.jobDetailsFragment, args, options);
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void sortNotifications(List<NotificationItem> data) {
        Map<String, NotificationItem> deduped = new LinkedHashMap<>();
        for (NotificationItem item : data) {
            if (item == null) {
                continue;
            }
            String key = item.readKey == null || item.readKey.isBlank()
                    ? (item.type + "|" + item.id + "|" + item.jobId)
                    : item.readKey;
            NotificationItem existing = deduped.get(key);
            if (existing == null || item.sortTimeMs > existing.sortTimeMs) {
                deduped.put(key, item);
            }
        }
        data.clear();
        data.addAll(deduped.values());
        data.sort((left, right) -> Long.compare(right.sortTimeMs, left.sortTimeMs));
    }

    private String formatTimestamp(String preferred, String fallback) {
        String formatted = DateTimeUtils.formatDateTime(preferred, fallback);
        return formatted.isBlank() ? getString(R.string.notification_recent) : formatted;
    }

    private long parseSortTime(String preferred, String fallback) {
        long first = parseSortTime(preferred);
        if (first > 0L) {
            return first;
        }
        return parseSortTime(fallback);
    }

    private long parseSortTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return OffsetDateTime.parse(raw.trim()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(raw.trim())
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        return 0L;
    }

    private void markAllAsRead() {
        if (currentNotifications.isEmpty()) {
            return;
        }
        for (NotificationItem item : currentNotifications) {
            if (item == null || item.readKey == null || item.readKey.isBlank()) {
                continue;
            }
            readStateStore.markRead(item.readKey);
            item.unread = false;
        }
        adapter.notifyDataSetChanged();
    }

    private void clearAllNotifications() {
        if (currentNotifications.isEmpty()) {
            return;
        }
        for (NotificationItem item : currentNotifications) {
            if (item == null || item.readKey == null || item.readKey.isBlank()) {
                continue;
            }
            readStateStore.markRead(item.readKey);
            readStateStore.markHidden(item.readKey);
        }
        View root = getView();
        if (root != null) {
            loadNotifications(root);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "APPLIED";
        String s = status.toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("UNDER_REVIEW".equals(s) || "IN_REVIEW".equals(s)) return "REVIEWED";
        return s;
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }

    @Override
    public void onDestroyView() {
        notificationsRequestVersion++;
        super.onDestroyView();
    }
}
