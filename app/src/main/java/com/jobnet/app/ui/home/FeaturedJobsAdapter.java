package com.jobnet.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import java.util.List;

public class FeaturedJobsAdapter extends RecyclerView.Adapter<FeaturedJobsAdapter.FeaturedViewHolder> {

    private List<Job> jobs;
    private OnFeaturedJobClickListener listener;

    public interface OnFeaturedJobClickListener {
        void onFeaturedJobClick(Job job, int position);
    }

    public FeaturedJobsAdapter(List<Job> jobs, OnFeaturedJobClickListener listener) {
        this.jobs = jobs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeaturedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_featured_card, parent, false);
        return new FeaturedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeaturedViewHolder holder, int position) {
        Job job = jobs.get(position);
        holder.tvTitle.setText(job.getTitle());
        holder.tvCompany.setText(job.getCompany());
        holder.tvLocation.setText(job.getWorkMode());
        holder.tvSalary.setText(job.getSalary());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFeaturedJobClick(job, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return Math.min(jobs.size(), 5); }

    static class FeaturedViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCompany, tvLocation, tvSalary;

        FeaturedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tv_featured_title);
            tvCompany  = itemView.findViewById(R.id.tv_featured_company);
            tvLocation = itemView.findViewById(R.id.tv_featured_type);
            tvSalary   = itemView.findViewById(R.id.tv_featured_salary);
        }
    }
}
