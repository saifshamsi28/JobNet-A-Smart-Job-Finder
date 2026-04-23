package com.jobnet.app.ui.recruiter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;

import java.util.List;

public class RecruiterJobAdapter extends RecyclerView.Adapter<RecruiterJobAdapter.JobViewHolder> {

    public interface OnRecruiterJobActionListener {
        void onJobClick(Job job, int position);

        void onViewApplicants(Job job, int position);

        void onEditJob(Job job, int position);
    }

    private final List<Job> jobs;
    private final OnRecruiterJobActionListener listener;

    public RecruiterJobAdapter(List<Job> jobs, OnRecruiterJobActionListener listener) {
        this.jobs = jobs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recruiter_job_card, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobs.get(position);
        holder.title.setText(defaultIfBlank(job.getTitle(), "Untitled Role"));
        holder.company.setText(defaultIfBlank(job.getCompany(), "Unknown Company"));
        holder.location.setText(defaultIfBlank(job.getLocation(), holder.itemView.getContext().getString(R.string.not_available)));
        holder.posted.setText(defaultIfBlank(job.getPostedDate(), holder.itemView.getContext().getString(R.string.not_available)));

        boolean active = isActiveJob(job);
        holder.status.setText(active ? "Active" : "Closed");
        holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), active ? R.color.success : R.color.text_secondary));
        holder.status.setBackgroundResource(active ? R.drawable.bg_tag_green : R.drawable.bg_tag_warning);

        int applicantsCount = Math.max(0, job.getApplicantsCount());
        holder.applicants.setText(holder.itemView.getContext().getString(R.string.applicants_count, applicantsCount));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onJobClick(job, holder.getAdapterPosition());
            }
        });
        holder.viewApplicants.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewApplicants(job, holder.getAdapterPosition());
            }
        });
        holder.editJob.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditJob(job, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return jobs.size();
    }

    private boolean isActiveJob(Job job) {
        String value = defaultIfBlank(job.getJobType(), "") + " " + defaultIfBlank(job.getWorkMode(), "") + " " + defaultIfBlank(job.getTitle(), "");
        String normalized = value.toLowerCase();
        return !(normalized.contains("closed") || normalized.contains("inactive") || normalized.contains("expired"));
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView company;
        TextView status;
        TextView location;
        TextView posted;
        TextView applicants;
        View viewApplicants;
        View editJob;

        JobViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_recruiter_job_title);
            company = itemView.findViewById(R.id.tv_recruiter_job_company);
            status = itemView.findViewById(R.id.tv_recruiter_job_status);
            location = itemView.findViewById(R.id.tv_recruiter_job_location);
            posted = itemView.findViewById(R.id.tv_recruiter_job_posted);
            applicants = itemView.findViewById(R.id.tv_recruiter_applicant_count);
            viewApplicants = itemView.findViewById(R.id.btn_view_applicants);
            editJob = itemView.findViewById(R.id.btn_edit_recruiter_job);
        }
    }
}