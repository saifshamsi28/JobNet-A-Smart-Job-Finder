package com.jobnet.app.ui.recruiter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.util.RecruiterJobStatusUtils;

import java.util.List;

/**
 * RecyclerView adapter for the recruiter's posted jobs list.
 * Each card shows the job title/meta and exposes
 * "View Applicants", "Edit" and "Delete" action buttons.
 */
public class RecruiterJobsAdapter extends RecyclerView.Adapter<RecruiterJobsAdapter.VH> {

    private static final int VIEW_TYPE_SKELETON = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int SKELETON_COUNT = 4;

    public interface OnJobActionListener {
        void onViewApplicants(Job job, int position);
        void onEditJob(Job job, int position);
        void onDeleteJob(Job job, int position);
        void onJobCardClick(Job job, int position);
    }

    private final List<Job> items;
    private final OnJobActionListener listener;
    private boolean showSkeleton;

    public RecruiterJobsAdapter(List<Job> items, OnJobActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void showSkeleton(boolean show) {
        showSkeleton = show;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return showSkeleton ? VIEW_TYPE_SKELETON : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = viewType == VIEW_TYPE_SKELETON
            ? R.layout.item_recruiter_job_skeleton
            : R.layout.item_recruiter_job_card;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (showSkeleton) {
            startShimmer(holder.itemView);
            return;
        }

        Job job = items.get(position);

        holder.tvTitle.setText(job.getTitle() != null ? job.getTitle() : "Untitled Role");
        holder.tvCompany.setText(job.getCompany() != null ? job.getCompany() : "");
        holder.tvLocation.setText(job.getLocation() != null ? job.getLocation() : "");
        holder.tvPosted.setText((job.getPostedDate() == null || job.getPostedDate().isBlank())
                ? "Recently"
                : job.getPostedDate());

        RecruiterJobStatusUtils.Bucket bucket = RecruiterJobStatusUtils.resolveBucket(job);
        if (bucket == RecruiterJobStatusUtils.Bucket.CLOSED) {
            holder.tvStatus.setText("Closed");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_rejected_text));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_tag_error);
        } else if (bucket == RecruiterJobStatusUtils.Bucket.DRAFT) {
            holder.tvStatus.setText("Draft");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.primary));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_tag_primary);
        } else {
            holder.tvStatus.setText("Active");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.success));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_tag_green);
        }

        int count = job.getApplicantsCount();
        boolean canViewApplicants = bucket != RecruiterJobStatusUtils.Bucket.DRAFT;
        if (canViewApplicants) {
            holder.tvApplicantsCount.setText(count + " Applicant" + (count == 1 ? "" : "s"));
        } else {
            holder.tvApplicantsCount.setText("Draft");
        }
        holder.btnViewApplicants.setEnabled(canViewApplicants);
        holder.btnViewApplicants.setClickable(canViewApplicants);
        holder.btnViewApplicants.setAlpha(canViewApplicants ? 1f : 0.6f);

        holder.btnViewApplicants.setOnClickListener(v -> {
            if (!canViewApplicants) {
                return;
            }
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onViewApplicants(items.get(pos), pos);
            }
        });

        holder.btnEditJob.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onEditJob(items.get(pos), pos);
            }
        });

        holder.btnDeleteJob.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onDeleteJob(items.get(pos), pos);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onJobCardClick(items.get(pos), pos);
            }
        });

        holder.itemView.setAlpha(0f);
        holder.itemView.animate().alpha(1f).setDuration(250).setStartDelay(position * 40L).start();
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        Object tag = holder.itemView.getTag(R.id.tv_recruiter_job_title);
        if (tag instanceof android.animation.ObjectAnimator) {
            ((android.animation.ObjectAnimator) tag).cancel();
            holder.itemView.setTag(R.id.tv_recruiter_job_title, null);
        }
    }

    @Override
    public int getItemCount() {
        return showSkeleton ? SKELETON_COUNT : items.size();
    }

    private void startShimmer(View view) {
        Object existing = view.getTag(R.id.tv_recruiter_job_title);
        if (existing instanceof android.animation.ObjectAnimator) {
            return;
        }
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(view, "alpha", 0.4f, 1f);
        animator.setDuration(900);
        animator.setRepeatMode(android.animation.ObjectAnimator.REVERSE);
        animator.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        animator.start();
        view.setTag(R.id.tv_recruiter_job_title, animator);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvCompany;
        final TextView tvLocation;
        final TextView tvPosted;
        final TextView tvStatus;
        final TextView tvApplicantsCount;
        final View btnViewApplicants;
        final View btnEditJob;
        final View btnDeleteJob;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_recruiter_job_title);
            tvCompany = itemView.findViewById(R.id.tv_recruiter_job_company);
            tvLocation = itemView.findViewById(R.id.tv_recruiter_job_location);
            tvPosted = itemView.findViewById(R.id.tv_recruiter_job_posted);
            tvStatus = itemView.findViewById(R.id.tv_recruiter_job_status);
            tvApplicantsCount = itemView.findViewById(R.id.tv_recruiter_applicant_count);
            btnViewApplicants = itemView.findViewById(R.id.btn_view_applicants);
            btnEditJob = itemView.findViewById(R.id.btn_edit_recruiter_job);
            btnDeleteJob = itemView.findViewById(R.id.btn_delete_recruiter_job);
        }
    }
}
