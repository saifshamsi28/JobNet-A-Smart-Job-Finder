package com.saif.jobnet;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.jobnet.Models.Job;
import com.saif.jobnet.databinding.JobCardBinding;

import java.util.List;

import com.saif.jobnet.Activities.JobDetailActivity;

public class JobsAdapter extends RecyclerView.Adapter<JobsAdapter.JobViewHolder> {

    private final List<Job> jobList;
    private final Context context;

    public JobsAdapter(Context context, List<Job> jobList) {
        this.context = context;
        this.jobList = jobList;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.job_card, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobList.get(position);
        holder.binding.jobTitle.setText(job.getTitle());
        holder.binding.companyName.setText(job.getCompany());
        holder.binding.location.setText(job.getLocation());
        String numericReview = job.getReview().replaceAll("[^\\d]", "");
        if(numericReview.isEmpty()){
            holder.binding.jobRating.setVisibility(View.GONE);
            holder.binding.ratingImg.setVisibility(View.GONE);
        }else {
            holder.binding.jobRating.setVisibility(View.VISIBLE);
            holder.binding.jobRating.setText(job.getRating());
            holder.binding.ratingImg.setVisibility(View.VISIBLE);
        }

        holder.binding.salary.setText(job.getSalary());
        holder.binding.description.setText(job.getDescription());
        holder.binding.postDate.setText(job.getPostDate());
        setJobReviews(holder.binding, job.getReview());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //to go to job detail activity
                Intent intent = new Intent(context, JobDetailActivity.class);
                intent.putExtra("url", job.getUrl());
                context.startActivity(intent);
            }});
    }

    @Override
    public int getItemCount() {
        return jobList.size();
    }

    private void setJobReviews(JobCardBinding binding, String review) {
        if (review != null) {
            // Use regex to extract only numbers from the review string
            String numericReview = review.replaceAll("[^\\d]", ""); // Remove all non-digit characters

            if (!numericReview.isEmpty()) {
                numericReview=numericReview.trim()+" Reviews";
                // Set only the numerical part if it exists
                binding.reviews.setVisibility(View.VISIBLE);
                binding.reviews.setText(numericReview);
            } else {
                // Optionally, set an empty string or some placeholder if no numerical value is present
                binding.reviews.setVisibility(View.GONE);
            }
        }
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {

        JobCardBinding binding;
        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = JobCardBinding.bind(itemView);
        }
    }
}
