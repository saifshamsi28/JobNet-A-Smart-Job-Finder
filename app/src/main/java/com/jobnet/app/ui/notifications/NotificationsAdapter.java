package com.jobnet.app.ui.notifications;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jobnet.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SKELETON = 0;
    private static final int VIEW_TYPE_ITEM     = 1;
    private static final int SKELETON_COUNT     = 5;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem item);
    }

    private final List<NotificationItem> items = new ArrayList<>();
    private final OnNotificationClickListener listener;
    private boolean showSkeleton = false;

    public NotificationsAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void showSkeleton(boolean show) {
        this.showSkeleton = show;
        notifyDataSetChanged();
    }

    public void submitItems(List<NotificationItem> data) {
        showSkeleton = false;
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return showSkeleton ? VIEW_TYPE_SKELETON : VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return showSkeleton ? SKELETON_COUNT : items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SKELETON) {
            View v = inflater.inflate(R.layout.item_notification_skeleton, parent, false);
            return new SkeletonViewHolder(v);
        }
        View v = inflater.inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SkeletonViewHolder) {
            startShimmer(holder.itemView);
            return;
        }
        NotificationItem item = items.get(position);
        NotificationViewHolder h = (NotificationViewHolder) holder;

        h.title.setText(item.title);
        h.message.setText(item.message);
        h.time.setText(item.timestamp == null || item.timestamp.isBlank()
                ? holder.itemView.getContext().getString(R.string.notification_recent)
                : item.timestamp);

        String status = item.status == null ? "" : item.status.toUpperCase(Locale.ROOT);
        String chipLabel = status.isBlank()
                ? holder.itemView.getContext().getString(R.string.notification_update)
                : status.replace('_', ' ');
        h.statusChip.setText(chipLabel);
        applyStatusChip(holder.itemView.getContext(), h.statusChip, status);
        applyEventIcon(h.eventIcon, item.type, status);

        // Unread indicator (driven by persisted read state)
        h.unreadDot.setVisibility(item.unread ? View.VISIBLE : View.GONE);

        // Entrance fade
        holder.itemView.setAlpha(0f);
        holder.itemView.animate().alpha(1f).setDuration(250).setStartDelay(position * 40L).start();

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(item);
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void applyStatusChip(Context ctx, TextView chip, String status) {
        switch (status) {
            case "REJECTED":
                chip.setBackgroundResource(R.drawable.bg_tag_error);
                chip.setTextColor(ctx.getColor(R.color.status_rejected_text));
                break;
            case "SHORTLISTED":
                chip.setBackgroundResource(R.drawable.bg_tag_warning);
                chip.setTextColor(ctx.getColor(R.color.status_shortlisted_text));
                break;
            case "REVIEWED":
            case "INTERVIEW":
                chip.setBackgroundResource(R.drawable.bg_tag_reviewed);
                chip.setTextColor(ctx.getColor(R.color.status_reviewed_text));
                break;
            case "OFFERED":
                chip.setBackgroundResource(R.drawable.bg_tag_green);
                chip.setTextColor(ctx.getColor(R.color.status_offered_text));
                break;
            case "WITHDRAWN":
                chip.setBackgroundResource(R.drawable.bg_tag_withdrawn);
                chip.setTextColor(ctx.getColor(R.color.status_withdrawn_text));
                break;
            default:
                chip.setBackgroundResource(R.drawable.bg_tag_primary);
                chip.setTextColor(ctx.getColor(R.color.status_applied_text));
                break;
        }
    }

    private void applyEventIcon(ImageView icon, String type, String status) {
        if (NotificationItem.TYPE_RECRUITER_APPLICANT.equals(type)) {
            icon.setImageResource(R.drawable.ic_users);
            icon.setColorFilter(icon.getContext().getColor(R.color.primary));
            return;
        }
        switch (status) {
            case "REJECTED":
                icon.setImageResource(R.drawable.ic_close);
                icon.setColorFilter(icon.getContext().getColor(R.color.status_rejected_text));
                break;
            case "SHORTLISTED":
            case "OFFERED":
                icon.setImageResource(R.drawable.ic_check);
                icon.setColorFilter(icon.getContext().getColor(R.color.status_offered_text));
                break;
            case "REVIEWED":
            case "INTERVIEW":
                icon.setImageResource(R.drawable.ic_clock);
                icon.setColorFilter(icon.getContext().getColor(R.color.status_reviewed_text));
                break;
            case "WITHDRAWN":
                icon.setImageResource(R.drawable.ic_close);
                icon.setColorFilter(icon.getContext().getColor(R.color.text_tertiary));
                break;
            default:
                icon.setImageResource(R.drawable.ic_briefcase);
                icon.setColorFilter(icon.getContext().getColor(R.color.primary));
                break;
        }
    }

    private void startShimmer(View view) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 0.4f, 1f);
        anim.setDuration(900);
        anim.setRepeatMode(ObjectAnimator.REVERSE);
        anim.setRepeatCount(ObjectAnimator.INFINITE);
        anim.start();
        view.setTag(R.id.tv_notification_title, anim);
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final ImageView eventIcon;
        final TextView title;
        final TextView message;
        final TextView time;
        final TextView statusChip;
        final View unreadDot;

        NotificationViewHolder(@NonNull View v) {
            super(v);
            eventIcon  = v.findViewById(R.id.iv_notification_icon);
            title      = v.findViewById(R.id.tv_notification_title);
            message    = v.findViewById(R.id.tv_notification_message);
            time       = v.findViewById(R.id.tv_notification_time);
            statusChip = v.findViewById(R.id.tv_notification_status);
            unreadDot  = v.findViewById(R.id.view_unread_dot);
        }
    }

    static class SkeletonViewHolder extends RecyclerView.ViewHolder {
        SkeletonViewHolder(@NonNull View v) { super(v); }
    }
}
