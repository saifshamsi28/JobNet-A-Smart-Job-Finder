package com.jobnet.app.util;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class SkeletonShimmerHelper {

    private static final long SHIMMER_DURATION_MS = 900L;
    private static final float SHIMMER_MIN_ALPHA = 0.45f;

    private SkeletonShimmerHelper() {
    }

    public static void start(View root, List<ObjectAnimator> runningAnimators) {
        stop(runningAnimators);
        if (root == null || runningAnimators == null) {
            return;
        }

        List<View> targets = new ArrayList<>();
        collectTargets(root, targets);

        for (int i = 0; i < targets.size(); i++) {
            View target = targets.get(i);
            target.setAlpha(SHIMMER_MIN_ALPHA);

            ObjectAnimator animator = ObjectAnimator.ofFloat(target, View.ALPHA, SHIMMER_MIN_ALPHA, 1f);
            animator.setDuration(SHIMMER_DURATION_MS);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setStartDelay((long) (i % 8) * 70L);
            animator.start();

            runningAnimators.add(animator);
        }
    }

    public static void stop(List<ObjectAnimator> runningAnimators) {
        if (runningAnimators == null) {
            return;
        }
        for (ObjectAnimator animator : runningAnimators) {
            if (animator != null) {
                animator.cancel();
            }
        }
        runningAnimators.clear();
    }

    private static void collectTargets(View node, List<View> targets) {
        if (node == null || node.getVisibility() != View.VISIBLE) {
            return;
        }

        if (node instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) node;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectTargets(group.getChildAt(i), targets);
            }
            return;
        }

        if (node.getBackground() == null) {
            return;
        }

        // Keep text/icon placeholders static and shimmer the shape blocks only.
        if (node instanceof TextView || node instanceof ImageView) {
            return;
        }

        targets.add(node);
    }
}
