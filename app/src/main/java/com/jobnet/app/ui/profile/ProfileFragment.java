package com.jobnet.app.ui.profile;

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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.network.dto.UserDto;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.data.session.NotificationReadStateStore;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.R;
import com.jobnet.app.ui.notifications.NotificationItem;
import com.jobnet.app.util.SkeletonShimmerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

        private JobNetRepository repository;
        private Dialog skeletonDialog;
        private final List<ObjectAnimator> skeletonAnimators = new ArrayList<>();
        private int pendingProfileLoads;
        private boolean skipNextResumeRefresh = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());
        applyInsets(view);
        hideStaticSearchSection(view);
        bindSessionIdentity(view);

        refreshProfileContent(view);

        ProgressBar progressRing = view.findViewById(R.id.progress_profile);
        ObjectAnimator animator = ObjectAnimator.ofInt(progressRing, "progress", 0, 72);
        animator.setDuration(1200);
        animator.start();

        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_editProfileFragment));

        view.findViewById(R.id.btn_profile_settings_icon).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_profileSettingsFragment));

        view.findViewById(R.id.btn_profile_notifications).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_notificationsFragment));

        view.findViewById(R.id.btn_upload_resume).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Upload Resume", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btn_settings).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_profileSettingsFragment));

        view.findViewById(R.id.btn_my_applications).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_profileFragment_to_applicationsFragment));

        view.findViewById(R.id.stat_saved_container).setOnClickListener(v ->
                navigateToTopLevel(R.id.savedFragment));
        view.findViewById(R.id.stat_recommended_container).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_profileFragment_to_searchFragment));
        view.findViewById(R.id.stat_applied_container).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_profileFragment_to_applicationsFragment));

                SessionManager sessionManager = new SessionManager(requireContext());
                String role = sessionManager.getUserRole();
                if (role != null && role.toUpperCase(Locale.ROOT).contains("RECRUITER")) {
                        view.findViewById(R.id.btn_my_applications).setVisibility(View.GONE);
                        TextView profileTitle = view.findViewById(R.id.tv_profile_title);
                        profileTitle.setText(R.string.recruiter_role_title);
                }

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutConfirmation(view));

        view.findViewById(R.id.btn_see_recommended).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_searchFragment));

                refreshNotificationBadge(view);
    }

        @Override
        public void onResume() {
                super.onResume();
                View root = getView();
                if (root != null) {
                        if (skipNextResumeRefresh) {
                                skipNextResumeRefresh = false;
                        } else {
                                refreshProfileContent(root);
                        }
                        refreshNotificationBadge(root);
                }
        }

        private void refreshProfileContent(View root) {
                pendingProfileLoads = 4;
                showProfileSkeleton(true);
                loadProfile(root);
                loadProfileStats(root);
        }

        @Override
        public void onPause() {
                showProfileSkeleton(false);
                super.onPause();
        }

        private void loadProfile(View view) {
                TextView profileName = view.findViewById(R.id.tv_profile_name);
                TextView profileTitle = view.findViewById(R.id.tv_profile_title);
                TextView profileCompany = view.findViewById(R.id.tv_profile_company);

                repository.fetchProfile(new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(UserDto data) {
                                if (!isAdded() || data == null) {
                                                markProfileLoadCompleted();
                                        return;
                                }
                                if (data.name != null && !data.name.isBlank()) {
                                        profileName.setText(data.name);
                                } else if (data.userName != null && !data.userName.isBlank()) {
                                        profileName.setText(data.userName);
                                }

                                String title = null;
                                if (data.skills != null && !data.skills.isEmpty()) {
                                        title = data.skills.get(0).toUpperCase() + " PROFESSIONAL";
                                }
                                if (title == null || title.isBlank()) {
                                        title = getString(R.string.default_profile_title);
                                }
                                profileTitle.setText(title);

                                if (data.email != null && !data.email.isBlank()) {
                                        profileCompany.setText(data.email);
                                }
                                markProfileLoadCompleted();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                                // Keep session/default values when profile fetch fails.
                                markProfileLoadCompleted();
                        }
                });
        }

        private void bindSessionIdentity(View view) {
                SessionManager sessionManager = new SessionManager(requireContext());
                TextView profileName = view.findViewById(R.id.tv_profile_name);
                TextView profileCompany = view.findViewById(R.id.tv_profile_company);
                String userName = sessionManager.getUserName();
                String email = sessionManager.getUserEmail();

                if (userName != null && !userName.isBlank()) {
                        profileName.setText(userName);
                }
                if (email != null && !email.isBlank()) {
                        profileCompany.setText(email);
                }
        }

        private void loadProfileStats(View view) {
                TextView saved = view.findViewById(R.id.tv_profile_stat_saved);
                TextView recommended = view.findViewById(R.id.tv_profile_stat_recommended);
                TextView openings = view.findViewById(R.id.tv_profile_stat_openings);
                TextView recommendationCount = view.findViewById(R.id.tv_recommended_count);
                TextView recommendationTitle = view.findViewById(R.id.tv_recommended_title);
                TextView recommendationCompany = view.findViewById(R.id.tv_recommended_company);

                repository.loadSavedJobs(new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(List<Job> data) {
                                if (!isAdded()) {
                                        markProfileLoadCompleted();
                                        return;
                                }
                                saved.setText(String.valueOf(data == null ? 0 : data.size()));
                                markProfileLoadCompleted();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                                if (!isAdded()) {
                                        markProfileLoadCompleted();
                                        return;
                                }
                                saved.setText("0");
                                markProfileLoadCompleted();
                        }
                });

                repository.loadHomeData(new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(JobNetRepository.HomeData data) {
                                if (!isAdded() || data == null) {
                                        markProfileLoadCompleted();
                                        return;
                                }
                                int recommendedCount = data.recommended == null ? 0 : data.recommended.size();
                                int openingsCount = data.featured == null ? 0 : data.featured.size();
                                recommended.setText(String.valueOf(recommendedCount));
                                recommendationCount.setText(String.valueOf(recommendedCount));
                                if (data.recommended != null && !data.recommended.isEmpty()) {
                                        Job top = data.recommended.get(0);
                                        recommendationTitle.setText(top.getTitle());
                                        recommendationCompany.setText(top.getCompany());
                                }
                                markProfileLoadCompleted();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                                if (!isAdded()) {
                                        markProfileLoadCompleted();
                                        return;
                                }
                                recommended.setText("0");
                                markProfileLoadCompleted();
                        }
                });

                repository.loadMyApplications(new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(List<ApplicationDto> data) {
                                if (!isAdded()) {
                                        markProfileLoadCompleted();
                                        return;
                                }
                                openings.setText(String.valueOf(data == null ? 0 : data.size()));
                                markProfileLoadCompleted();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                                if (!isAdded()) {
                                        markProfileLoadCompleted();
                                        return;
                                }
                                openings.setText("0");
                                markProfileLoadCompleted();
                        }
                });
        }

        private void refreshNotificationBadge(View root) {
                TextView badge = root.findViewById(R.id.view_profile_notification_badge);
                if (badge == null || !isAdded()) {
                        return;
                }

                SessionManager sessionManager = new SessionManager(requireContext());
                String role = sessionManager.getUserRole();
                if (role != null && role.toUpperCase(Locale.ROOT).contains("RECRUITER")) {
                        badge.setVisibility(View.GONE);
                        return;
                }

                repository.loadMyApplications(new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(List<ApplicationDto> data) {
                                if (!isAdded()) {
                                        return;
                                }
                                NotificationReadStateStore readStateStore = new NotificationReadStateStore(requireContext());
                                int unread = 0;
                                if (data != null) {
                                        for (ApplicationDto app : data) {
                                                if (app == null || app.status == null) {
                                                        continue;
                                                }
                                                String readKey = NotificationReadStateStore.buildKey(
                                                        NotificationItem.TYPE_SEEKER_STATUS,
                                                        app.id,
                                                        app.jobId,
                                                        app.status,
                                                        app.updatedAt,
                                                        app.appliedAt
                                                );
                                                if (!readStateStore.isRead(readKey)) {
                                                        unread++;
                                                }
                                        }
                                }

                                if (unread > 0) {
                                        badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                                        badge.setVisibility(View.VISIBLE);
                                } else {
                                        badge.setVisibility(View.GONE);
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

        private void navigateToTopLevel(int destinationId) {
                NavController navController = Navigation.findNavController(requireView());
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                        .setEnterAnim(R.anim.slide_in_right)
                        .setExitAnim(R.anim.slide_out_left)
                        .setPopEnterAnim(R.anim.slide_in_left)
                        .setPopExitAnim(R.anim.slide_out_right)
                        .build();
                navController.navigate(destinationId, null, options);
        }

        private void showLogoutConfirmation(View view) {
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
                                .setPopUpTo(R.id.homeFragment, true)
                                .build();
                        Navigation.findNavController(view)
                                .navigate(R.id.action_profileFragment_to_loginFragment, null, navOptions);
                        Toast.makeText(requireContext(), R.string.logged_out_success, Toast.LENGTH_SHORT).show();
                });
                dialog.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
                dialog.show();
        }

        private void markProfileLoadCompleted() {
                if (!isAdded()) {
                        return;
                }
                pendingProfileLoads = Math.max(0, pendingProfileLoads - 1);
                if (pendingProfileLoads == 0) {
                        showProfileSkeleton(false);
                }
        }

        private void showProfileSkeleton(boolean show) {
                if (!show) {
                        SkeletonShimmerHelper.stop(skeletonAnimators);
                        if (skeletonDialog != null && skeletonDialog.isShowing()) {
                                try {
                                        skeletonDialog.dismiss();
                                } catch (Exception ignored) {
                                }
                        }
                        return;
                }

                if (!isAdded()) {
                        return;
                }

                if (skeletonDialog == null) {
                        skeletonDialog = new Dialog(requireContext());
                        skeletonDialog.setContentView(R.layout.dialog_profile_skeleton);
                        skeletonDialog.setCancelable(false);
                        if (skeletonDialog.getWindow() != null) {
                                skeletonDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                skeletonDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        }
                }
                if (!skeletonDialog.isShowing()) {
                        skeletonDialog.show();
                        View root = skeletonDialog.findViewById(R.id.skeleton_root_profile_seeker);
                        SkeletonShimmerHelper.start(root, skeletonAnimators);
                }
        }

        private void hideStaticSearchSection(View view) {
                View section = view.findViewById(R.id.section_job_searches);
                if (section != null) {
                        section.setVisibility(View.GONE);
                }
        }

        private void applyInsets(View root) {
                View toolbar = root.findViewById(R.id.profile_toolbar_row);
                View scroll = root.findViewById(R.id.profile_scroll);

                final int toolbarTop = toolbar.getPaddingTop();
                final int scrollBottom = scroll.getPaddingBottom();

                ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                        Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                        toolbar.setPadding(toolbar.getPaddingLeft(), toolbarTop + bars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
                        scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(), scroll.getPaddingRight(), scrollBottom + bars.bottom);
                        return insets;
                });
                        ViewCompat.requestApplyInsets(root);
        }

        @Override
        public void onDestroyView() {
                showProfileSkeleton(false);
                super.onDestroyView();
        }
}
