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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.dto.UserDto;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.data.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Recruiter-specific profile page.
 * Shows identity header, stats (total jobs, active jobs, total applicants),
 * and actions: post job, my jobs, settings, logout.
 * Deliberately hides all seeker-only features.
 */
public class RecruiterProfileFragment extends Fragment {

    private JobNetRepository repository;
    private View profileContent;
    private View profileSkeleton;
    private final List<ObjectAnimator> skeletonAnimators = new ArrayList<>();
    private int pendingProfileLoads;
    private boolean skipNextResumeRefresh = true;
    private int profileRequestVersion = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recruiter_profile, container, false);
    }

    @Override
    public void onPause() {
        showProfileSkeleton(false);
        super.onPause();
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
            refreshProfileContent(root);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());
        profileContent = view.findViewById(R.id.layout_recruiter_profile_content);
        profileSkeleton = view.findViewById(R.id.layout_recruiter_profile_skeleton);

        bindSessionIdentity(view);
        refreshProfileContent(view);
        wireActions(view);
    }

    private void refreshProfileContent(View root) {
        int requestVersion = ++profileRequestVersion;
        pendingProfileLoads = 2;
        showProfileSkeleton(true);
        loadProfileFromApi(root, requestVersion);
        loadRecruiterStats(root, requestVersion);
    }

    private void bindSessionIdentity(View view) {
        SessionManager session = new SessionManager(requireContext());
        String name = session.getUserName();
        String email = session.getUserEmail();

        if (name != null && !name.isBlank()) {
            ((TextView) view.findViewById(R.id.tv_recruiter_name)).setText(name);
            String initial = name.substring(0, 1).toUpperCase(Locale.ROOT);
            ((TextView) view.findViewById(R.id.tv_recruiter_avatar_initial)).setText(initial);
        }
        if (email != null && !email.isBlank()) {
            ((TextView) view.findViewById(R.id.tv_recruiter_email)).setText(email);
        }
    }

    private void loadProfileFromApi(View view, int requestVersion) {
        repository.fetchProfile(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(UserDto data) {
                if (!isActiveProfileRequest(view, requestVersion) || data == null) {
                    return;
                }
                String name = data.name != null && !data.name.isBlank() ? data.name : data.userName;
                if (name != null && !name.isBlank()) {
                    ((TextView) view.findViewById(R.id.tv_recruiter_name)).setText(name);
                    String initial = name.substring(0, 1).toUpperCase(Locale.ROOT);
                    ((TextView) view.findViewById(R.id.tv_recruiter_avatar_initial)).setText(initial);
                }
                if (data.email != null && !data.email.isBlank()) {
                    ((TextView) view.findViewById(R.id.tv_recruiter_email)).setText(data.email);
                }
                markProfileLoadCompleted(requestVersion);
            }

            @Override
            public void onError(Throwable throwable) {
                // Keep session-bound values as fallback.
                if (!isActiveProfileRequest(view, requestVersion)) {
                    return;
                }
                markProfileLoadCompleted(requestVersion);
            }
        });
    }

    private void loadRecruiterStats(View view, int requestVersion) {
        TextView tvTotal = view.findViewById(R.id.tv_stat_total_jobs);
        TextView tvActive = view.findViewById(R.id.tv_stat_active_jobs);
        TextView tvApplicants = view.findViewById(R.id.tv_stat_total_applicants);

        repository.loadRecruiterPostedJobs(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Job> data) {
                if (!isActiveProfileRequest(view, requestVersion)) {
                    return;
                }
                int total = 0;
                int active = 0;
                int applicants = 0;
                if (data != null) {
                    for (Job job : data) {
                        String status = job.getStatus() == null ? "" : job.getStatus().trim().toUpperCase(Locale.ROOT);
                        if ("DRAFT".equals(status)) {
                            continue;
                        }
                        total++;
                        if ("PUBLISHED".equals(status) || status.isBlank()) {
                            active++;
                        }
                        applicants += job.getApplicantsCount();
                    }
                }
                tvTotal.setText(String.valueOf(total));
                tvActive.setText(String.valueOf(active));
                tvApplicants.setText(String.valueOf(applicants));
                markProfileLoadCompleted(requestVersion);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isActiveProfileRequest(view, requestVersion)) {
                    return;
                }
                tvTotal.setText("0");
                tvActive.setText("0");
                tvApplicants.setText("0");
                markProfileLoadCompleted(requestVersion);
            }
        });
    }

    private void wireActions(View view) {
        view.findViewById(R.id.btn_edit_recruiter_profile).setOnClickListener(v ->
            Navigation.findNavController(view)
                .navigate(R.id.action_recruiterProfileFragment_to_recruiterEditProfileFragment));

        view.findViewById(R.id.btn_recruiter_settings).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Settings", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btn_recruiter_settings_action).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Settings", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btn_recruiter_post_job).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_recruiterProfileFragment_to_recruiterPostJobFragment));

        view.findViewById(R.id.btn_recruiter_my_jobs).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_recruiterProfileFragment_to_recruiterDashboardFragment));

        view.findViewById(R.id.btn_recruiter_logout).setOnClickListener(v ->
                showLogoutDialog(view));
    }

    private void showLogoutDialog(View rootView) {
        if (!isAdded()) {
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

        ((TextView) dialog.findViewById(R.id.tv_dialog_title)).setText(R.string.logout_confirm_title);
        ((TextView) dialog.findViewById(R.id.tv_dialog_message)).setText(R.string.logout_confirm_message);

        TextView btnConfirm = dialog.findViewById(R.id.btn_dialog_confirm);
        btnConfirm.setText(R.string.confirm_logout);
        btnConfirm.setBackgroundResource(R.drawable.bg_dialog_button_danger);
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            repository.clearSessionData();
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            Navigation.findNavController(rootView)
                    .navigate(R.id.loginFragment, null, navOptions);
            Toast.makeText(requireContext(), getString(R.string.logged_out_success), Toast.LENGTH_SHORT).show();
        });
        dialog.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private boolean isActiveProfileRequest(View expectedRoot, int requestVersion) {
        return isAdded() && getView() == expectedRoot && requestVersion == profileRequestVersion;
    }

    private void markProfileLoadCompleted(int requestVersion) {
        if (!isAdded() || requestVersion != profileRequestVersion) {
            return;
        }
        pendingProfileLoads = Math.max(0, pendingProfileLoads - 1);
        if (pendingProfileLoads == 0) {
            showProfileSkeleton(false);
        }
    }

    private void showProfileSkeleton(boolean show) {
        if (profileContent == null || profileSkeleton == null) {
            return;
        }

        if (!show) {
            profileContent.setVisibility(View.VISIBLE);
            profileSkeleton.setVisibility(View.GONE);
            stopProfileSkeletonAnimations();
            return;
        }

        if (!isAdded()) {
            return;
        }

        profileContent.setVisibility(View.INVISIBLE);
        profileSkeleton.setVisibility(View.VISIBLE);
        startProfileSkeletonAnimations();
    }

    private void startProfileSkeletonAnimations() {
        stopProfileSkeletonAnimations();
        int[] skeletonItemIds = new int[]{
                R.id.skel_profile_header_title,
                R.id.skel_profile_header_action,
                R.id.skel_profile_avatar,
                R.id.skel_profile_name,
                R.id.skel_profile_role,
                R.id.skel_profile_email,
                R.id.skel_profile_edit,
                R.id.skel_profile_stat_1,
                R.id.skel_profile_stat_2,
                R.id.skel_profile_stat_3,
            R.id.skel_profile_actions_card,
            R.id.skel_profile_row_1,
            R.id.skel_profile_row_2,
            R.id.skel_profile_row_3,
            R.id.skel_profile_logout_row,
            R.id.skel_profile_logout_text
        };
        for (int i = 0; i < skeletonItemIds.length; i++) {
            if (profileSkeleton == null) {
                continue;
            }
            View block = profileSkeleton.findViewById(skeletonItemIds[i]);
            if (block == null) {
                continue;
            }
            ObjectAnimator animator = ObjectAnimator.ofFloat(block, "alpha", 0.4f, 1f);
            animator.setDuration(900);
            animator.setStartDelay((i % 4) * 80L);
            animator.setRepeatMode(ObjectAnimator.REVERSE);
            animator.setRepeatCount(ObjectAnimator.INFINITE);
            animator.start();
            skeletonAnimators.add(animator);
        }
    }

    private void stopProfileSkeletonAnimations() {
        for (ObjectAnimator animator : skeletonAnimators) {
            if (animator != null) {
                animator.cancel();
            }
        }
        skeletonAnimators.clear();
    }

    @Override
    public void onDestroyView() {
        profileRequestVersion++;
        pendingProfileLoads = 0;
        showProfileSkeleton(false);
        profileContent = null;
        profileSkeleton = null;
        super.onDestroyView();
    }
}
