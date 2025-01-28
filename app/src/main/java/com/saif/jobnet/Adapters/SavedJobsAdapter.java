package com.saif.jobnet.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.jobnet.R;
import com.saif.jobnet.databinding.JobCardBinding;
import com.saif.jobnet.databinding.SavedJobsLayoutBinding;

public class SavedJobsAdapter extends RecyclerView.Adapter<SavedJobsAdapter.JobViewHolder>{
    private Context context;

    public SavedJobsAdapter() {
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.saved_jobs_layout,parent,false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        holder.binding.jobTitle.setText("Job Title");
        holder.binding.companyName.setText("Company Name");
        holder.binding.location.setText("Location");
        holder.binding.salary.setText("Salary");
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {

        SavedJobsLayoutBinding binding;
        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            binding=SavedJobsLayoutBinding.bind(itemView);
        }
    }
}
