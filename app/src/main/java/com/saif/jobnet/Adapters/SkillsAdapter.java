package com.saif.jobnet.Adapters;

import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;
import com.saif.jobnet.R;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SkillsAdapter extends RecyclerView.Adapter<SkillsAdapter.ViewHolder> {
    private List<String> originalSkills, filteredSkills, selectedSkills;
    private Context context;
    private FlexboxLayout selectedSkillsContainer;
    private Drawable closeIcon;
    private RecyclerView suggestionsRecyclerView;

    public SkillsAdapter(Context context, List<String> skills, List<String> selected,
                         FlexboxLayout selectedSkillsContainer, Drawable closeIcon, RecyclerView suggestionsRecyclerView) {
        this.context = context;
        this.originalSkills = new ArrayList<>(skills);
        this.filteredSkills = new ArrayList<>(skills);
        this.selectedSkills = selected != null ? new ArrayList<>(selected) : new ArrayList<>();
        this.selectedSkillsContainer=selectedSkillsContainer;
        this.closeIcon=closeIcon;
        this.suggestionsRecyclerView=suggestionsRecyclerView;
    }

    public List<String> getSelectedSkills() { return selectedSkills; }

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
        holder.skillRadioButton.setText(skill);

        boolean isSelected = selectedSkills.contains(skill);

        // Toggle visibility
        holder.tvSkillName.setVisibility(isSelected ? View.GONE : VISIBLE);
        holder.skillRadioButton.setVisibility(isSelected ? VISIBLE : View.GONE);

        // Set background & close icon for selected skill
        if (isSelected) {
            holder.skillRadioButton.setChecked(true);
            holder.skillRadioButton.setBackgroundResource(R.drawable.gender_selected);
            holder.skillRadioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, closeIcon, null);
        } else {
            holder.skillRadioButton.setChecked(false);
            holder.skillRadioButton.setBackgroundResource(R.drawable.gender_unselected);
            holder.skillRadioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }

        // Handle click (select/deselect skill)
        holder.itemView.setOnClickListener(v -> {
            if (isSelected) {
                selectedSkills.remove(skill);
            } else {
                selectedSkills.add(skill);
            }
            suggestionsRecyclerView.setVisibility(View.GONE);
            notifyDataSetChanged();
//            updateSelectedSkillsUI();
        });

        // Handle click on RadioButton to remove skill
        holder.skillRadioButton.setOnClickListener(v -> {
            selectedSkills.remove(skill);
            notifyDataSetChanged();
            updateSelectedSkillsUI();
        });
    }


    @Override
    public int getItemCount() { return filteredSkills.size(); }

    public void filter(String query) {
        filteredSkills = query.isEmpty() ? new ArrayList<>(originalSkills) :
                originalSkills.stream().filter(s -> s.toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList());
        notifyDataSetChanged();
    }

    private void updateSelectedSkillsUI() {
        selectedSkillsContainer.removeAllViews();
        if (selectedSkills.isEmpty()) {
            selectedSkillsContainer.setVisibility(View.GONE);
        } else {
            selectedSkillsContainer.setVisibility(VISIBLE);
            for (String skill : selectedSkills) {
                View chipView = LayoutInflater.from(context).inflate(R.layout.skill_item, selectedSkillsContainer, false);
                RadioButton chip = chipView.findViewById(R.id.skillRadioButton);
                chip.setVisibility(VISIBLE);

                chip.setText(skill);
                chip.setChecked(true);
                chip.setBackgroundResource(R.drawable.gender_selected);

                chip.setOnClickListener(v -> {
                    selectedSkills.remove(skill);
                    notifyDataSetChanged();
                    updateSelectedSkillsUI();
                });

                selectedSkillsContainer.addView(chipView);
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSkillName;
        RadioButton skillRadioButton;

        ViewHolder(View itemView) {
            super(itemView);
            tvSkillName = itemView.findViewById(R.id.tvSkillName);
            skillRadioButton = itemView.findViewById(R.id.skillRadioButton);
        }
    }
}
