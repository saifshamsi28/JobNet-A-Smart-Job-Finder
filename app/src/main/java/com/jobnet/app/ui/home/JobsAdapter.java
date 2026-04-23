package com.jobnet.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.util.SalaryUtils;
import java.util.List;

public class JobsAdapter extends RecyclerView.Adapter<JobsAdapter.JobViewHolder> {

    private List<Job> jobs;
    private OnJobClickListener listener;
    private int lastAnimatedPosition = -1;

    public interface OnJobClickListener {
        void onJobClick(Job job, int position);
        void onBookmarkClick(Job job, int position);
    }

    public JobsAdapter(List<Job> jobs, OnJobClickListener listener) {
        this.jobs = jobs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_job_card, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobs.get(position);
        holder.tvTitle.setText(job.getTitle());
        holder.tvCompany.setText(job.getCompany());
        holder.tvLocation.setText(job.getLocation());
        holder.tvSalary.setText(SalaryUtils.normalizeDisplay(job.getSalary()));
        holder.tvType.setText(job.getType());
        holder.tvWorkMode.setText(job.getWorkMode());
        holder.ivBookmark.setImageResource(
                job.isSaved() ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark
        );
        ImageViewCompat.setImageTintList(holder.ivBookmark, null);
        if (!job.isSaved()) {
            holder.ivBookmark.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.bookmark_unsaved));
            holder.ivBookmark.setAlpha(1.0f);
        } else {
            holder.ivBookmark.clearColorFilter();
            holder.ivBookmark.setAlpha(1.0f);
        }

        // Company logo placeholder color
        holder.ivCompanyLogo.setBackgroundResource(
                position % 3 == 0 ? R.drawable.bg_icon_primary :
                position % 3 == 1 ? R.drawable.bg_tag_green : R.drawable.bg_tag_primary
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onJobClick(job, holder.getAdapterPosition());
        });
        holder.ivBookmark.setOnClickListener(v -> {
            if (listener != null) listener.onBookmarkClick(job, holder.getAdapterPosition());
        });

        animateEntry(holder.itemView, position);
    }

    @Override
    public int getItemCount() { return jobs.size(); }

    public void updateData(List<Job> newJobs) {
        this.jobs = newJobs;
        this.lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    private void animateEntry(View itemView, int position) {
        if (position <= lastAnimatedPosition) {
            itemView.setAlpha(1f);
            itemView.setTranslationY(0f);
            return;
        }
        itemView.animate().cancel();
        itemView.setAlpha(0f);
        itemView.setTranslationY(16f);
        itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260L)
                .setStartDelay((long) Math.min(position, 6) * 42L)
                .start();
        lastAnimatedPosition = position;
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCompanyLogo, ivBookmark;
        TextView tvTitle, tvCompany, tvLocation, tvSalary, tvType, tvWorkMode;

        JobViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCompanyLogo = itemView.findViewById(R.id.iv_company_logo);
            ivBookmark    = itemView.findViewById(R.id.iv_bookmark);
            tvTitle       = itemView.findViewById(R.id.tv_job_title);
            tvCompany     = itemView.findViewById(R.id.tv_company_name);
            tvLocation    = itemView.findViewById(R.id.tv_location);
            tvSalary      = itemView.findViewById(R.id.tv_salary);
            tvType        = itemView.findViewById(R.id.tv_job_type);
            tvWorkMode    = itemView.findViewById(R.id.tv_work_mode);
        }
    }
}
