package com.jobnet.app.ui.recruiter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jobnet.app.R;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.util.DateTimeUtils;

import java.util.List;
import java.util.Locale;

public class RecruiterApplicantsAdapter extends RecyclerView.Adapter<RecruiterApplicantsAdapter.VH> {

    private static final int VIEW_TYPE_SKELETON = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int SKELETON_COUNT = 4;

    public interface OnActionListener {
        void onShortlist(ApplicationDto application, int position);
        void onReject(ApplicationDto application, int position);
    }

    private final List<ApplicationDto> items;
    private final OnActionListener listener;
    private boolean showSkeleton;

    public RecruiterApplicantsAdapter(List<ApplicationDto> items, OnActionListener listener) {
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
            ? R.layout.item_recruiter_applicant_skeleton
            : R.layout.item_recruiter_applicant;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (showSkeleton) {
            startShimmer(holder.itemView);
            return;
        }

        ApplicationDto app = items.get(position);
        Context ctx = holder.itemView.getContext();

        String displayName = resolveDisplayName(app);
        holder.tvName.setText(displayName);

        String initial = displayName.substring(0, 1).toUpperCase(Locale.ROOT);
        holder.tvInitial.setText(initial);

        String date = DateTimeUtils.formatDateTime(app.appliedAt, app.updatedAt);
        if (date.isBlank()) {
            date = "Recently";
        }
        holder.tvAppliedDate.setText("Applied " + date);

        String status = safe(app.status).isEmpty() ? "APPLIED" : app.status.toUpperCase(Locale.ROOT);
        holder.tvStatus.setText(status.replace('_', ' '));
        applyStatusStyle(ctx, holder.tvStatus, status);

        // Hide action buttons if already actioned
        boolean canAction = !status.equals("REJECTED")
            && !status.equals("OFFERED")
            && !status.equals("SHORTLISTED");
        holder.btnShortlist.setVisibility(canAction ? View.VISIBLE : View.GONE);
        holder.btnReject.setVisibility(canAction ? View.VISIBLE : View.GONE);

        holder.btnShortlist.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onShortlist(items.get(pos), pos);
            }
        });
        holder.btnReject.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onReject(items.get(pos), pos);
            }
        });

        holder.itemView.setAlpha(0f);
        holder.itemView.animate().alpha(1f).setDuration(250).setStartDelay(position * 40L).start();
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        Object tag = holder.itemView.getTag(R.id.tv_applicant_name);
        if (tag instanceof android.animation.ObjectAnimator) {
            ((android.animation.ObjectAnimator) tag).cancel();
            holder.itemView.setTag(R.id.tv_applicant_name, null);
        }
    }

    private void applyStatusStyle(Context ctx, TextView tv, String status) {
        switch (status) {
            case "SHORTLISTED":
                tv.setTextColor(ctx.getColor(R.color.status_shortlisted_text));
                tv.setBackgroundResource(R.drawable.bg_tag_warning);
                break;
            case "REJECTED":
                tv.setTextColor(ctx.getColor(R.color.status_rejected_text));
                tv.setBackgroundResource(R.drawable.bg_tag_error);
                break;
            case "REVIEWED":
                tv.setTextColor(ctx.getColor(R.color.status_reviewed_text));
                tv.setBackgroundResource(R.drawable.bg_chip_unselected);
                break;
            default: // APPLIED
                tv.setTextColor(ctx.getColor(R.color.status_applied_text));
                tv.setBackgroundResource(R.drawable.bg_tag_primary);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return showSkeleton ? SKELETON_COUNT : items.size();
    }

    private String resolveDisplayName(ApplicationDto app) {
        String source = app == null ? "" : safe(app.userId);
        if (source.isEmpty()) {
            return "Applicant";
        }
        if (source.contains("@")) {
            source = source.substring(0, source.indexOf('@'));
        }
        String normalized = source.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.matches("(?i)^[0-9a-f-]{6,}$")) {
            return "Applicant";
        }
        if (normalized.isEmpty()) {
            return "Applicant";
        }
        String[] words = normalized.split("\\s+");
        StringBuilder pretty = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (pretty.length() > 0) {
                pretty.append(' ');
            }
            pretty.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                pretty.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return pretty.length() == 0 ? "Applicant" : pretty.toString();
    }

    private void startShimmer(View view) {
        Object existing = view.getTag(R.id.tv_applicant_name);
        if (existing instanceof android.animation.ObjectAnimator) {
            return;
        }
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(view, "alpha", 0.4f, 1f);
        animator.setDuration(900);
        animator.setRepeatMode(android.animation.ObjectAnimator.REVERSE);
        animator.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        animator.start();
        view.setTag(R.id.tv_applicant_name, animator);
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvInitial;
        final TextView tvName;
        final TextView tvAppliedDate;
        final TextView tvStatus;
        final TextView btnShortlist;
        final TextView btnReject;

        VH(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tv_applicant_initial);
            tvName = itemView.findViewById(R.id.tv_applicant_name);
            tvAppliedDate = itemView.findViewById(R.id.tv_applicant_applied_date);
            tvStatus = itemView.findViewById(R.id.tv_applicant_status);
            btnShortlist = itemView.findViewById(R.id.btn_shortlist);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}
