package com.saif.jobnet.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.jobnet.Models.Education;
import com.saif.jobnet.R;

import java.util.List;

public class EducationAdapter extends RecyclerView.Adapter<EducationAdapter.EducationViewHolder> {

    private List<Education> educationList;
    private Context context;
    private OnEditClickListener editClickListener;

    public interface OnEditClickListener {
        void onEditClick(Education education);
    }

    public EducationAdapter(List<Education> educationList, Context context, OnEditClickListener editClickListener) {
        this.educationList = educationList;
        this.context = context;
        this.editClickListener = editClickListener;
    }

    @NonNull
    @Override
    public EducationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.education_item, parent, false);
        return new EducationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EducationViewHolder holder, int position) {
        Education education = educationList.get(position);
        holder.tvEducationTitle.setText(education.getTitle());
        holder.tvCollegeName.setText(education.getCollege());
        holder.tvGraduationYear.setText(education.getGraduationYear());

//        holder.ivEditEducation.setOnClickListener(v -> editClickListener.onEditClick(education));
    }

    @Override
    public int getItemCount() {
        return educationList.size();
    }

    public static class EducationViewHolder extends RecyclerView.ViewHolder {
        TextView tvEducationTitle, tvCollegeName, tvGraduationYear;

        public EducationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEducationTitle = itemView.findViewById(R.id.tv_education_title);
            tvCollegeName = itemView.findViewById(R.id.tv_college_name);
            tvGraduationYear = itemView.findViewById(R.id.tv_graduation_year);
        }
    }
}
