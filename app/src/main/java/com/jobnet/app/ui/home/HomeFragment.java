package com.jobnet.app.ui.home;

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
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.databinding.FragmentHomeBinding;
import com.jobnet.app.util.SampleData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private JobNetRepository repository;
    private final List<View> shimmerViews = new ArrayList<>();

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
                // Navigate to search with category filter
            });

            binding.containerCategories.addView(cardView, params);
        }
    }

    private void loadHomeJobs() {
        repository.loadHomeData(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(JobNetRepository.HomeData data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                stopShimmer();
                List<Job> featured = data == null ? Collections.emptyList() : data.featured;
                List<Job> recommended = data == null ? Collections.emptyList() : data.recommended;
                loadFeaturedJobs(featured);
                loadRecommendedJobs(recommended);
                bindStats(featured, recommended);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded() || binding == null) {
                    return;
                }
                stopShimmer();
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
            salary.setText(job.getSalary());
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
        salary.setText(job.getSalary());
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

        binding.btnSeeAllCategories.setOnClickListener(v ->
            navigateToSearchTab());
        binding.btnSeeAllFeatured.setOnClickListener(v ->
            navigateToSearchTab());
        binding.btnSeeAllRecommended.setOnClickListener(v ->
            navigateToSearchTab());

        binding.btnFilter.setOnClickListener(v -> {
            // Show filter bottom sheet
        });
    }

    private void navigateToSearchTab() {
        NavController navController = Navigation.findNavController(requireView());
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                .build();
        navController.navigate(R.id.searchFragment, null, options);
    }

    private void showLoadingState() {
        stopShimmer();
        binding.containerFeatured.removeAllViews();
        binding.containerRecommended.removeAllViews();

        for (int i = 0; i < 2; i++) {
            View featuredBlock = buildFeaturedSkeleton();
            binding.containerFeatured.addView(featuredBlock);
            shimmerViews.add(featuredBlock);
        }

        for (int i = 0; i < 4; i++) {
            View listBlock = buildListSkeleton();
            binding.containerRecommended.addView(listBlock);
            shimmerViews.add(listBlock);
        }

        for (int i = 0; i < shimmerViews.size(); i++) {
            View block = shimmerViews.get(i);
            long delay = i * 90L;
            block.animate().alpha(0.45f).setStartDelay(delay).setDuration(580).withEndAction(() -> {
                if (block.getParent() != null) {
                    block.animate().alpha(1f).setDuration(580).withEndAction(() -> {
                        if (block.getParent() != null) {
                            block.animate().alpha(0.45f).setDuration(580).start();
                        }
                    }).start();
                }
            }).start();
        }
    }

    private void stopShimmer() {
        for (View block : shimmerViews) {
            block.animate().cancel();
            block.setAlpha(1f);
        }
        shimmerViews.clear();
    }

    private View buildFeaturedSkeleton() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_white);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(250), dp(150));
        params.setMarginEnd(dp(16));
        card.setLayoutParams(params);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        card.addView(skeletonLine(LinearLayout.LayoutParams.MATCH_PARENT, dp(18)));
        card.addView(skeletonSpacer(dp(10)));
        card.addView(skeletonLine(dp(130), dp(14)));
        card.addView(skeletonSpacer(dp(10)));
        card.addView(skeletonLine(dp(100), dp(14)));
        return card;
    }

    private View buildListSkeleton() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_white);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(12);
        card.setLayoutParams(params);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        card.addView(skeletonLine(LinearLayout.LayoutParams.MATCH_PARENT, dp(16)));
        card.addView(skeletonSpacer(dp(10)));
        card.addView(skeletonLine(dp(150), dp(14)));
        card.addView(skeletonSpacer(dp(10)));
        card.addView(skeletonLine(dp(180), dp(12)));
        return card;
    }

    private View skeletonLine(int width, int height) {
        View line = new View(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        line.setLayoutParams(lp);
        line.setBackgroundResource(R.drawable.bg_skeleton_block);
        return line;
    }

    private View skeletonSpacer(int height) {
        View spacer = new View(requireContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(1, height));
        return spacer;
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
