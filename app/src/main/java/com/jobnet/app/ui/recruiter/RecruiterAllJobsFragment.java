package com.jobnet.app.ui.recruiter;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.util.RecruiterJobStatusUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecruiterAllJobsFragment extends Fragment {

    private enum StatusFilter {
        ALL,
        ACTIVE,
        CLOSED,
        DRAFT
    }

    private JobNetRepository repository;
    private RecruiterJobsAdapter adapter;
    private final List<Job> allJobs = new ArrayList<>();
    private final List<Job> visibleJobs = new ArrayList<>();
    private String searchQuery = "";
    private StatusFilter activeFilter = StatusFilter.ALL;

    private ProgressBar progressBar;
    private View emptyLayout;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private TextView chipAll;
    private TextView chipActive;
    private TextView chipClosed;
    private TextView chipDraft;
    private boolean skipNextResumeRefresh = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recruiter_all_jobs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());

        progressBar = view.findViewById(R.id.progress_recruiter_all_jobs);
        emptyLayout = view.findViewById(R.id.layout_recruiter_jobs_empty);
        emptyTitle = view.findViewById(R.id.tv_recruiter_jobs_empty_title);
        emptyMessage = view.findViewById(R.id.tv_recruiter_jobs_empty_message);
        chipAll = view.findViewById(R.id.chip_recruiter_jobs_all);
        chipActive = view.findViewById(R.id.chip_recruiter_jobs_active);
        chipClosed = view.findViewById(R.id.chip_recruiter_jobs_closed);
        chipDraft = view.findViewById(R.id.chip_recruiter_jobs_draft);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_recruiter_all_jobs);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RecruiterJobsAdapter(visibleJobs, new RecruiterJobsAdapter.OnJobActionListener() {
            @Override
            public void onViewApplicants(Job job, int position) {
                Bundle bundle = new Bundle();
                bundle.putString("jobId", job.getId());
                bundle.putString("jobTitle", job.getTitle());
                Navigation.findNavController(view)
                        .navigate(R.id.recruiterApplicantsFragment, bundle);
            }

            @Override
            public void onEditJob(Job job, int position) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("job", job);
                Navigation.findNavController(view)
                        .navigate(R.id.recruiterEditJobFragment, bundle);
            }

            @Override
            public void onDeleteJob(Job job, int position) {
                showDeleteJobDialog(job);
            }

            @Override
            public void onJobCardClick(Job job, int position) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("job", job);
                Navigation.findNavController(view)
                        .navigate(R.id.jobDetailsFragment, bundle);
            }
        });
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btn_back_recruiter_jobs).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        setupSearch(view.findViewById(R.id.et_recruiter_jobs_search));
        setupFilters();
        loadJobs(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        loadJobs(true);
    }

    private void setupSearch(EditText searchInput) {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                searchQuery = editable == null ? "" : editable.toString().trim();
                applyFilters();
            }
        });
    }

    private void setupFilters() {
        chipAll.setOnClickListener(v -> {
            activeFilter = StatusFilter.ALL;
            refreshChips();
            applyFilters();
        });
        chipActive.setOnClickListener(v -> {
            activeFilter = StatusFilter.ACTIVE;
            refreshChips();
            applyFilters();
        });
        chipClosed.setOnClickListener(v -> {
            activeFilter = StatusFilter.CLOSED;
            refreshChips();
            applyFilters();
        });
        chipDraft.setOnClickListener(v -> {
            activeFilter = StatusFilter.DRAFT;
            refreshChips();
            applyFilters();
        });
        refreshChips();
        updateChipCounts();
    }

    private void loadJobs(boolean showLoader) {
        if (progressBar == null) {
            return;
        }
        progressBar.setVisibility(View.GONE);
        if (adapter != null) {
            adapter.showSkeleton(showLoader);
        }
        if (emptyLayout != null) {
            emptyLayout.setVisibility(View.GONE);
        }
        repository.loadRecruiterPostedJobs(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Job> data) {
                if (!isAdded()) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                if (adapter != null) {
                    adapter.showSkeleton(false);
                }
                allJobs.clear();
                if (data != null) {
                    allJobs.addAll(data);
                }
                updateChipCounts();
                applyFilters();
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                if (adapter != null) {
                    adapter.showSkeleton(false);
                }
                updateChipCounts();
                applyFilters();
                if (allJobs.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.recruiter_jobs_load_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyFilters() {
        visibleJobs.clear();
        String query = searchQuery.toLowerCase(Locale.ROOT);

        for (Job job : allJobs) {
            if (job == null) {
                continue;
            }
            if (!matchesStatus(job) || !matchesSearch(job, query)) {
                continue;
            }
            visibleJobs.add(job);
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private boolean matchesStatus(Job job) {
        RecruiterJobStatusUtils.Bucket bucket = RecruiterJobStatusUtils.resolveBucket(job);
        if (activeFilter == StatusFilter.ALL) {
            return true;
        }
        if (activeFilter == StatusFilter.ACTIVE) {
            return bucket == RecruiterJobStatusUtils.Bucket.ACTIVE;
        }
        if (activeFilter == StatusFilter.CLOSED) {
            return bucket == RecruiterJobStatusUtils.Bucket.CLOSED;
        }
        return bucket == RecruiterJobStatusUtils.Bucket.DRAFT;
    }

    private boolean matchesSearch(Job job, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String title = safe(job.getTitle()).toLowerCase(Locale.ROOT);
        String company = safe(job.getCompany()).toLowerCase(Locale.ROOT);
        String location = safe(job.getLocation()).toLowerCase(Locale.ROOT);
        String category = safe(job.getCategory()).toLowerCase(Locale.ROOT);
        return title.contains(query)
                || company.contains(query)
                || location.contains(query)
                || category.contains(query);
    }

    private void updateChipCounts() {
        int active = 0;
        int closed = 0;
        int draft = 0;
        for (Job job : allJobs) {
            RecruiterJobStatusUtils.Bucket bucket = RecruiterJobStatusUtils.resolveBucket(job);
            if (bucket == RecruiterJobStatusUtils.Bucket.ACTIVE) {
                active++;
            } else if (bucket == RecruiterJobStatusUtils.Bucket.CLOSED) {
                closed++;
            } else {
                draft++;
            }
        }
        chipAll.setText(getString(R.string.recruiter_filter_all_count, allJobs.size()));
        chipActive.setText(getString(R.string.recruiter_filter_active_count, active));
        chipClosed.setText(getString(R.string.recruiter_filter_closed_count, closed));
        chipDraft.setText(getString(R.string.recruiter_filter_draft_count, draft));
    }

    private void refreshChips() {
        applyChipState(chipAll, activeFilter == StatusFilter.ALL);
        applyChipState(chipActive, activeFilter == StatusFilter.ACTIVE);
        applyChipState(chipClosed, activeFilter == StatusFilter.CLOSED);
        applyChipState(chipDraft, activeFilter == StatusFilter.DRAFT);
    }

    private void applyChipState(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(requireContext().getColor(selected ? R.color.text_chip_selected : R.color.text_chip));
    }

    private void updateEmptyState() {
        if (emptyLayout == null) {
            return;
        }
        if (!allJobs.isEmpty() && visibleJobs.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            emptyTitle.setText(R.string.recruiter_jobs_empty_filtered_title);
            emptyMessage.setText(R.string.recruiter_jobs_empty_filtered_message);
            return;
        }
        if (allJobs.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            emptyTitle.setText(R.string.recruiter_jobs_empty_all_title);
            emptyMessage.setText(R.string.recruiter_jobs_empty_all_message);
            return;
        }
        emptyLayout.setVisibility(View.GONE);
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
                loadJobs(false);
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
