package com.jobnet.app.ui.recruiter;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.network.dto.UserDto;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.data.session.NotificationReadStateStore;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.ui.notifications.NotificationItem;
import com.jobnet.app.util.RecruiterJobStatusUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecruiterDashboardFragment extends Fragment {

    private JobNetRepository repository;
    private RecruiterJobsAdapter adapter;
    private final List<Job> allPostedJobs = new ArrayList<>();
    private final List<Job> visibleJobs = new ArrayList<>();
    private DashboardFilter activeFilter = DashboardFilter.ACTIVE;

    private ProgressBar progressBar;
    private View emptyView;
    private TextView totalJobsView;
    private TextView activeJobsView;
    private TextView applicantsView;
    private TextView chipActiveView;
    private TextView chipClosedView;
    private TextView chipDraftView;
    private View dashboardContentScroll;
    private View dashboardSkeletonScroll;
    private final List<ObjectAnimator> dashboardSkeletonAnimators = new ArrayList<>();
    private boolean skipNextResumeRefresh = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recruiter_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.recycler_recruiter_jobs);
        progressBar = view.findViewById(R.id.recruiter_jobs_progress);
        emptyView = view.findViewById(R.id.tv_recruiter_empty);
        totalJobsView = view.findViewById(R.id.tv_stat_total_jobs);
        activeJobsView = view.findViewById(R.id.tv_stat_active_jobs);
        applicantsView = view.findViewById(R.id.tv_stat_applicants);
        chipActiveView = view.findViewById(R.id.chip_dashboard_active);
        chipClosedView = view.findViewById(R.id.chip_dashboard_closed);
        chipDraftView = view.findViewById(R.id.chip_dashboard_draft);
        dashboardContentScroll = view.findViewById(R.id.scroll_view);
        dashboardSkeletonScroll = view.findViewById(R.id.scroll_view_dashboard_skeleton);
        TextView greetingView = view.findViewById(R.id.tv_recruiter_greeting);
        TextView nameView = view.findViewById(R.id.tv_recruiter_name);
        MaterialButton createJobButton = view.findViewById(R.id.btn_create_job);
        View notificationButton = view.findViewById(R.id.btn_notification);
        View profileButton = view.findViewById(R.id.iv_recruiter_avatar);
        View allJobsButton = view.findViewById(R.id.tv_recruiter_all_jobs);

        adapter = new RecruiterJobsAdapter(visibleJobs, new RecruiterJobsAdapter.OnJobActionListener() {
            @Override
            public void onJobCardClick(Job job, int position) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("job", job);
                Navigation.findNavController(view)
                        .navigate(R.id.action_recruiterDashboardFragment_to_jobDetailsFragment, bundle);
            }

            @Override
            public void onViewApplicants(Job job, int position) {
                Bundle bundle = new Bundle();
                bundle.putString("jobId", job.getId());
                bundle.putString("jobTitle", job.getTitle());
                Navigation.findNavController(view)
                        .navigate(R.id.action_recruiterDashboardFragment_to_recruiterApplicantsFragment, bundle);
            }

            @Override
            public void onEditJob(Job job, int position) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("job", job);
                Navigation.findNavController(view)
                        .navigate(R.id.action_recruiterDashboardFragment_to_recruiterEditJobFragment, bundle);
            }

            @Override
            public void onDeleteJob(Job job, int position) {
                showDeleteJobDialog(job);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        bindGreeting(greetingView, nameView);
        createJobButton.setEnabled(true);
        createJobButton.setAlpha(1f);
        setupStatusFilters();

        createJobButton.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_recruiterDashboardFragment_to_recruiterPostJobFragment));

        notificationButton.setOnClickListener(v ->
            Navigation.findNavController(view)
                .navigate(R.id.action_recruiterDashboardFragment_to_notificationsFragment));

        profileButton.setOnClickListener(v ->
            Navigation.findNavController(view).navigate(R.id.recruiterProfileFragment));

        allJobsButton.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_recruiterDashboardFragment_to_recruiterAllJobsFragment));

        loadJobs(true, true);
        refreshNotificationDot(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view == null || repository == null) {
            return;
        }
        TextView nameView = view.findViewById(R.id.tv_recruiter_name);
        refreshHeaderName(nameView);
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        loadJobs(true, false);
        refreshNotificationDot(view);
    }

    @Override
    public void onPause() {
        showDashboardSkeleton(false);
        showJobsListSkeleton(false);
        super.onPause();
    }

    private void refreshNotificationDot(View rootView) {
        TextView badge = rootView.findViewById(R.id.view_notification_badge_dot);
        if (badge == null) {
            return;
        }
        badge.setVisibility(View.GONE);
        NotificationReadStateStore readStateStore = new NotificationReadStateStore(requireContext());

        repository.loadRecruiterPostedJobs(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Job> jobs) {
                if (!isAdded()) {
                    return;
                }
                if (jobs == null || jobs.isEmpty()) {
                    badge.setVisibility(View.GONE);
                    return;
                }

                List<Job> eligibleJobs = new ArrayList<>();
                for (Job job : jobs) {
                    if (shouldDisplayOnDashboard(job)) {
                        eligibleJobs.add(job);
                    }
                }
                if (eligibleJobs.isEmpty()) {
                    badge.setVisibility(View.GONE);
                    return;
                }

                final int[] unreadCount = {0};
                final int[] pending = {eligibleJobs.size()};
                for (Job job : eligibleJobs) {
                    repository.loadJobApplicants(job.getId(), new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(List<ApplicationDto> data) {
                            if (!isAdded()) {
                                return;
                            }
                            if (data != null) {
                                for (ApplicationDto app : data) {
                                    if (app == null || app.status == null
                                            || !"APPLIED".equalsIgnoreCase(app.status.trim())) {
                                        continue;
                                    }
                                    String readKey = NotificationReadStateStore.buildKey(
                                            NotificationItem.TYPE_RECRUITER_APPLICANT,
                                            app.id,
                                            app.jobId,
                                            app.status,
                                            app.updatedAt,
                                            app.appliedAt
                                    );
                                    if (!readStateStore.isRead(readKey)) {
                                        unreadCount[0]++;
                                    }
                                }
                            }
                            finalizeRecruiterBadge(badge, unreadCount[0], pending);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (!isAdded()) {
                                return;
                            }
                            finalizeRecruiterBadge(badge, unreadCount[0], pending);
                        }
                    });
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                badge.setVisibility(View.GONE);
            }
        });
    }

    private void finalizeRecruiterBadge(TextView badge, int unread, int[] pending) {
        pending[0]--;
        if (pending[0] > 0) {
            return;
        }
        if (unread > 0) {
            badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void loadJobs(boolean showLoader, boolean showErrorToast) {
        if (progressBar == null || emptyView == null || totalJobsView == null || activeJobsView == null || applicantsView == null) {
            return;
        }
        progressBar.setVisibility(View.GONE);
        showDashboardSkeleton(showLoader);
        showJobsListSkeleton(!showLoader);
        emptyView.setVisibility(View.GONE);
        repository.loadRecruiterPostedJobs(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Job> data) {
                showDashboardSkeleton(false);
                showJobsListSkeleton(false);
                if (!isAdded()) {
                    return;
                }
                allPostedJobs.clear();
                if (data != null) {
                    allPostedJobs.addAll(data);
                }
                bindStats(totalJobsView, activeJobsView, applicantsView, allPostedJobs);
                updateStatusChipLabels();
                applyActiveFilter();
            }

            @Override
            public void onError(Throwable throwable) {
                showDashboardSkeleton(false);
                showJobsListSkeleton(false);
                if (!isAdded()) {
                    return;
                }
                bindStats(totalJobsView, activeJobsView, applicantsView, allPostedJobs);
                updateStatusChipLabels();
                applyActiveFilter();
                if (showErrorToast && allPostedJobs.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.recruiter_jobs_load_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupStatusFilters() {
        if (chipActiveView == null || chipClosedView == null || chipDraftView == null) {
            return;
        }
        chipActiveView.setOnClickListener(v -> {
            activeFilter = DashboardFilter.ACTIVE;
            applyActiveFilter();
            refreshChipState();
        });
        chipClosedView.setOnClickListener(v -> {
            activeFilter = DashboardFilter.CLOSED;
            applyActiveFilter();
            refreshChipState();
        });
        chipDraftView.setOnClickListener(v -> {
            activeFilter = DashboardFilter.DRAFT;
            applyActiveFilter();
            refreshChipState();
        });
        refreshChipState();
        updateStatusChipLabels();
    }

    private void applyActiveFilter() {
        visibleJobs.clear();
        for (Job job : allPostedJobs) {
            if (job == null) {
                continue;
            }
            DashboardFilter status = resolveStatus(job);
            if (activeFilter == status) {
                visibleJobs.add(job);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (emptyView != null) {
            emptyView.setVisibility(visibleJobs.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateStatusChipLabels() {
        if (chipActiveView == null || chipClosedView == null || chipDraftView == null) {
            return;
        }
        int activeCount = 0;
        int closedCount = 0;
        int draftCount = 0;
        for (Job job : allPostedJobs) {
            DashboardFilter status = resolveStatus(job);
            if (status == DashboardFilter.ACTIVE) {
                activeCount++;
            } else if (status == DashboardFilter.CLOSED) {
                closedCount++;
            } else {
                draftCount++;
            }
        }
        chipActiveView.setText(getString(R.string.recruiter_filter_active_count, activeCount));
        chipClosedView.setText(getString(R.string.recruiter_filter_closed_count, closedCount));
        chipDraftView.setText(getString(R.string.recruiter_filter_draft_count, draftCount));
    }

    private void refreshChipState() {
        applyChipStyle(chipActiveView, activeFilter == DashboardFilter.ACTIVE);
        applyChipStyle(chipClosedView, activeFilter == DashboardFilter.CLOSED);
        applyChipStyle(chipDraftView, activeFilter == DashboardFilter.DRAFT);
    }

    private void applyChipStyle(TextView chip, boolean selected) {
        if (chip == null) {
            return;
        }
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(requireContext().getColor(selected ? R.color.text_chip_selected : R.color.text_chip));
    }

    private void bindGreeting(TextView greetingView, TextView nameView) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning,";
        } else if (hour < 17) {
            greeting = "Good Afternoon,";
        } else {
            greeting = "Good Evening,";
        }
        greetingView.setText(greeting);

        SessionManager sessionManager = new SessionManager(requireContext());
        String displayName = sessionManager.getUserName();
        if (displayName == null || displayName.isBlank()) {
            displayName = "Recruiter";
        }
        nameView.setText(displayName);
        refreshHeaderName(nameView);
    }

    private void refreshHeaderName(TextView nameView) {
        if (nameView == null || repository == null) {
            return;
        }
        repository.fetchProfile(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(UserDto data) {
                if (!isAdded() || data == null) {
                    return;
                }
                String resolvedName = data.name != null && !data.name.isBlank() ? data.name : "";
                if (resolvedName.isBlank()) {
                    return;
                }
                nameView.setText(resolvedName.trim());
            }

            @Override
            public void onError(Throwable throwable) {
                // Keep existing fallback text when profile fetch is unavailable.
            }
        });
    }

    private void showDeleteJobDialog(Job job) {
        if (job == null || job.getId() == null || job.getId().isBlank() || !isAdded()) {
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        ((TextView) dialog.findViewById(R.id.tv_dialog_title)).setText(R.string.delete_job_confirm_title);
        ((TextView) dialog.findViewById(R.id.tv_dialog_message)).setText(R.string.delete_job_confirm_message);

        TextView btnConfirm = dialog.findViewById(R.id.btn_dialog_confirm);
        btnConfirm.setText(R.string.delete_job_action);
        btnConfirm.setBackgroundResource(R.drawable.bg_dialog_button_danger);
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            deleteJob(job.getId());
        });
        dialog.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void deleteJob(String jobId) {
        repository.deleteRecruiterJob(jobId, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(Boolean data) {
                if (!isAdded()) {
                    return;
                }
                loadJobs(false, false);
                View view = getView();
                if (view != null) {
                    refreshNotificationDot(view);
                }
                Toast.makeText(requireContext(), R.string.recruiter_job_deleted, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), R.string.recruiter_job_delete_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindStats(TextView totalJobsView, TextView activeJobsView, TextView applicantsView, List<Job> jobs) {
        int total = jobs == null ? 0 : jobs.size();
        int active = 0;
        int applicants = 0;
        if (jobs != null) {
            for (Job job : jobs) {
                applicants += Math.max(0, job.getApplicantsCount());
                if (isActiveJob(job)) {
                    active++;
                }
            }
        }
        totalJobsView.setText(String.valueOf(total));
        activeJobsView.setText(String.valueOf(active));
        applicantsView.setText(String.valueOf(applicants));
    }

    private boolean isActiveJob(Job job) {
        return RecruiterJobStatusUtils.inferActive(job);
    }

    private boolean shouldDisplayOnDashboard(Job job) {
        if (job == null) {
            return false;
        }
        return RecruiterJobStatusUtils.resolveBucket(job) != RecruiterJobStatusUtils.Bucket.DRAFT;
    }

    private DashboardFilter resolveStatus(Job job) {
        RecruiterJobStatusUtils.Bucket bucket = RecruiterJobStatusUtils.resolveBucket(job);
        if (bucket == RecruiterJobStatusUtils.Bucket.DRAFT) {
            return DashboardFilter.DRAFT;
        }
        if (bucket == RecruiterJobStatusUtils.Bucket.CLOSED) {
            return DashboardFilter.CLOSED;
        }
        return DashboardFilter.ACTIVE;
    }

    private void showDashboardSkeleton(boolean show) {
        if (adapter == null || emptyView == null || dashboardContentScroll == null || dashboardSkeletonScroll == null) {
            return;
        }
        dashboardContentScroll.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        dashboardSkeletonScroll.setVisibility(show ? View.VISIBLE : View.GONE);
        emptyView.setVisibility(View.GONE);
        if (show) {
            startDashboardSkeletonAnimations();
        } else {
            stopDashboardSkeletonAnimations();
        }
    }

    private void showJobsListSkeleton(boolean show) {
        if (adapter == null) {
            return;
        }
        adapter.showSkeleton(show);
    }

    private void startDashboardSkeletonAnimations() {
        stopDashboardSkeletonAnimations();
        if (dashboardSkeletonScroll == null) {
            return;
        }
        int[] skeletonIds = new int[]{
                R.id.skel_dash_greeting,
                R.id.skel_dash_name,
                R.id.skel_dash_role,
                R.id.skel_dash_bell,
                R.id.skel_dash_avatar,
                R.id.skel_dash_stat_1,
                R.id.skel_dash_stat_2,
                R.id.skel_dash_stat_3,
                R.id.skel_dash_cta,
                R.id.skel_dash_jobs_title,
                R.id.skel_dash_jobs_all,
                R.id.skel_dash_chip_1,
                R.id.skel_dash_chip_2,
                R.id.skel_dash_chip_3,
                R.id.skel_dash_job_card_1,
                R.id.skel_dash_job_card_2
        };
        for (int i = 0; i < skeletonIds.length; i++) {
            View block = dashboardSkeletonScroll.findViewById(skeletonIds[i]);
            if (block == null) {
                continue;
            }
            ObjectAnimator animator = ObjectAnimator.ofFloat(block, "alpha", 0.4f, 1f);
            animator.setDuration(900);
            animator.setStartDelay((i % 4) * 80L);
            animator.setRepeatMode(ObjectAnimator.REVERSE);
            animator.setRepeatCount(ObjectAnimator.INFINITE);
            animator.start();
            dashboardSkeletonAnimators.add(animator);
        }
    }

    private void stopDashboardSkeletonAnimations() {
        for (ObjectAnimator animator : dashboardSkeletonAnimators) {
            if (animator != null) {
                animator.cancel();
            }
        }
        dashboardSkeletonAnimators.clear();
    }

    @Override
    public void onDestroyView() {
        showDashboardSkeleton(false);
        dashboardContentScroll = null;
        dashboardSkeletonScroll = null;
        super.onDestroyView();
    }

    private enum DashboardFilter {
        ACTIVE,
        CLOSED,
        DRAFT
    }
}
