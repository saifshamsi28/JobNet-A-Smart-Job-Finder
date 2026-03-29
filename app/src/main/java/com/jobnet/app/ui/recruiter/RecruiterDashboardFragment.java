package com.jobnet.app.ui.recruiter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.ui.home.JobsAdapter;

import java.util.ArrayList;
import java.util.List;

public class RecruiterDashboardFragment extends Fragment {

    private JobNetRepository repository;
    private JobsAdapter adapter;
    private final List<Job> postedJobs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recruiter_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.recycler_recruiter_jobs);
        ProgressBar progressBar = view.findViewById(R.id.recruiter_jobs_progress);
        TextView emptyView = view.findViewById(R.id.tv_recruiter_empty);
        MaterialButton createJobButton = view.findViewById(R.id.btn_create_job);

        adapter = new JobsAdapter(postedJobs, new JobsAdapter.OnJobClickListener() {
            @Override
            public void onJobClick(Job job, int position) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("job", job);
                Navigation.findNavController(view)
                        .navigate(R.id.action_recruiterDashboardFragment_to_jobDetailsFragment, bundle);
            }

            @Override
            public void onBookmarkClick(Job job, int position) {
                // Recruiter board does not use bookmark actions.
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        createJobButton.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_recruiterDashboardFragment_to_recruiterPostJobFragment));

        loadJobs(progressBar, emptyView);
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view == null || repository == null) {
            return;
        }
        loadJobs(view.findViewById(R.id.recruiter_jobs_progress), view.findViewById(R.id.tv_recruiter_empty));
    }

    private void loadJobs(ProgressBar progressBar, TextView emptyView) {
        progressBar.setVisibility(View.VISIBLE);
        repository.loadRecruiterPostedJobs(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Job> data) {
                if (!isAdded()) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                postedJobs.clear();
                if (data != null) {
                    postedJobs.addAll(data);
                }
                adapter.notifyDataSetChanged();
                emptyView.setVisibility(postedJobs.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                emptyView.setVisibility(postedJobs.isEmpty() ? View.VISIBLE : View.GONE);
                Toast.makeText(requireContext(), R.string.create_job_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
