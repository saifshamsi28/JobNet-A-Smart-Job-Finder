package com.saif.jobnet.Activities;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.saif.jobnet.Models.Job;
import com.saif.jobnet.R;
import com.saif.jobnet.Activities.placeholder.PlaceholderContent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class SavedJobsFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private List<Job> savedJobsList = new ArrayList<>();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SavedJobsFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
//    public static SavedJobsFragment newInstance(int columnCount) {
//        Bundle bundle = new Bundle();
//        bundle.putSerializable("savedJobs", (Serializable) user.getSavedJobs());
//        SavedJobsFragment savedJobsFragment = SavedJobsFragment.newInstance(1);
//        savedJobsFragment.setArguments(bundle);
//
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.fragment_container, savedJobsFragment)
//                .commit();
//
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the saved jobs list from arguments
        if (getArguments() != null) {
            savedJobsList = (List<Job>) getArguments().getSerializable("savedJobs");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_jobs_list, container, false);

        // Set up RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.saved_jobs_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Check if savedJobsList is not null
        if (savedJobsList != null) {
            recyclerView.setAdapter(new SavedJobsAdapter(getContext(),savedJobsList));
        }

        return view;
    }

}