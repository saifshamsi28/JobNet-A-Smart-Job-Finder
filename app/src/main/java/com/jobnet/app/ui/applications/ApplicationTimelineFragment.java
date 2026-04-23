package com.jobnet.app.ui.applications;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ApplicationTimelineFragment extends Fragment {

    private enum StepState {
        COMPLETED,
        CURRENT,
        PENDING
    }

    private static class TimelineStep {
        final String status;
        final StepState state;
        final String meta;

        TimelineStep(String status, StepState state, String meta) {
            this.status = status;
            this.state = state;
            this.meta = meta;
        }
    }

    private JobNetRepository repository;
    private ApplicationDto application;
    private boolean withdrawInFlight;
    private boolean skipNextResumeRefresh = true;
    private final List<ObjectAnimator> timelineGlowAnimators = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_application_timeline, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());

        if (getArguments() != null) {
            Object raw = getArguments().getSerializable("application");
            if (raw instanceof ApplicationDto) {
                application = (ApplicationDto) raw;
            }
        }

        view.findViewById(R.id.btn_back_application_timeline).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        view.findViewById(R.id.btn_timeline_open_job).setOnClickListener(v -> openJobDetails(view));
        view.findViewById(R.id.btn_timeline_withdraw).setOnClickListener(v -> showWithdrawConfirmation(view));

        if (application == null) {
            String applicationId = getArguments() == null ? "" : getArguments().getString("applicationId", "");
            if (!applicationId.isBlank()) {
                loadApplication(applicationId, view);
                return;
            }
        }
        bind(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        View root = getView();
        if (root == null) {
            return;
        }
        if (application == null) {
            String applicationId = getArguments() == null ? "" : getArguments().getString("applicationId", "");
            if (!applicationId.isBlank()) {
                loadApplication(applicationId, root);
                return;
            }
        }
        bind(root);
    }

    private void loadApplication(String applicationId, View root) {
        repository.loadMyApplications(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<ApplicationDto> data) {
                if (!isAdded()) {
                    return;
                }
                if (data != null) {
                    for (ApplicationDto dto : data) {
                        if (dto != null && applicationId.equals(dto.id)) {
                            application = dto;
                            break;
                        }
                    }
                }
                bind(root);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                bind(root);
            }
        });
    }

    private void bind(View root) {
        TextView title = root.findViewById(R.id.tv_timeline_job_title);
        TextView company = root.findViewById(R.id.tv_timeline_company);
        TextView statusChip = root.findViewById(R.id.tv_timeline_current_status);
        TextView subtitle = root.findViewById(R.id.tv_timeline_subtitle);
        TextView withdrawButton = root.findViewById(R.id.btn_timeline_withdraw);

        if (application == null) {
            title.setText(getString(R.string.notification_unknown_job));
            company.setText(getString(R.string.notification_unknown_company));
            subtitle.setText(getString(R.string.timeline_data_unavailable));
            statusChip.setText(getString(R.string.status_applied));
            applyStatusStyle(statusChip, "APPLIED");
            withdrawButton.setVisibility(View.GONE);
            renderSteps(root, new ArrayList<>());
            return;
        }

        title.setText(defaultIfBlank(application.jobTitle, getString(R.string.notification_unknown_job)));
        company.setText(defaultIfBlank(application.company, getString(R.string.notification_unknown_company)));

        String normalizedStatus = normalizeStatus(application.status);
        statusChip.setText(displayStatus(normalizedStatus));
        applyStatusStyle(statusChip, normalizedStatus);

        String updated = DateTimeUtils.formatDateTime(application.updatedAt, application.appliedAt);
        subtitle.setText(updated.isBlank()
                ? getString(R.string.timeline_subtitle_default)
                : getString(R.string.timeline_subtitle_updated, updated));

        boolean canWithdraw = canWithdraw(normalizedStatus) && !withdrawInFlight;
        withdrawButton.setVisibility(canWithdraw ? View.VISIBLE : View.GONE);
        withdrawButton.setEnabled(canWithdraw);
        withdrawButton.setAlpha(canWithdraw ? 1f : 0.65f);
        withdrawButton.setText(withdrawInFlight
                ? getString(R.string.timeline_withdrawing)
                : getString(R.string.timeline_withdraw_action));

        renderSteps(root, buildSteps(normalizedStatus, application.appliedAt, application.updatedAt));
    }

    private void renderSteps(View root, List<TimelineStep> steps) {
        LinearLayout container = root.findViewById(R.id.container_timeline_steps);
        clearTimelineGlowAnimators();
        container.removeAllViews();

        if (steps.isEmpty()) {
            TimelineStep fallback = new TimelineStep("APPLIED", StepState.PENDING, getString(R.string.timeline_pending));
            steps = List.of(fallback);
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < steps.size(); i++) {
            TimelineStep step = steps.get(i);
            View item = inflater.inflate(R.layout.item_application_timeline_step, container, false);
            ImageView icon = item.findViewById(R.id.iv_timeline_step_icon);
            TextView title = item.findViewById(R.id.tv_timeline_step_title);
            TextView meta = item.findViewById(R.id.tv_timeline_step_meta);
            View iconShell = item.findViewById(R.id.layout_timeline_step_icon);
            View iconRing = item.findViewById(R.id.view_timeline_step_ring);
            View connectorLine = item.findViewById(R.id.view_timeline_step_line);

            title.setText(displayStatus(step.status));
            meta.setText(step.meta);
            applyStepStyle(iconShell, iconRing, icon, title, meta, connectorLine, step.state, i == steps.size() - 1);

            container.addView(item);
        }
    }

    private List<TimelineStep> buildSteps(String status, String appliedAt, String updatedAt) {
        List<String> ordered = Arrays.asList("APPLIED", "REVIEWED", "SHORTLISTED", "INTERVIEW", "OFFERED");
        int idx = ordered.indexOf(status);

        List<TimelineStep> result = new ArrayList<>();
        String appliedTime = DateTimeUtils.formatDateTime(appliedAt, appliedAt);
        String updatedTime = DateTimeUtils.formatDateTime(updatedAt, appliedAt);

        if (idx >= 0) {
            for (int i = 0; i < ordered.size(); i++) {
                String stepStatus = ordered.get(i);
                if (i < idx) {
                    String meta = "APPLIED".equals(stepStatus)
                            ? valueOrDefault(appliedTime, getString(R.string.timeline_completed))
                            : getString(R.string.timeline_completed);
                    result.add(new TimelineStep(stepStatus, StepState.COMPLETED, meta));
                } else if (i == idx) {
                    String meta = "APPLIED".equals(stepStatus)
                            ? valueOrDefault(appliedTime, getString(R.string.timeline_in_progress))
                            : valueOrDefault(updatedTime, getString(R.string.timeline_in_progress));
                    result.add(new TimelineStep(stepStatus, StepState.CURRENT, meta));
                } else {
                    result.add(new TimelineStep(stepStatus, StepState.PENDING, getString(R.string.timeline_pending)));
                }
            }
            return result;
        }

        if ("REJECTED".equals(status) || "WITHDRAWN".equals(status)) {
            result.add(new TimelineStep("APPLIED", StepState.COMPLETED, valueOrDefault(appliedTime, getString(R.string.timeline_completed))));
            result.add(new TimelineStep(status, StepState.CURRENT, valueOrDefault(updatedTime, getString(R.string.timeline_in_progress))));
            return result;
        }

        result.add(new TimelineStep("APPLIED", StepState.COMPLETED, valueOrDefault(appliedTime, getString(R.string.timeline_completed))));
        result.add(new TimelineStep(status, StepState.CURRENT, valueOrDefault(updatedTime, getString(R.string.timeline_in_progress))));
        return result;
    }

    private void applyStepStyle(View iconShell,
                                View iconRing,
                                ImageView icon,
                                TextView title,
                                TextView meta,
                                View connectorLine,
                                StepState state,
                                boolean isLastStep) {
        if (connectorLine != null) {
            connectorLine.setVisibility(isLastStep ? View.GONE : View.VISIBLE);
        }
        if (iconRing != null) {
            iconRing.setVisibility(View.GONE);
            iconRing.setAlpha(0f);
            iconRing.setScaleX(1f);
            iconRing.setScaleY(1f);
        }
        if (state == StepState.COMPLETED) {
            if (iconShell != null) {
                iconShell.setBackgroundResource(R.drawable.bg_timeline_icon_completed);
                iconShell.setAlpha(1f);
            }
            icon.setImageResource(R.drawable.ic_check);
            icon.setColorFilter(requireContext().getColor(R.color.white));
            title.setTextColor(requireContext().getColor(R.color.text_primary));
            meta.setTextColor(requireContext().getColor(R.color.text_secondary));
            if (connectorLine != null && !isLastStep) {
                connectorLine.setBackgroundColor(requireContext().getColor(R.color.success));
                connectorLine.setAlpha(0.9f);
            }
            return;
        }
        if (state == StepState.CURRENT) {
            if (iconShell != null) {
                iconShell.setBackgroundResource(R.drawable.bg_timeline_icon_current);
            }
            if (iconRing != null) {
                iconRing.setVisibility(View.VISIBLE);
                startPulseRingAnimation(iconRing);
            }
            icon.setImageResource(R.drawable.ic_clock);
            icon.setColorFilter(requireContext().getColor(R.color.white));
            title.setTextColor(requireContext().getColor(R.color.primary));
            meta.setTextColor(requireContext().getColor(R.color.text_secondary));
            if (iconShell != null) {
                startGlowAnimation(iconShell, 0.58f, 1f, 900L);
            }
            if (connectorLine != null && !isLastStep) {
                connectorLine.setBackgroundColor(requireContext().getColor(R.color.primary));
                connectorLine.setAlpha(0.9f);
                startGlowAnimation(connectorLine, 0.5f, 1f, 850L);
            }
            return;
        }
        if (iconShell != null) {
            iconShell.setBackgroundResource(R.drawable.bg_timeline_icon_pending);
            iconShell.setAlpha(1f);
        }
        icon.setImageResource(R.drawable.ic_clock);
        icon.setColorFilter(requireContext().getColor(R.color.text_secondary));
        title.setTextColor(requireContext().getColor(R.color.text_secondary));
        meta.setTextColor(requireContext().getColor(R.color.text_tertiary));
        if (connectorLine != null && !isLastStep) {
            connectorLine.setBackgroundColor(requireContext().getColor(R.color.text_tertiary));
            connectorLine.setAlpha(0.56f);
        }
    }

    private void startPulseRingAnimation(View ring) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 1f, 1.45f);
        scaleX.setDuration(1100L);
        scaleX.setRepeatMode(ObjectAnimator.RESTART);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 1f, 1.45f);
        scaleY.setDuration(1100L);
        scaleY.setRepeatMode(ObjectAnimator.RESTART);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(ring, "alpha", 0.62f, 0f);
        alpha.setDuration(1100L);
        alpha.setRepeatMode(ObjectAnimator.RESTART);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        scaleX.start();
        scaleY.start();
        alpha.start();

        timelineGlowAnimators.add(scaleX);
        timelineGlowAnimators.add(scaleY);
        timelineGlowAnimators.add(alpha);
    }

    private void applyStatusStyle(TextView statusChip, String status) {
        switch (status) {
            case "REJECTED":
                statusChip.setBackgroundResource(R.drawable.bg_tag_error);
                statusChip.setTextColor(requireContext().getColor(R.color.status_rejected_text));
                break;
            case "SHORTLISTED":
                statusChip.setBackgroundResource(R.drawable.bg_tag_warning);
                statusChip.setTextColor(requireContext().getColor(R.color.status_shortlisted_text));
                break;
            case "REVIEWED":
            case "INTERVIEW":
                statusChip.setBackgroundResource(R.drawable.bg_tag_reviewed);
                statusChip.setTextColor(requireContext().getColor(R.color.status_reviewed_text));
                break;
            case "OFFERED":
                statusChip.setBackgroundResource(R.drawable.bg_tag_green);
                statusChip.setTextColor(requireContext().getColor(R.color.status_offered_text));
                break;
            case "WITHDRAWN":
                statusChip.setBackgroundResource(R.drawable.bg_tag_withdrawn);
                statusChip.setTextColor(requireContext().getColor(R.color.status_withdrawn_text));
                break;
            default:
                statusChip.setBackgroundResource(R.drawable.bg_tag_primary);
                statusChip.setTextColor(requireContext().getColor(R.color.status_applied_text));
                break;
        }
    }

    private void showWithdrawConfirmation(View root) {
        if (application == null || application.id == null || application.id.isBlank() || withdrawInFlight) {
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

        ((TextView) dialog.findViewById(R.id.tv_dialog_title)).setText(R.string.timeline_withdraw_confirm_title);
        ((TextView) dialog.findViewById(R.id.tv_dialog_message)).setText(R.string.timeline_withdraw_confirm_message);

        TextView btnConfirm = dialog.findViewById(R.id.btn_dialog_confirm);
        btnConfirm.setText(R.string.timeline_withdraw_confirm_action);
        btnConfirm.setBackgroundResource(R.drawable.bg_dialog_button_danger);
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            withdrawApplication(root);
        });
        dialog.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void withdrawApplication(View root) {
        if (application == null || application.id == null || application.id.isBlank() || withdrawInFlight) {
            return;
        }
        withdrawInFlight = true;
        bind(root);

        repository.withdrawApplication(application.id, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(ApplicationDto data) {
                if (!isAdded()) {
                    return;
                }
                withdrawInFlight = false;
                if (data != null) {
                    application = data;
                } else {
                    application.status = "WITHDRAWN";
                }
                bind(root);
                Toast.makeText(requireContext(), R.string.timeline_withdraw_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                withdrawInFlight = false;
                bind(root);
                Toast.makeText(requireContext(), R.string.timeline_withdraw_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openJobDetails(View root) {
        if (application == null || application.jobId == null || application.jobId.isBlank()) {
            Toast.makeText(requireContext(), R.string.timeline_job_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        Job seed = new Job();
        seed.setId(application.jobId);
        seed.setTitle(defaultIfBlank(application.jobTitle, getString(R.string.notification_unknown_job)));
        seed.setCompany(defaultIfBlank(application.company, getString(R.string.notification_unknown_company)));

        Bundle args = new Bundle();
        args.putSerializable("job", seed);
        Navigation.findNavController(root).navigate(R.id.jobDetailsFragment, args);
    }

    private boolean canWithdraw(String status) {
        return "APPLIED".equals(status)
                || "REVIEWED".equals(status)
                || "SHORTLISTED".equals(status)
                || "INTERVIEW".equals(status)
                || "OFFERED".equals(status);
    }

    private String normalizeStatus(String rawStatus) {
        String normalized = defaultIfBlank(rawStatus, "APPLIED").trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("UNDER_REVIEW".equals(normalized) || "IN_REVIEW".equals(normalized)) {
            return "REVIEWED";
        }
        return normalized;
    }

    private String displayStatus(String status) {
        switch (status) {
            case "REVIEWED":
                return getString(R.string.status_reviewed);
            case "SHORTLISTED":
                return getString(R.string.status_shortlisted);
            case "REJECTED":
                return getString(R.string.status_rejected);
            case "OFFERED":
                return getString(R.string.status_offered);
            case "INTERVIEW":
                return getString(R.string.status_interview);
            case "WITHDRAWN":
                return getString(R.string.status_withdrawn);
            default:
                return getString(R.string.status_applied);
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private void startGlowAnimation(View target, float minAlpha, float maxAlpha, long duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, "alpha", minAlpha, maxAlpha);
        animator.setDuration(duration);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.start();
        timelineGlowAnimators.add(animator);
    }

    private void clearTimelineGlowAnimators() {
        for (ObjectAnimator animator : timelineGlowAnimators) {
            if (animator != null) {
                animator.cancel();
            }
        }
        timelineGlowAnimators.clear();
    }

    @Override
    public void onDestroyView() {
        clearTimelineGlowAnimators();
        super.onDestroyView();
    }
}
