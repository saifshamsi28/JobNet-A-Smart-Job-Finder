package com.jobnet.app.ui.home;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.model.JobCategory;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.data.session.NotificationReadStateStore;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.databinding.FragmentHomeBinding;
import com.jobnet.app.ui.notifications.NotificationItem;
import com.jobnet.app.util.SkeletonShimmerHelper;
import com.jobnet.app.util.SalaryUtils;
import com.jobnet.app.util.SampleData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private JobNetRepository repository;
    private Dialog skeletonDialog;
    private final List<ObjectAnimator> skeletonAnimators = new ArrayList<>();
    private boolean skipNextResumeRefresh = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());
        applyInsets(view);
        setGreeting();
        bindHeaderIdentity();
        showLoadingState();
        loadCategories();
        loadHomeJobs();
        setupListeners();
        refreshNotificationDot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        if (binding == null) {
            return;
        }
        refreshNotificationDot();
        showLoadingState();
        bindHeaderIdentity();
        loadHomeJobs();
    }

    @Override
    public void onPause() {
        stopShimmer();
        super.onPause();
    }

    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning,";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon,";
        } else {
            greeting = "Good Evening,";
        }
        binding.tvGreeting.setText(greeting);
    }

    private void bindHeaderIdentity() {
        SessionManager sessionManager = new SessionManager(requireContext());
        String cachedName = sessionManager.getUserName();
        if (cachedName != null && !cachedName.isBlank()) {
            binding.tvName.setText(cachedName);
        }

        repository.fetchProfile(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(com.jobnet.app.data.network.dto.UserDto data) {
                if (!isAdded() || binding == null || data == null) {
                    return;
                }
                String actualName = data.name;
                if (actualName == null || actualName.isBlank()) {
                    actualName = data.userName;
                }
                if (actualName != null && !actualName.isBlank()) {
                    binding.tvName.setText(actualName);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                // Keep cached/session name when profile fetch fails.
            }
        });
    }

    private void loadCategories() {
        List<JobCategory> categories = SampleData.getCategories();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        binding.containerCategories.removeAllViews();

        for (int i = 0; i < categories.size(); i++) {
            JobCategory category = categories.get(i);
            View cardView = inflater.inflate(R.layout.item_category_card, binding.containerCategories, false);
            ImageView icon = cardView.findViewById(R.id.iv_category_icon);
            TextView name = cardView.findViewById(R.id.tv_category_name);

            icon.setImageResource(category.getIconRes());
            name.setText(category.getName());

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardView.getLayoutParams();
            if (params == null) {
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
            }
            if (i < categories.size() - 1) {
                params.setMarginEnd(dp(12));
            }

            cardView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("categoryName", category.getName());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_home_to_categoryJobsFragment, args);
            });

            binding.containerCategories.addView(cardView, params);
        }
    }

    private void loadHomeJobs() {
        repository.loadHomeData(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(JobNetRepository.HomeData data) {
                stopShimmer();
                if (!isAdded() || binding == null) {
                    return;
                }
                List<Job> featured = data == null ? Collections.emptyList() : data.featured;
                List<Job> recommended = data == null ? Collections.emptyList() : data.recommended;
                loadFeaturedJobs(featured);
                loadRecommendedJobs(recommended);
                bindStats(featured, recommended);
            }

            @Override
            public void onError(Throwable throwable) {
                stopShimmer();
                if (!isAdded() || binding == null) {
                    return;
                }
                Toast.makeText(requireContext(), "Could not refresh jobs right now", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindStats(List<Job> featured, List<Job> recommended) {
        binding.tvStatFeaturedValue.setText(String.valueOf(featured == null ? 0 : featured.size()));
        binding.tvStatRecommendedValue.setText(String.valueOf(recommended == null ? 0 : recommended.size()));
        repository.loadSavedJobs(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Job> data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.tvStatSavedValue.setText(String.valueOf(data == null ? 0 : data.size()));
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.tvStatSavedValue.setText("0");
            }
        });
    }

    private void loadFeaturedJobs(List<Job> featuredJobs) {
        binding.containerFeatured.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < featuredJobs.size(); i++) {
            Job job = featuredJobs.get(i);
            View cardView = inflater.inflate(R.layout.item_featured_card, binding.containerFeatured, false);
            TextView title = cardView.findViewById(R.id.tv_featured_title);
            TextView company = cardView.findViewById(R.id.tv_featured_company);
            TextView salary = cardView.findViewById(R.id.tv_featured_salary);
            TextView type = cardView.findViewById(R.id.tv_featured_type);
            ImageView bookmark = cardView.findViewById(R.id.iv_featured_bookmark);

            title.setText(job.getTitle());
            company.setText(job.getCompany());
            salary.setText(SalaryUtils.normalizeDisplay(job.getSalary()));
            type.setText(job.getWorkMode());
            applyFeaturedBookmarkState(bookmark, job);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardView.getLayoutParams();
            if (params == null) {
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
            }
            if (i < featuredJobs.size() - 1) {
                params.setMarginEnd(dp(16));
            }

            bookmark.setOnClickListener(v -> toggleBookmark(bookmark, job));
            cardView.setOnClickListener(v -> openJobDetail(job));
            animateCardEntry(cardView, i);

            binding.containerFeatured.addView(cardView, params);
        }
    }

    private void loadRecommendedJobs(List<Job> jobs) {
        binding.containerRecommended.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            View cardView = inflater.inflate(R.layout.item_job_card, binding.containerRecommended, false);
            bindJobCard(cardView, job);

            // Add margins between cards
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardView.getLayoutParams();
            if (params == null) {
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
            }
            if (i < jobs.size() - 1) {
                params.bottomMargin = dp(12);
            }

            animateCardEntry(cardView, i);
            binding.containerRecommended.addView(cardView, params);
        }
    }

    private void bindJobCard(View cardView, Job job) {
        TextView title = cardView.findViewById(R.id.tv_job_title);
        TextView company = cardView.findViewById(R.id.tv_company_name);
        TextView salary = cardView.findViewById(R.id.tv_salary);
        TextView location = cardView.findViewById(R.id.tv_location);
        TextView jobType = cardView.findViewById(R.id.tv_job_type);
        TextView workMode = cardView.findViewById(R.id.tv_work_mode);
        ImageView bookmark = cardView.findViewById(R.id.iv_bookmark);

        title.setText(job.getTitle());
        company.setText(job.getCompany());
        salary.setText(SalaryUtils.normalizeDisplay(job.getSalary()));
        location.setText(job.getLocation());
        jobType.setText(job.getJobType());
        workMode.setText(job.getWorkMode());

        applyBookmarkState(bookmark, job);

        bookmark.setOnClickListener(v -> toggleBookmark(bookmark, job));
        cardView.setOnClickListener(v -> openJobDetail(job));
    }

    private void toggleBookmark(ImageView bookmarkView, Job job) {
        boolean nextSaved = !job.isSaved();
        repository.toggleSave(job, nextSaved, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(Boolean data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                job.setSaved(nextSaved);
                applyBookmarkState(bookmarkView, job);
                applyFeaturedBookmarkState(bookmarkView, job);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded() || binding == null) {
                    return;
                }
                applyBookmarkState(bookmarkView, job);
                applyFeaturedBookmarkState(bookmarkView, job);
            }
        });

        // Animate scale
        bookmarkView.animate()
                .scaleX(1.3f).scaleY(1.3f)
                .setDuration(120)
                .withEndAction(() -> bookmarkView.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(120)
                        .start())
                .start();
    }

    private void openJobDetail(Job job) {
        Bundle args = new Bundle();
        args.putSerializable("job", job);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_home_to_jobDetails, args);
    }

    private void setupListeners() {
        binding.searchBar.setOnClickListener(v -> navigateToSearchTab());
        binding.btnNotification.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_home_to_notificationsFragment));
        binding.ivAvatar.setOnClickListener(v -> {
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
            navController.navigate(R.id.profileFragment, null, options);
        });

        binding.btnSeeAllCategories.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_home_to_categoriesFragment));
        binding.btnSeeAllFeatured.setOnClickListener(v ->
            navigateToSearchTab());
        binding.btnSeeAllRecommended.setOnClickListener(v ->
            navigateToSearchTab());

        binding.btnFilter.setOnClickListener(v -> {
            navigateToSearchTab();
        });
    }

    private void navigateToSearchTab() {
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
        navController.navigate(R.id.searchFragment, null, options);
    }

    private void refreshNotificationDot() {
        if (!isAdded() || binding == null) {
            return;
        }
        TextView badge = binding.btnNotification.findViewById(R.id.view_notification_badge_dot);
        if (badge == null) {
            return;
        }

        repository.loadMyApplications(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<ApplicationDto> data) {
                if (!isAdded() || binding == null) {
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
                if (!isAdded() || binding == null) {
                    return;
                }
                badge.setVisibility(View.GONE);
            }
        });
    }

    private void showLoadingState() {
        stopShimmer();
        showHomeSkeleton(true);
    }

    private void stopShimmer() {
        showHomeSkeleton(false);
    }

    private void showHomeSkeleton(boolean show) {
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
            skeletonDialog.setContentView(R.layout.dialog_home_skeleton);
            skeletonDialog.setCancelable(false);
            if (skeletonDialog.getWindow() != null) {
                skeletonDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                skeletonDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        }
        if (!skeletonDialog.isShowing()) {
            skeletonDialog.show();
            View root = skeletonDialog.findViewById(R.id.skeleton_root_home);
            SkeletonShimmerHelper.start(root, skeletonAnimators);
        }
    }

    private void animateCardEntry(View cardView, int index) {
        if (cardView == null) {
            return;
        }
        cardView.animate().cancel();
        cardView.setAlpha(0f);
        cardView.setTranslationY(dp(10));
        cardView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260L)
                .setStartDelay((long) Math.min(index, 6) * 42L)
                .start();
    }

    private void applyInsets(View root) {
        final int baseHeaderTop = binding.headerContainer.getPaddingTop();
        final int baseScrollBottom = binding.scrollView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.headerContainer.setPadding(
                    binding.headerContainer.getPaddingLeft(),
                    baseHeaderTop + bars.top,
                    binding.headerContainer.getPaddingRight(),
                    binding.headerContainer.getPaddingBottom());
            binding.scrollView.setPadding(
                    binding.scrollView.getPaddingLeft(),
                    binding.scrollView.getPaddingTop(),
                    binding.scrollView.getPaddingRight(),
                    baseScrollBottom + bars.bottom);
            return insets;
        });
    }

    private void applyBookmarkState(ImageView view, Job job) {
        view.setImageResource(job.isSaved() ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        ImageViewCompat.setImageTintList(view, null);
        if (job.isSaved()) {
            view.clearColorFilter();
        } else {
            view.setColorFilter(ContextCompat.getColor(requireContext(), R.color.bookmark_unsaved));
        }
        view.setAlpha(1f);
    }

    private void applyFeaturedBookmarkState(ImageView view, Job job) {
        view.setImageResource(job.isSaved() ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        ImageViewCompat.setImageTintList(view, null);
        if (job.isSaved()) {
            view.clearColorFilter();
        } else {
            view.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_on_dark_67));
        }
        view.setAlpha(1f);
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        stopShimmer();
        super.onDestroyView();
        binding = null;
    }
}
