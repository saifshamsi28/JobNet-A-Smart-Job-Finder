package com.jobnet.app.ui.main;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jobnet.app.R;

public class SplashActivity extends AppCompatActivity {

	private static final long SPLASH_DURATION_MS = 1500L;

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable navigateRunnable = this::openMain;

	private ValueAnimator titleRevealAnimator;
	private ObjectAnimator logoSlideAnimator;
	private ObjectAnimator titleSlideXAnimator;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		View logoPlate = findViewById(R.id.splash_logo_plate);
		View title = findViewById(R.id.splash_title);
		View titleMask = findViewById(R.id.splash_title_reveal_mask);

		animateHero(logoPlate, title, titleMask);

		handler.postDelayed(navigateRunnable, SPLASH_DURATION_MS);
	}

	@Override
	protected void onDestroy() {
		handler.removeCallbacks(navigateRunnable);
		if (titleRevealAnimator != null) {
			titleRevealAnimator.cancel();
		}
		if (logoSlideAnimator != null) {
			logoSlideAnimator.cancel();
		}
		if (titleSlideXAnimator != null) {
			titleSlideXAnimator.cancel();
		}
		super.onDestroy();
	}

	private void animateHero(View logoPlate, View title, View titleMask) {
		// Phase 1: logo appears from small to full size in center.
		logoPlate.setScaleX(0.2f);
		logoPlate.setScaleY(0.2f);
		logoPlate.setAlpha(0f);
		ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoPlate, "scaleX", 0.2f, 1f);
		ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoPlate, "scaleY", 0.2f, 1f);
		ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logoPlate, "alpha", 0f, 1f);
		logoScaleX.setDuration(1000);
		logoScaleY.setDuration(1000);
		logoAlpha.setDuration(480);
		logoScaleX.setInterpolator(new DecelerateInterpolator());
		logoScaleY.setInterpolator(new DecelerateInterpolator());
		logoAlpha.setInterpolator(new DecelerateInterpolator());
		logoScaleX.start();
		logoScaleY.start();
		logoAlpha.start();

		// Phase 2: slide logo left, reveal title from behind logo with masked width expansion.
		title.setTranslationX(-60f);

		logoSlideAnimator = ObjectAnimator.ofFloat(logoPlate, "translationX", 0f, -60f);
		logoSlideAnimator.setStartDelay(100);
		logoSlideAnimator.setDuration(750);
		logoSlideAnimator.setInterpolator(new AccelerateInterpolator());
		logoSlideAnimator.start();

		titleSlideXAnimator = ObjectAnimator.ofFloat(title, "translationX", -350f, 0f);
		titleSlideXAnimator.setStartDelay(100);
		titleSlideXAnimator.setDuration(750);
		titleSlideXAnimator.setInterpolator(new AccelerateInterpolator());
		titleSlideXAnimator.start();

//		if (titleMask != null) {
//			titleMask.setClipBounds(new Rect(0, 0, 0, Math.max(titleMask.getHeight(), 1)));
//			title.post(() -> {
//				if (!isFinishing() && !isDestroyed()) {
//					android.widget.TextView titleText = null;
//					int measuredText = title.getWidth();
//					if (title instanceof android.widget.TextView) {
//						titleText = (android.widget.TextView) title;
//					}
//					if (titleText != null && titleText.getText() != null) {
//						measuredText = Math.max(measuredText,
//								Math.round(titleText.getPaint().measureText(titleText.getText().toString())) + titleMask.getPaddingStart() + titleMask.getPaddingEnd());
//					}
//					final int revealWidth = Math.max(1, measuredText);
//					final int revealHeight = Math.max(titleMask.getHeight(), title.getHeight());
//					final Rect clipRect = new Rect();
//					titleRevealAnimator = ValueAnimator.ofFloat(0f, 1f);
//					titleRevealAnimator.setStartDelay(540);
//					titleRevealAnimator.setDuration(390);
//					titleRevealAnimator.setInterpolator(new DecelerateInterpolator(1.2f));
//					titleRevealAnimator.addUpdateListener(animation -> {
//						float progress = (float) animation.getAnimatedValue();
//						int right = Math.round(revealWidth * progress);
//						clipRect.set(0, 0, right, revealHeight);
//						titleMask.setClipBounds(clipRect);
//					});
//					titleRevealAnimator.start();
//				}
//			});
//		}

		View orbTop = findViewById(R.id.splash_orb_top);
		View orbBottom = findViewById(R.id.splash_orb_bottom);

		ObjectAnimator topDriftX = ObjectAnimator.ofFloat(orbTop, "translationX", 0f, -14f, 0f);
		topDriftX.setDuration(2200);
		topDriftX.setRepeatCount(ObjectAnimator.INFINITE);
		topDriftX.setRepeatMode(ObjectAnimator.RESTART);

		ObjectAnimator topDriftY = ObjectAnimator.ofFloat(orbTop, "translationY", 0f, 16f, 0f);
		topDriftY.setDuration(2200);
		topDriftY.setRepeatCount(ObjectAnimator.INFINITE);
		topDriftY.setRepeatMode(ObjectAnimator.RESTART);

		ObjectAnimator bottomDriftX = ObjectAnimator.ofFloat(orbBottom, "translationX", 0f, 12f, 0f);
		bottomDriftX.setDuration(2400);
		bottomDriftX.setRepeatCount(ObjectAnimator.INFINITE);
		bottomDriftX.setRepeatMode(ObjectAnimator.RESTART);

		ObjectAnimator bottomDriftY = ObjectAnimator.ofFloat(orbBottom, "translationY", 0f, -12f, 0f);
		bottomDriftY.setDuration(2400);
		bottomDriftY.setRepeatCount(ObjectAnimator.INFINITE);
		bottomDriftY.setRepeatMode(ObjectAnimator.RESTART);

		topDriftX.start();
		topDriftY.start();
		bottomDriftX.start();
		bottomDriftY.start();
	}

	private void openMain() {
		Intent intent = new Intent(this, MainActivity.class);
		if (getIntent() != null && getIntent().getExtras() != null) {
			intent.putExtras(getIntent().getExtras());
		}
		startActivity(intent);
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		finish();
	}
}
