package com.saif.jobnet.Activities;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.jobnet.Models.Job;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.JobCardBinding;

import java.util.List;

import com.saif.jobnet.Activities.JobDetailActivity;

public class SavedJobsAdapter extends RecyclerView.Adapter<SavedJobsAdapter.SavedJobViewHolder> {

    private final List<Job> savedJobsList;
    private final Context context;

    public SavedJobsAdapter(Context context, List<Job> savedJobsList) {
        this.context = context;
        this.savedJobsList = savedJobsList;
    }

    @NonNull
    @Override
    public SavedJobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.job_card, parent, false);
        return new SavedJobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedJobViewHolder holder, int position) {
        Job job = savedJobsList.get(position);

        // Set job details
        holder.binding.jobTitle.setText(job.getTitle());
        holder.binding.companyName.setText(job.getCompany());
        holder.binding.location.setText(job.getLocation());
        holder.binding.salary.setText(job.getSalary());
        holder.binding.shortDescription.setText(job.getShortDescription());
        holder.binding.postDate.setText(job.getPostDate());

        // Hide "Save Job" icon for saved jobs
//        holder.binding.saveJobs.setImageResource(R.drawable.job_saved_icon);
//        holder.binding.saveJobs.setOnClickListener(v -> unsaveJob(job.getJobId(), holder.binding.saveJobs));

        // Navigate to JobDetailActivity on item click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, JobDetailActivity.class);
            intent.putExtra("jobId", job.getJobId());
            intent.putExtra("url", job.getUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return savedJobsList.size();
    }

    private void unsaveJob(String jobId, ImageView saveJobs) {
        // Logic to remove the job from saved jobs (API call, local DB update, etc.)
        Toast.makeText(context, "Job removed from saved jobs", Toast.LENGTH_SHORT).show();
    }

    static class SavedJobViewHolder extends RecyclerView.ViewHolder {

        JobCardBinding binding;

        public SavedJobViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = JobCardBinding.bind(itemView);
        }
    }
}
