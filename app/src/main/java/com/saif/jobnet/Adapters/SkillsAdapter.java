package com.saif.jobnet.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.jobnet.R;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SkillsAdapter extends RecyclerView.Adapter<SkillsAdapter.ViewHolder> {
    private List<String> originalSkills, filteredSkills;
    private Context context;
    private OnSkillSelectedListener skillSelectedListener;

    public interface OnSkillSelectedListener {
        void onSkillSelected(String skill);
    }

    public SkillsAdapter(Context context, List<String> skills, OnSkillSelectedListener listener) {
        this.context = context;
        this.originalSkills = new ArrayList<>(skills);
        this.filteredSkills = new ArrayList<>(skills);
        this.skillSelectedListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.skill_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String skill = filteredSkills.get(position);
        holder.tvSkillName.setText(skill);

        holder.itemView.setOnClickListener(v -> {
            if (skillSelectedListener != null) {
                skillSelectedListener.onSkillSelected(skill);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredSkills.size();
    }

    public void filter(String query) {
        filteredSkills = query.isEmpty() ? new ArrayList<>(originalSkills) :
                originalSkills.stream().filter(s -> s.toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList());
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSkillName;
        ViewHolder(View itemView) {
            super(itemView);
            tvSkillName = itemView.findViewById(R.id.tvSkillName);
            tvSkillName.setVisibility(View.VISIBLE);
        }
    }
}
