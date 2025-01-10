package com.saif.jobnet.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.saif.jobnet.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

//        ImageView logo = findViewById(R.id.jobnet_logo);
//        TextView bottom = findViewById(R.id.bottom);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slide_left = AnimationUtils.loadAnimation(this, R.anim.move_left_to_right);
//        logo.startAnimation(fadeIn);
//        bottom.startAnimation(slide_left);

        // Load animations
        Animation leftToRight = AnimationUtils.loadAnimation(this, R.anim.move_left_to_right);
        Animation rightToLeft = AnimationUtils.loadAnimation(this, R.anim.move_right_to_left);

        // Apply animations to each job title
        findViewById(R.id.title1).startAnimation(leftToRight);
        findViewById(R.id.title2).startAnimation(rightToLeft);
        findViewById(R.id.title3).startAnimation(leftToRight);
        findViewById(R.id.title4).startAnimation(rightToLeft);
        findViewById(R.id.title5).startAnimation(leftToRight);
        findViewById(R.id.title6).startAnimation(rightToLeft);
        findViewById(R.id.title7).startAnimation(leftToRight);
        findViewById(R.id.title8).startAnimation(rightToLeft);
        findViewById(R.id.title9).startAnimation(leftToRight);
        findViewById(R.id.title10).startAnimation(rightToLeft);
        findViewById(R.id.title11).startAnimation(leftToRight);
        findViewById(R.id.title12).startAnimation(rightToLeft);
        findViewById(R.id.title13).startAnimation(leftToRight);
        findViewById(R.id.title14).startAnimation(rightToLeft);
        findViewById(R.id.title15).startAnimation(leftToRight);
        findViewById(R.id.title16).startAnimation(rightToLeft);
        findViewById(R.id.title17).startAnimation(leftToRight);
        findViewById(R.id.title18).startAnimation(rightToLeft);
        findViewById(R.id.title19).startAnimation(leftToRight);
        findViewById(R.id.title20).startAnimation(rightToLeft);
        // After 3 seconds, move to MainActivity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 3000);
    }
}