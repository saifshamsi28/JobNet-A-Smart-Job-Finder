package com.jobnet.app.ui.jobdetails;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.flexbox.FlexboxLayout;
import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.util.SkeletonShimmerHelper;
import com.jobnet.app.util.SalaryUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JobDetailsFragment extends Fragment {

    private Job job;
    private JobNetRepository repository;
    private boolean recruiterViewOnly;
    private boolean applyInFlight;
    private boolean alreadyApplied;
    private ApplicationDto currentApplication;
    private Dialog skeletonDialog;
    private final List<ObjectAnimator> skeletonAnimators = new ArrayList<>();
    private boolean skipNextResumeRefresh = true;

    private boolean isJobClosed() {
        String status = safe(job == null ? null : job.getStatus()).toUpperCase(Locale.ROOT);
        return "CLOSED".equals(status) || "DRAFT".equals(status);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_job_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());
        SessionManager session = new SessionManager(requireContext());
        String role = session.getUserRole();
        recruiterViewOnly = role != null && role.toUpperCase(Locale.ROOT).contains("RECRUITER");

        if (getArguments() != null) {
            job = (Job) getArguments().getSerializable("job");
        }

        applyInsets(view);

        if (job != null) {
            populateUI(view);
            loadFullDetails(view);
            if (!recruiterViewOnly) {
                loadApplicationStatus(view);
            }
        }

        view.findViewById(R.id.iv_back).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        if (recruiterViewOnly) {
            view.findViewById(R.id.bottom_action_bar).setVisibility(View.GONE);
            view.findViewById(R.id.tv_application_status).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.btn_apply_now).setOnClickListener(v -> applyToJob(view));
            view.findViewById(R.id.btn_view_timeline).setOnClickListener(v -> openTimeline(view));
            view.findViewById(R.id.btn_withdraw_application_inline).setOnClickListener(v -> showWithdrawConfirmation(view));
            updateApplyButton(view, false, false);

            ImageView ivBookmark = view.findViewById(R.id.iv_detail_bookmark);
            applyBookmarkAlpha(ivBookmark);

            View.OnClickListener saveClick = v -> toggleSave(ivBookmark);
            view.findViewById(R.id.btn_save_detail).setOnClickListener(saveClick);
            ivBookmark.setOnClickListener(saveClick);
        }

        view.findViewById(R.id.iv_share).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Share link copied!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        View root = getView();
        if (root == null || job == null) {
            return;
        }
        loadFullDetails(root);
        if (!recruiterViewOnly) {
            loadApplicationStatus(root);
        }
    }

    private void populateUI(View view) {
        ((TextView) view.findViewById(R.id.tv_detail_title)).setText(defaultIfBlank(job.getTitle(), "Untitled Role"));
        ((TextView) view.findViewById(R.id.tv_detail_company)).setText(defaultIfBlank(job.getCompany(), "Unknown Company"));
        ((TextView) view.findViewById(R.id.tv_detail_salary)).setText(SalaryUtils.normalizeDisplay(defaultIfBlank(job.getSalary(), getString(R.string.not_available))));
        ((TextView) view.findViewById(R.id.tv_detail_location)).setText(defaultIfBlank(job.getLocation(), getString(R.string.not_available)));
        ((TextView) view.findViewById(R.id.tv_detail_type)).setText(defaultIfBlank(job.getType(), getString(R.string.not_available)));
        ((TextView) view.findViewById(R.id.tv_company_name_detail)).setText(defaultIfBlank(job.getCompany(), "Unknown Company"));
        TextView descriptionView = view.findViewById(R.id.tv_description);
        String description = safe(job.getDescription());
        descriptionView.setText(description.isEmpty() ? getString(R.string.not_available) : description);

        LinearLayout llReq = view.findViewById(R.id.ll_requirements);
        llReq.removeAllViews();
        List<String> requirements = buildRequirements(job);
        for (String req : requirements) {
            View reqView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_requirement, llReq, false);
            ((TextView) reqView.findViewById(R.id.tv_requirement)).setText(req);
            llReq.addView(reqView);
        }

        FlexboxLayout flexSkills = view.findViewById(R.id.flex_skills);
        flexSkills.removeAllViews();
        List<String> skills = buildSkills(job);
        for (String skill : skills) {
            TextView tag = (TextView) LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_skill_tag, flexSkills, false);
            tag.setText(skill);
            flexSkills.addView(tag);
        }
    }

    private void loadFullDetails(View view) {
        showDetailsSkeleton(true);
        repository.loadJobDetails(job, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(Job data) {
                if (!isAdded() || data == null) {
                    showDetailsSkeleton(false);
                    return;
                }
                job = data;
                populateUI(view);
                showDetailsSkeleton(false);
                animateDetailSections(view);
            }

            @Override
            public void onError(Throwable throwable) {
                // Keep showing seed data.
                showDetailsSkeleton(false);
            }
        });
    }

    private void toggleSave(ImageView ivBookmark) {
        if (job == null) {
            return;
        }

        boolean wantToSave = !job.isSaved();
        repository.toggleSave(job, wantToSave, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(Boolean data) {
                if (!isAdded()) {
                    return;
                }
                job.setSaved(wantToSave);
                applyBookmarkAlpha(ivBookmark);
                Toast.makeText(requireContext(), wantToSave ? "Saved!" : "Removed from saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                applyBookmarkAlpha(ivBookmark);
            }
        });
    }

    private void applyToJob(View view) {
        if (job == null || applyInFlight || alreadyApplied || isJobClosed()) {
            return;
        }
        updateApplyButton(view, true, alreadyApplied);
        repository.applyToJob(job, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(ApplicationDto data) {
                if (!isAdded()) {
                    return;
                }
                updateApplyButton(view, false, true);
                updateApplicationStatusUI(view, data);
                Toast.makeText(requireContext(), R.string.apply_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                updateApplyButton(view, false, alreadyApplied);
                Toast.makeText(requireContext(), R.string.apply_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadApplicationStatus(View view) {
        if (job == null || safe(job.getId()).isEmpty()) {
            return;
        }
        repository.loadMyApplicationForJob(job.getId(), new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(ApplicationDto data) {
                if (!isAdded()) {
                    return;
                }
                updateApplicationStatusUI(view, data);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                updateApplicationStatusUI(view, null);
            }
        });
    }

    private void updateApplicationStatusUI(View view, ApplicationDto application) {
        TextView statusText = view.findViewById(R.id.tv_application_status);
        TextView applyLabel = view.findViewById(R.id.tv_apply_label);
        TextView timelineButton = view.findViewById(R.id.btn_view_timeline);
        TextView withdrawButton = view.findViewById(R.id.btn_withdraw_application_inline);

        currentApplication = application;

        if (application == null || safe(application.status).isEmpty()) {
            alreadyApplied = false;
            statusText.setText(isJobClosed() ? R.string.application_status_closed : R.string.application_status_none);
            timelineButton.setVisibility(View.GONE);
            withdrawButton.setVisibility(View.GONE);
            updateApplyButton(view, false, false);
            return;
        }

        alreadyApplied = true;
        String normalized = normalizeStatus(application.status);
        statusText.setText(getString(R.string.application_status_format, normalized.replace('_', ' ')));
        timelineButton.setVisibility(View.VISIBLE);
        withdrawButton.setVisibility(canWithdraw(normalized) ? View.VISIBLE : View.GONE);
        applyLabel.setText(R.string.applied_label);
        updateApplyButton(view, false, true);
    }

    private void openTimeline(View root) {
        if (currentApplication == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putSerializable("application", currentApplication);
        Navigation.findNavController(root).navigate(R.id.applicationTimelineFragment, args);
    }

    private void withdrawApplicationInline(View root) {
        if (currentApplication == null || safe(currentApplication.id).isEmpty() || applyInFlight) {
            return;
        }
        applyInFlight = true;
        TextView withdrawButton = root.findViewById(R.id.btn_withdraw_application_inline);
        withdrawButton.setEnabled(false);
        withdrawButton.setAlpha(0.65f);
        withdrawButton.setText(R.string.timeline_withdrawing);

        repository.withdrawApplication(currentApplication.id, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(ApplicationDto data) {
                if (!isAdded()) {
                    return;
                }
                applyInFlight = false;
                if (data != null) {
                    currentApplication = data;
                } else {
                    currentApplication.status = "WITHDRAWN";
                }
                updateApplicationStatusUI(root, currentApplication);
                Toast.makeText(requireContext(), R.string.timeline_withdraw_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                applyInFlight = false;
                updateApplicationStatusUI(root, currentApplication);
                Toast.makeText(requireContext(), R.string.timeline_withdraw_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showWithdrawConfirmation(View root) {
        if (currentApplication == null || safe(currentApplication.id).isEmpty() || applyInFlight) {
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
            withdrawApplicationInline(root);
        });
        dialog.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private boolean canWithdraw(String normalizedStatus) {
        return "APPLIED".equals(normalizedStatus)
                || "REVIEWED".equals(normalizedStatus)
                || "SHORTLISTED".equals(normalizedStatus)
                || "INTERVIEW".equals(normalizedStatus)
                || "OFFERED".equals(normalizedStatus);
    }

    private String normalizeStatus(String rawStatus) {
        String normalized = safe(rawStatus).toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("UNDER_REVIEW".equals(normalized) || "IN_REVIEW".equals(normalized)) {
            return "REVIEWED";
        }
        return normalized.isEmpty() ? "APPLIED" : normalized;
    }

    private void updateApplyButton(View view, boolean loading, boolean applied) {
        applyInFlight = loading;
        alreadyApplied = applied;

        View applyButton = view.findViewById(R.id.btn_apply_now);
        TextView applyLabel = view.findViewById(R.id.tv_apply_label);
        ImageView applyIcon = view.findViewById(R.id.iv_apply_icon);
        ProgressBar applyProgress = view.findViewById(R.id.progress_apply);
        boolean closed = isJobClosed();

        boolean enabled = !loading && !applied && !closed;
        applyButton.setEnabled(enabled);
        applyButton.setClickable(enabled);
        applyButton.setAlpha(enabled ? 1f : 0.72f);

        applyProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        applyIcon.setVisibility(loading ? View.GONE : View.VISIBLE);

        if (!loading) {
            if (closed) {
                applyIcon.setImageResource(R.drawable.ic_close);
            } else if (applied) {
                applyIcon.setImageResource(R.drawable.ic_check);
            } else {
                applyIcon.setImageResource(R.drawable.ic_arrow_right);
            }
        }

        if (loading) {
            applyLabel.setText(R.string.applying_label);
        } else if (closed) {
            applyLabel.setText(R.string.hiring_closed);
        } else if (applied) {
            applyLabel.setText(R.string.applied_label);
        } else {
            applyLabel.setText(R.string.apply_now_label);
        }
    }

    private void applyBookmarkAlpha(ImageView ivBookmark) {
        boolean saved = job != null && job.isSaved();
        ivBookmark.setImageResource(saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
//        btnSaveDetailBookMark.setImageResource(saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);

        ImageViewCompat.setImageTintList(ivBookmark, null);
        if (!saved) {
            ivBookmark.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            ivBookmark.clearColorFilter();
        }
        ivBookmark.setAlpha(1.0f);
    }

    private List<String> buildRequirements(Job source) {
        List<String> requirements = new ArrayList<>();
        String body = safe(source == null ? null : source.getDescription());
        if (!body.isEmpty()) {
            String normalized = body.replace("\r", "\n");
            String[] parts = normalized.split("[\\n\\.] ");
            for (String part : parts) {
                String line = part.trim();
                if (line.length() < 18) {
                    continue;
                }
                if (!line.endsWith(".")) {
                    line = line + ".";
                }
                requirements.add(line);
                if (requirements.size() == 5) {
                    break;
                }
            }
        }
        if (requirements.isEmpty()) {
            requirements.add(getString(R.string.job_requirements_unavailable));
        }
        return requirements;
    }

    private List<String> buildSkills(Job source) {
        Set<String> unique = new LinkedHashSet<>();
        if (source != null) {
            if (source.getRequiredSkills() != null) {
                for (String skill : source.getRequiredSkills()) {
                    String clean = safe(skill);
                    if (!clean.isEmpty()) {
                        unique.add(clean);
                    }
                }
            }
            addWords(unique, source.getTitle());
            addWords(unique, source.getJobType());
            addWords(unique, source.getWorkMode());
        }

        String description = safe(source == null ? null : source.getDescription()).toLowerCase(Locale.ROOT);
        if (description.contains("java")) unique.add("Java");
        if (description.contains("spring")) unique.add("Spring");
        if (description.contains("react")) unique.add("React");
        if (description.contains("figma")) unique.add("Figma");
        if (description.contains("android")) unique.add("Android");
        if (description.contains("ui")) unique.add("UI");
        if (description.contains("ux")) unique.add("UX");

        if (unique.isEmpty()) {
            unique.add("Communication");
            unique.add("Teamwork");
            unique.add("Problem Solving");
        }

        List<String> skills = new ArrayList<>();
        for (String value : unique) {
            String skill = value.trim();
            if (skill.length() < 2 || skill.length() > 20) {
                continue;
            }
            skills.add(skill);
            if (skills.size() == 8) {
                break;
            }
        }
        return skills;
    }

    private void addWords(Set<String> set, String value) {
        String safeValue = safe(value);
        if (safeValue.isEmpty()) {
            return;
        }
        String[] parts = safeValue.split("[^A-Za-z0-9+#]+");
        for (String part : parts) {
            if (part.length() >= 3 && part.length() <= 16) {
                set.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
    }

    private void applyInsets(View root) {
        View hero = root.findViewById(R.id.hero_content);
        View toolbar = root.findViewById(R.id.toolbar);
        View scroll = root.findViewById(R.id.details_scroll);
        View bottom = root.findViewById(R.id.bottom_action_bar);

        final int heroTop = hero.getPaddingTop();
        final int toolbarTop = toolbar.getPaddingTop();
        final int scrollBottom = scroll.getPaddingBottom();
        final int bottomPadding = bottom.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            hero.setPadding(hero.getPaddingLeft(), heroTop + bars.top, hero.getPaddingRight(), hero.getPaddingBottom());
            toolbar.setPadding(toolbar.getPaddingLeft(), toolbarTop + bars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            int bottomActionSpace = recruiterViewOnly ? 0 : dp(68);
            scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(), scroll.getPaddingRight(), scrollBottom + bars.bottom + bottomActionSpace);
            bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(), bottomPadding + bars.bottom);
            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        String candidate = safe(value);
        return candidate.isEmpty() ? fallback : candidate;
    }

    private void showDetailsSkeleton(boolean show) {
        if (!isAdded()) {
            return;
        }
        if (!show) {
            SkeletonShimmerHelper.stop(skeletonAnimators);
            if (skeletonDialog != null && skeletonDialog.isShowing()) {
                skeletonDialog.dismiss();
            }
            return;
        }

        if (skeletonDialog == null) {
            skeletonDialog = new Dialog(requireContext());
            skeletonDialog.setContentView(R.layout.dialog_job_details_skeleton);
            skeletonDialog.setCancelable(false);
            if (skeletonDialog.getWindow() != null) {
                skeletonDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                skeletonDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        }
        if (!skeletonDialog.isShowing()) {
            skeletonDialog.show();
            View root = skeletonDialog.findViewById(R.id.skeleton_root_job_details);
            SkeletonShimmerHelper.start(root, skeletonAnimators);
        }
    }

    private void animateDetailSections(View root) {
        int[] sectionIds = new int[]{
                R.id.app_bar,
                R.id.tv_application_status,
                R.id.tv_description,
                R.id.ll_requirements,
                R.id.flex_skills
        };
        for (int i = 0; i < sectionIds.length; i++) {
            View section = root.findViewById(sectionIds[i]);
            if (section == null) {
                continue;
            }
            section.animate().cancel();
            section.setAlpha(0f);
            section.setTranslationY(dp(10));
            section.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(260L)
                    .setStartDelay((long) i * 45L)
                    .start();
        }
    }

    @Override
    public void onDestroyView() {
        showDetailsSkeleton(false);
        super.onDestroyView();
    }
}
