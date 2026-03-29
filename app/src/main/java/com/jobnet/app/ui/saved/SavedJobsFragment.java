package com.jobnet.app.ui.saved;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.ui.home.JobsAdapter;
import com.jobnet.app.util.SampleData;

import java.util.ArrayList;
import java.util.List;

public class SavedJobsFragment extends Fragment {

    private RecyclerView rvSaved;
    private TextView tvCount;
    private JobsAdapter adapter;
    private final List<Job> savedJobs = new ArrayList<>();
    private JobNetRepository repository;

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
        repository = JobNetRepository.getInstance(requireContext());

        adapter = new JobsAdapter(savedJobs, new JobsAdapter.OnJobClickListener() {
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
                            if (position >= 0 && position < savedJobs.size()) {
                                savedJobs.remove(position);
                                adapter.updateData(new ArrayList<>(savedJobs));
                            }
                        } else {
                            job.setSaved(true);
                            adapter.notifyItemChanged(position);
                        }
                        tvCount.setText(countSaved() + " jobs saved");
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
        loadSavedJobs();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedJobs();
    }

    private void loadSavedJobs() {
        repository.loadSavedJobs(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Job> data) {
                if (!isAdded()) {
                    return;
                }
                savedJobs.clear();
                if (data != null) {
                    for (Job job : data) {
                        if (job != null) {
                            job.setSaved(true);
                            savedJobs.add(job);
                        }
                    }
                }

                if (savedJobs.isEmpty()) {
                    for (Job job : SampleData.getAllJobs()) {
                        if (job.isSaved()) {
                            savedJobs.add(job);
                        }
                    }
                }

                adapter.updateData(new ArrayList<>(savedJobs));
                tvCount.setText(savedJobs.size() + " jobs saved");
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                tvCount.setText(countSaved() + " jobs saved");
            }
        });
    }

    private int countSaved() {
        int count = 0;
        for (Job job : savedJobs) {
            if (job.isSaved()) {
                count++;
            }
        }
        return count;
    }
}
