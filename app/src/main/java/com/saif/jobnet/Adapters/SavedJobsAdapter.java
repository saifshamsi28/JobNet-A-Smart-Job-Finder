package com.saif.jobnet.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.jobnet.Activities.JobDetailActivity;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.JobCardBinding;
import com.saif.jobnet.databinding.SavedJobsLayoutBinding;

import java.util.List;

public class SavedJobsAdapter extends RecyclerView.Adapter<SavedJobsAdapter.JobViewHolder>{
    private Context context;
    private List<Job> savedJobs;

    public SavedJobsAdapter(Context context,List<Job> savedJobs) {
        this.savedJobs = savedJobs;
        System.out.println("in saved jobs adapter constructor, saved jobs size: "+savedJobs.size());
        this.context = context;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.saved_jobs_layout,parent,false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job=savedJobs.get(position);
        holder.binding.jobTitle.setText(job.getTitle());
        holder.binding.companyName.setText(job.getCompany());
        holder.binding.location.setText(job.getLocation());
        holder.binding.salary.setText(job.getSalary());
        String numericReview = job.getReview().replaceAll("[^\\d]", "");
        if(numericReview.isEmpty()){
            holder.binding.jobRating.setVisibility(View.GONE);
            holder.binding.ratingImg.setVisibility(View.GONE);
        }else {
            holder.binding.jobRating.setVisibility(View.VISIBLE);
            holder.binding.jobRating.setText(job.getRating());
            holder.binding.ratingImg.setVisibility(View.VISIBLE);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, JobDetailActivity.class);
                intent.putExtra("jobId", job.getJobId());
                intent.putExtra("url", job.getUrl());
                System.out.println("url to visit: "+ job.getUrl());
                context.startActivity(intent);
            }
        });
//        holder.binding.shortDescription.setText(job.getShortDescription());
//        holder.binding.postDate.setText(job.getPostDate());
    }

    @Override
    public int getItemCount() {
        return savedJobs.size();
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {

        SavedJobsLayoutBinding binding;
        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            binding=SavedJobsLayoutBinding.bind(itemView);
        }
    }
}
