package com.jobnet.app.ui.categories;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.ui.home.JobsAdapter;
import com.jobnet.app.util.SkeletonShimmerHelper;
import com.jobnet.app.util.SampleData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CategoryJobsFragment extends Fragment {

    private JobNetRepository repository;
    private JobsAdapter adapter;
    private Dialog skeletonDialog;
    private final List<ObjectAnimator> skeletonAnimators = new ArrayList<>();
    private boolean skipNextResumeRefresh = true;

    private String categoryName = "General";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category_jobs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());
        applyInsets(view);

        if (getArguments() != null) {
            String arg = getArguments().getString("categoryName", "General");
            if (arg != null && !arg.isBlank()) {
                categoryName = arg;
            }
        }

        TextView title = view.findViewById(R.id.tv_category_jobs_title);
        title.setText(categoryName);

        view.findViewById(R.id.btn_back_category_jobs).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        RecyclerView recyclerView = view.findViewById(R.id.rv_category_jobs);
        adapter = new JobsAdapter(new ArrayList<>(), new JobsAdapter.OnJobClickListener() {
            @Override
            public void onJobClick(Job job, int position) {
                Bundle args = new Bundle();
                args.putSerializable("job", job);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_categoryJobsFragment_to_jobDetailsFragment, args);
            }

            @Override
            public void onBookmarkClick(Job job, int position) {
                if (job == null) {
                    return;
                }
                boolean nextSaved = !job.isSaved();
                repository.toggleSave(job, nextSaved, new JobNetRepository.DataCallback<>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        if (!isAdded()) {
                            return;
                        }
                        job.setSaved(nextSaved);
                        adapter.notifyItemChanged(position);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (!isAdded()) {
                            return;
                        }
                        adapter.notifyItemChanged(position);
                    }
                });
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        loadCategoryJobs(view);
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
            loadCategoryJobs(root);
        }
    }

    private void loadCategoryJobs(View root) {
        showSkeleton(true);
        repository.loadHomeData(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(JobNetRepository.HomeData data) {
                if (!isAdded()) {
                    return;
                }
                List<Job> merged = new ArrayList<>();
                if (data != null && data.featured != null) {
                    merged.addAll(data.featured);
                }
                if (data != null && data.recommended != null) {
                    merged.addAll(data.recommended);
                }
                if (merged.isEmpty()) {
                    merged.addAll(SampleData.getAllJobs());
                }
                renderJobs(root, filterByCategory(merged));
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                renderJobs(root, filterByCategory(SampleData.getAllJobs()));
            }
        });
    }

    private void renderJobs(View root, List<Job> jobs) {
        showSkeleton(false);

        List<Job> data = jobs == null ? Collections.emptyList() : jobs;
        adapter.updateData(data);

        TextView count = root.findViewById(R.id.tv_category_jobs_count);
        count.setText(data.size() + " jobs found");

        View empty = root.findViewById(R.id.layout_category_jobs_empty);
        View list = root.findViewById(R.id.rv_category_jobs);
        boolean isEmpty = data.isEmpty();
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private List<Job> filterByCategory(List<Job> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        List<Job> filtered = new ArrayList<>();
        for (Job job : source) {
            if (job == null) {
                continue;
            }

            String actualCategory = safe(job.getCategory());
            if (actualCategory.isEmpty()) {
                actualCategory = inferCategory(job);
            }

            if (actualCategory.equalsIgnoreCase(categoryName)) {
                filtered.add(job);
            }
        }
        return filtered;
    }

    private String inferCategory(Job job) {
        String text = (safe(job.getTitle()) + " " + safe(job.getDescription())).toLowerCase(Locale.ROOT);
        if (containsAny(text, "design", "ui", "ux", "figma")) return "Design";
        if (containsAny(text, "engineer", "developer", "android", "backend", "frontend", "devops", "software")) return "Engineering";
        if (containsAny(text, "marketing", "growth", "seo", "content", "brand")) return "Marketing";
        if (containsAny(text, "finance", "account", "audit", "analyst")) return "Finance";
        if (containsAny(text, "hr", "human resources", "recruit", "talent")) return "HR";
        if (containsAny(text, "teacher", "education", "instructor", "curriculum")) return "Education";
        return "General";
    }

    private boolean containsAny(String text, String... terms) {
        if (text == null || text.isBlank() || terms == null) {
            return false;
        }
        for (String term : terms) {
            if (term != null && !term.isBlank() && text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private void applyInsets(View root) {
        View toolbar = root.findViewById(R.id.category_jobs_toolbar);
        final int toolbarTop = toolbar.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(toolbar.getPaddingLeft(), toolbarTop + bars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            return insets;
        });
    }

    private void showSkeleton(boolean show) {
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
            skeletonDialog.setContentView(R.layout.dialog_category_jobs_skeleton);
            skeletonDialog.setCancelable(false);
            if (skeletonDialog.getWindow() != null) {
                skeletonDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                skeletonDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        }

        if (!skeletonDialog.isShowing()) {
            skeletonDialog.show();
            View skeletonRoot = skeletonDialog.findViewById(R.id.skeleton_root_category_jobs);
            SkeletonShimmerHelper.start(skeletonRoot, skeletonAnimators);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public void onDestroyView() {
        showSkeleton(false);
        super.onDestroyView();
    }
}
