package com.jobnet.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jobnet.app.R;
import com.jobnet.app.data.model.JobCategory;
import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {

    private List<JobCategory> categories;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(JobCategory category, int position);
    }

    public CategoriesAdapter(List<JobCategory> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_card, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        JobCategory category = categories.get(position);
        holder.tvName.setText(category.getName());
        if (holder.tvCount != null) {
            holder.tvCount.setText(category.getJobCount() + "+ Jobs");
        }
        holder.ivIcon.setImageResource(category.getIconRes());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(category, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return categories.size(); }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvCount;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon  = itemView.findViewById(R.id.iv_category_icon);
            tvName  = itemView.findViewById(R.id.tv_category_name);
            tvCount = null;
        }
    }
}
