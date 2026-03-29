package com.jobnet.app.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private RecyclerView rvResults;
    private EditText etSearch;
    private TextView tvResultsCount;
    private TextView chipAll, chipFullTime, chipPartTime, chipRemote, chipInternship;
    private JobsAdapter adapter;
    private final List<Job> allJobs = new ArrayList<>();
    private String activeFilter = "All Jobs";
    private JobNetRepository repository;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvResults      = view.findViewById(R.id.rv_search_results);
        etSearch       = view.findViewById(R.id.et_search);
        tvResultsCount = view.findViewById(R.id.tv_results_count);
        chipAll        = view.findViewById(R.id.chip_all);
        chipFullTime   = view.findViewById(R.id.chip_full_time);
        chipPartTime   = view.findViewById(R.id.chip_part_time);
        chipRemote     = view.findViewById(R.id.chip_remote);
        chipInternship = view.findViewById(R.id.chip_internship);

        repository = JobNetRepository.getInstance(requireContext());

        adapter = new JobsAdapter(new ArrayList<>(), new JobsAdapter.OnJobClickListener() {
            @Override
            public void onJobClick(Job job, int position) {
                Bundle args = new Bundle();
                args.putSerializable("job", job);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_search_to_jobDetails, args);
            }
            @Override
            public void onBookmarkClick(Job job, int position) {
                job.setSaved(!job.isSaved());
                adapter.notifyItemChanged(position);
            }
        });

        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResults.setAdapter(adapter);
        rvResults.setHasFixedSize(true);

        setupSearch();
        setupChips();
        loadInitialJobs();
    }

    private void loadInitialJobs() {
        repository.loadHomeData(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(JobNetRepository.HomeData data) {
                if (!isAdded()) {
                    return;
                }
                allJobs.clear();
                if (data != null) {
                    if (data.featured != null) {
                        allJobs.addAll(data.featured);
                    }
                    if (data.recommended != null) {
                        allJobs.addAll(data.recommended);
                    }
                }
                if (allJobs.isEmpty()) {
                    allJobs.addAll(SampleData.getAllJobs());
                }
                List<Job> filtered = localFilter(allJobs, "", activeFilter);
                adapter.updateData(filtered);
                updateResultsCount(filtered.size());
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                allJobs.clear();
                allJobs.addAll(SampleData.getAllJobs());
                List<Job> filtered = localFilter(allJobs, "", activeFilter);
                adapter.updateData(filtered);
                updateResultsCount(filtered.size());
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleSearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupChips() {
        View.OnClickListener chipClick = v -> {
            TextView clicked = (TextView) v;
            activeFilter = clicked.getText().toString();
            updateChipUI();
            scheduleSearch();
        };
        chipAll.setOnClickListener(chipClick);
        chipFullTime.setOnClickListener(chipClick);
        chipPartTime.setOnClickListener(chipClick);
        chipRemote.setOnClickListener(chipClick);
        chipInternship.setOnClickListener(chipClick);
    }

    private void updateChipUI() {
        TextView[] chips = {chipAll, chipFullTime, chipPartTime, chipRemote, chipInternship};
        for (TextView chip : chips) {
            boolean selected = chip.getText().toString().equals(activeFilter);
            chip.setBackgroundResource(selected ?
                    R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(requireContext().getColor(
                    selected ? R.color.white : R.color.text_secondary));
        }
    }

    private void filterJobs(String query) {
        String trimmed = query == null ? "" : query.trim();

        if (trimmed.isEmpty()) {
            List<Job> filtered = localFilter(allJobs.isEmpty() ? SampleData.getAllJobs() : allJobs, "", activeFilter);
            adapter.updateData(filtered);
            updateResultsCount(filtered.size());
            return;
        }

        repository.searchJobs(trimmed, activeFilter, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Job> data) {
                if (!isAdded()) {
                    return;
                }
                List<Job> jobs = data == null ? Collections.emptyList() : data;
                adapter.updateData(jobs);
                updateResultsCount(jobs.size());
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                List<Job> filtered = localFilter(allJobs.isEmpty() ? SampleData.getAllJobs() : allJobs, trimmed, activeFilter);
                adapter.updateData(filtered);
                updateResultsCount(filtered.size());
            }
        });
    }

    private void scheduleSearch() {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> filterJobs(etSearch.getText().toString());
        searchHandler.postDelayed(pendingSearch, 300);
    }

    private List<Job> localFilter(List<Job> source, String query, String filter) {
        List<Job> filtered = new ArrayList<>();
        String lowerQuery = query == null ? "" : query.toLowerCase(Locale.getDefault());

        for (Job job : source) {
            boolean matchesQuery = query.isEmpty()
                    || job.getTitle().toLowerCase().contains(lowerQuery)
                    || job.getCompany().toLowerCase().contains(lowerQuery)
                    || job.getLocation().toLowerCase().contains(lowerQuery);

                boolean matchesFilter = activeFilter.equals("All Jobs")
                    || job.getType().equalsIgnoreCase(activeFilter)
                    || (activeFilter.equalsIgnoreCase("Remote")
                    && job.getWorkMode().equalsIgnoreCase("Remote"));

            if (matchesQuery && matchesFilter) filtered.add(job);
        }
        return filtered;
    }

    private void updateResultsCount(int count) {
        tvResultsCount.setText(count + " Jobs Found");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
    }
}
