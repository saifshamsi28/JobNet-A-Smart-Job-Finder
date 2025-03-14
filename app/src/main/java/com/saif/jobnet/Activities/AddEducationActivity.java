package com.saif.jobnet.Activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityAddEducationBinding;

public class AddEducationActivity extends AppCompatActivity {

    private ActivityAddEducationBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityAddEducationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}