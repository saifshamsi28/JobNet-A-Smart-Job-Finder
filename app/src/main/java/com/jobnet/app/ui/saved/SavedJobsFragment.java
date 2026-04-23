package com.jobnet.app.ui.saved;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.ui.home.JobsAdapter;
import com.jobnet.app.util.SkeletonShimmerHelper;
import com.jobnet.app.util.SampleData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SavedJobsFragment extends Fragment {

    private enum SavedFilter {
        ALL,
        APPLIED
    }

    private RecyclerView rvSaved;
    private TextView tvCount;
    private TextView tabAll;
    private TextView tabApplied;
    private JobsAdapter adapter;
    private final List<Job> allSavedJobs = new ArrayList<>();
    private final List<Job> visibleJobs = new ArrayList<>();
    private final Set<String> appliedJobIds = new HashSet<>();
    private SavedFilter activeFilter = SavedFilter.ALL;
    private JobNetRepository repository;
    private Dialog skeletonDialog;
    private final List<ObjectAnimator> skeletonAnimators = new ArrayList<>();
    private boolean skipNextResumeRefresh = true;
    private int refreshRequestVersion = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvSaved = view.findViewById(R.id.rv_saved_jobs);
        tvCount = view.findViewById(R.id.tv_saved_count);
        tabAll = view.findViewById(R.id.tab_all);
        tabApplied = view.findViewById(R.id.tab_applied);
        repository = JobNetRepository.getInstance(requireContext());

        adapter = new JobsAdapter(visibleJobs, new JobsAdapter.OnJobClickListener() {
            @Override
            public void onJobClick(Job job, int position) {
                Bundle args = new Bundle();
                args.putSerializable("job", job);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_saved_to_jobDetails, args);
            }
            @Override
            public void onBookmarkClick(Job job, int position) {
                boolean wantToSave = !job.isSaved();
                repository.toggleSave(job, wantToSave, new JobNetRepository.DataCallback<>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        if (!isAdded()) {
                            return;
                        }
                        if (!wantToSave) {
                            removeSavedJob(job.getId());
                        } else {
                            job.setSaved(true);
                            if (!containsJob(allSavedJobs, job.getId())) {
                                allSavedJobs.add(job);
                            }
                        }
                        applyActiveFilterAndRender();
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

        rvSaved.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSaved.setAdapter(adapter);
        setupTabs();
        refreshSavedJobs();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        refreshSavedJobs();
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> setActiveFilter(SavedFilter.ALL));
        tabApplied.setOnClickListener(v -> setActiveFilter(SavedFilter.APPLIED));
        updateTabStyles();
    }

    private void setActiveFilter(SavedFilter filter) {
        if (activeFilter == filter) {
            return;
        }
        activeFilter = filter;
        updateTabStyles();
        applyActiveFilterAndRender();
    }

    private void updateTabStyles() {
        boolean allSelected = activeFilter == SavedFilter.ALL;

        tabAll.setBackgroundResource(allSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        tabApplied.setBackgroundResource(allSelected ? R.drawable.bg_chip_unselected : R.drawable.bg_chip_selected);

        tabAll.setTextColor(ContextCompat.getColor(requireContext(), allSelected ? R.color.white : R.color.text_secondary));
        tabApplied.setTextColor(ContextCompat.getColor(requireContext(), allSelected ? R.color.text_secondary : R.color.white));
    }

    private void refreshSavedJobs() {
        final int requestVersion = ++refreshRequestVersion;
        showSavedSkeleton(true);
        repository.loadSavedJobs(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Job> data) {
                if (!isActiveRefreshRequest(requestVersion)) {
                    return;
                }
                allSavedJobs.clear();
                if (data != null) {
                    for (Job job : data) {
                        if (job != null) {
                            job.setSaved(true);
                            allSavedJobs.add(job);
                        }
                    }
                }

                if (allSavedJobs.isEmpty()) {
                    for (Job job : SampleData.getAllJobs()) {
                        if (job.isSaved()) {
                            allSavedJobs.add(job);
                        }
                    }
                }
                fetchAppliedJobIdsAndRender(requestVersion);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isActiveRefreshRequest(requestVersion)) {
                    return;
                }
                fetchAppliedJobIdsAndRender(requestVersion);
            }
        });
    }

    private void fetchAppliedJobIdsAndRender(int requestVersion) {
        repository.loadMyApplications(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<ApplicationDto> data) {
                if (!isActiveRefreshRequest(requestVersion)) {
                    return;
                }
                appliedJobIds.clear();
                if (data != null) {
                    for (ApplicationDto application : data) {
                        if (application != null && application.jobId != null && !application.jobId.trim().isEmpty()) {
                            appliedJobIds.add(application.jobId.trim());
                        }
                    }
                }
                applyActiveFilterAndRender();
                showSavedSkeleton(false);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isActiveRefreshRequest(requestVersion)) {
                    return;
                }
                appliedJobIds.clear();
                applyActiveFilterAndRender();
                showSavedSkeleton(false);
            }
        });
    }

    private boolean isActiveRefreshRequest(int requestVersion) {
        return isAdded() && requestVersion == refreshRequestVersion;
    }

    private void applyActiveFilterAndRender() {
        visibleJobs.clear();
        if (activeFilter == SavedFilter.ALL) {
            visibleJobs.addAll(allSavedJobs);
        } else {
            for (Job job : allSavedJobs) {
                if (job != null && isApplied(job.getId())) {
                    visibleJobs.add(job);
                }
            }
        }

        adapter.updateData(new ArrayList<>(visibleJobs));
        updateCountLabel();
    }

    private void removeSavedJob(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            return;
        }
        for (int i = allSavedJobs.size() - 1; i >= 0; i--) {
            Job item = allSavedJobs.get(i);
            if (item != null && jobId.equals(item.getId())) {
                allSavedJobs.remove(i);
            }
        }
    }

    private boolean containsJob(List<Job> list, String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            return false;
        }
        for (Job job : list) {
            if (job != null && jobId.equals(job.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isApplied(String jobId) {
        return jobId != null && appliedJobIds.contains(jobId);
    }

    private void updateCountLabel() {
        if (activeFilter == SavedFilter.APPLIED) {
            tvCount.setText(visibleJobs.size() + " applied jobs");
            return;
        }
        tvCount.setText(allSavedJobs.size() + " jobs saved");
    }

    private void showSavedSkeleton(boolean show) {
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
            skeletonDialog.setContentView(R.layout.dialog_saved_skeleton);
            skeletonDialog.setCancelable(false);
            if (skeletonDialog.getWindow() != null) {
                skeletonDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                skeletonDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        }
        if (!skeletonDialog.isShowing()) {
            skeletonDialog.show();
            View root = skeletonDialog.findViewById(R.id.skeleton_root_saved);
            SkeletonShimmerHelper.start(root, skeletonAnimators);
        }
    }

    @Override
    public void onDestroyView() {
        refreshRequestVersion++;
        showSavedSkeleton(false);
        super.onDestroyView();
    }
}
