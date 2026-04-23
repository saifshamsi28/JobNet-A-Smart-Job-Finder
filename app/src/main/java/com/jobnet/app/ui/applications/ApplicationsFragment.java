package com.jobnet.app.ui.applications;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.jobnet.app.R;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.util.SkeletonShimmerHelper;
import com.jobnet.app.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ApplicationsFragment extends Fragment {

    private JobNetRepository repository;
    private final List<ApplicationDto> allApplications = new ArrayList<>();
    private String activeFilter = "ALL";
    private Dialog skeletonDialog;
    private final List<ObjectAnimator> skeletonAnimators = new ArrayList<>();
    private boolean skipNextResumeRefresh = true;
    private int applicationsRequestVersion = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_applications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());

        view.findViewById(R.id.btn_back_applications).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        setupStatusFilters(view);

        loadApplications(view, true);
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
            loadApplications(root, allApplications.isEmpty());
        }
    }

    private void loadApplications(View view, boolean showSkeleton) {
        LinearLayout container = view.findViewById(R.id.container_applications);
        View emptyView = view.findViewById(R.id.tv_no_applications);
        View progress = view.findViewById(R.id.applications_progress);
        final int requestVersion = ++applicationsRequestVersion;

        progress.setVisibility(View.GONE);
        if (showSkeleton) {
            showApplicationsSkeleton(true);
        }

        repository.loadMyApplications(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<ApplicationDto> data) {
                if (!isActiveApplicationsRequest(requestVersion, view)) {
                    return;
                }
                progress.setVisibility(View.GONE);
                showApplicationsSkeleton(false);
                allApplications.clear();
                if (data != null) {
                    allApplications.addAll(data);
                }
                renderApplications(view);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isActiveApplicationsRequest(requestVersion, view)) {
                    return;
                }
                progress.setVisibility(View.GONE);
                showApplicationsSkeleton(false);
                if (!showSkeleton && !allApplications.isEmpty()) {
                    return;
                }
                allApplications.clear();
                container.removeAllViews();
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    private boolean isActiveApplicationsRequest(int requestVersion, View expectedRoot) {
        return isAdded() && requestVersion == applicationsRequestVersion && getView() == expectedRoot;
    }

    private void renderApplications(View root) {
        LinearLayout container = root.findViewById(R.id.container_applications);
        View emptyView = root.findViewById(R.id.tv_no_applications);
        TextView countView = root.findViewById(R.id.tv_applications_count);

        container.removeAllViews();

        List<ApplicationDto> filtered = getFilteredApplications();
        
        // Update count display
        if (countView != null) {
            if (filtered.isEmpty()) {
                countView.setVisibility(View.GONE);
            } else {
                String countText;
                if ("ALL".equals(activeFilter)) {
                    countText = getString(R.string.applications_count_all, filtered.size());
                } else {
                    countText = getString(R.string.applications_count_filtered, filtered.size(), displayStatus(activeFilter).toLowerCase());
                }
                countView.setText(countText);
                countView.setVisibility(View.VISIBLE);
            }
        }
        
        if (filtered.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        emptyView.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < filtered.size(); i++) {
            ApplicationDto application = filtered.get(i);
            View item = inflater.inflate(R.layout.item_application, container, false);
            bindApplicationItem(item, application);
            animateApplicationEntry(item, i);
            container.addView(item);
        }
    }

    private void bindApplicationItem(View item, ApplicationDto application) {
        TextView title = item.findViewById(R.id.tv_app_job_title);
        TextView company = item.findViewById(R.id.tv_app_company);
        TextView status = item.findViewById(R.id.tv_app_status);
        TextView updated = item.findViewById(R.id.tv_app_updated);

        title.setText(defaultIfBlank(application.jobTitle, "Untitled Role"));
        company.setText(defaultIfBlank(application.company, "Unknown Company"));

        String normalizedStatus = normalizeStatus(application.status);
        status.setText(displayStatus(normalizedStatus));
        applyStatusStyle(status, normalizedStatus);
        String formattedTime = DateTimeUtils.formatRelativeDateTime(application.updatedAt, application.appliedAt);
        if (formattedTime == null || formattedTime.isBlank()) {
            formattedTime = getString(R.string.notification_recent);
        }
        updated.setText(getString(R.string.application_updated_time, formattedTime));

        item.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putSerializable("application", application);
            Navigation.findNavController(item).navigate(R.id.action_applicationsFragment_to_applicationTimelineFragment, args);
        });
    }

    private void setupStatusFilters(View root) {
        TextView chipAll = root.findViewById(R.id.chip_all);
        View parent = (View) chipAll.getParent();
        if (!(parent instanceof LinearLayout)) {
            return;
        }
        LinearLayout chipRow = (LinearLayout) parent;
        for (int i = 0; i < chipRow.getChildCount(); i++) {
            View child = chipRow.getChildAt(i);
            if (!(child instanceof TextView)) {
                continue;
            }
            TextView chip = (TextView) child;
            chip.setOnClickListener(v -> {
                activeFilter = normalizeStatus(chip.getText().toString());
                applyChipSelection(chipRow);
                renderApplications(root);
            });
        }
        applyChipSelection(chipRow);
    }

    private void applyChipSelection(LinearLayout chipRow) {
        for (int i = 0; i < chipRow.getChildCount(); i++) {
            View child = chipRow.getChildAt(i);
            if (!(child instanceof TextView)) {
                continue;
            }
            TextView chip = (TextView) child;
            String chipStatus = normalizeStatus(chip.getText().toString());
            boolean selected = chipStatus.equals(activeFilter);
            chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), selected ? R.color.text_chip_selected : R.color.text_chip));
        }
    }

    private List<ApplicationDto> getFilteredApplications() {
        if (allApplications.isEmpty()) {
            return Collections.emptyList();
        }
        if ("ALL".equals(activeFilter)) {
            return new ArrayList<>(allApplications);
        }

        List<ApplicationDto> filtered = new ArrayList<>();
        for (ApplicationDto application : allApplications) {
            String status = normalizeStatus(application.status);
            if (activeFilter.equals(status)) {
                filtered.add(application);
            }
        }
        return filtered;
    }

    private void applyStatusStyle(TextView view, String status) {
        int textColor;
        int bgColor;
        switch (status) {
            case "REVIEWED":
                textColor = R.color.status_reviewed_text;
                bgColor = R.color.status_reviewed_bg;
                break;
            case "SHORTLISTED":
                textColor = R.color.status_shortlisted_text;
                bgColor = R.color.status_shortlisted_bg;
                break;
            case "REJECTED":
                textColor = R.color.status_rejected_text;
                bgColor = R.color.status_rejected_bg;
                break;
            case "OFFERED":
                textColor = R.color.status_offered_text;
                bgColor = R.color.status_offered_bg;
                break;
            case "INTERVIEW":
                textColor = R.color.status_reviewed_text;
                bgColor = R.color.status_reviewed_bg;
                break;
            case "WITHDRAWN":
                textColor = R.color.status_withdrawn_text;
                bgColor = R.color.status_withdrawn_bg;
                break;
            default:
                textColor = R.color.status_applied_text;
                bgColor = R.color.status_applied_bg;
                break;
        }

        view.setTextColor(ContextCompat.getColor(requireContext(), textColor));
        Drawable bg = DrawableCompat.wrap(view.getBackground().mutate());
        if (bg instanceof GradientDrawable) {
            ((GradientDrawable) bg).setColor(ContextCompat.getColor(requireContext(), bgColor));
            view.setBackground(bg);
            return;
        }
        DrawableCompat.setTint(bg, ContextCompat.getColor(requireContext(), bgColor));
        view.setBackground(bg);
    }

    private String normalizeStatus(String rawStatus) {
        String normalized = defaultIfBlank(rawStatus, "APPLIED").trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("UNDER_REVIEW".equals(normalized) || "IN_REVIEW".equals(normalized)) {
            return "REVIEWED";
        }
        return normalized;
    }

    private String displayStatus(String normalizedStatus) {
        switch (normalizedStatus) {
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

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private void showApplicationsSkeleton(boolean show) {
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
            skeletonDialog.setContentView(R.layout.dialog_applications_skeleton);
            skeletonDialog.setCancelable(false);
            if (skeletonDialog.getWindow() != null) {
                skeletonDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                skeletonDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        }
        if (!skeletonDialog.isShowing()) {
            skeletonDialog.show();
            View root = skeletonDialog.findViewById(R.id.skeleton_root_applications);
            SkeletonShimmerHelper.start(root, skeletonAnimators);
        }
    }

    private void animateApplicationEntry(View item, int index) {
        item.animate().cancel();
        item.setAlpha(0f);
        item.setTranslationY(dp(10));
        item.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260L)
                .setStartDelay((long) Math.min(index, 7) * 42L)
                .start();
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        applicationsRequestVersion++;
        showApplicationsSkeleton(false);
        super.onDestroyView();
    }
}
