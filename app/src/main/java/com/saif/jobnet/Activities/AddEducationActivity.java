package com.saif.jobnet.Activities;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityAddEducationBinding;

public class AddEducationActivity extends AppCompatActivity {

    private ActivityAddEducationBinding binding;
    Drawable closeIcon;
    private RadioButton lastSelected = null; // Track the last selected button
    private User user;
    private AppDatabase db;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityAddEducationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        closeIcon = ContextCompat.getDrawable(this, R.drawable.cancel_icon);
        binding.graduationOrDiploma.setOnClickListener(v -> handleSelection(binding.graduationOrDiploma));
        binding.matriculation.setOnClickListener(v -> handleSelection(binding.matriculation));
        binding.intermediate.setOnClickListener(v -> handleSelection(binding.intermediate));
        binding.saveButton.setOnClickListener(view -> Toast.makeText(AddEducationActivity.this, "Save button clicked", Toast.LENGTH_SHORT).show());
        binding.cancelButton.setOnClickListener(view -> finish());
        binding.backButton.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void handleSelection(RadioButton selected) {
        if (selected == lastSelected) {
            // User clicked the same button, so reset everything
            resetGenderSelection();
            lastSelected = null;
        } else {
            // New selection, show only this and hide others
            showSelectedGender(selected);
            lastSelected = selected;
        }
    }

    private void showSelectedGender(RadioButton selected) {
        for (int i = 0; i < binding.courseLevel.getChildCount(); i++) {
            View view = binding.courseLevel.getChildAt(i);
            if (view instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) view;
                if (radioButton == selected) {
                    radioButton.setChecked(true);
                    radioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, closeIcon, null);
                    radioButton.setVisibility(View.VISIBLE);
                    showEducationSection(radioButton);
//                    System.out.println("radio button selected: "+radioButton.getText());
                    radioButton.setBackground(ContextCompat.getDrawable(this, R.drawable.gender_selected));
                } else {
                    radioButton.setChecked(false);
                    radioButton.setVisibility(View.GONE);
                    radioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
            }
        }
    }

    private void hideEducationSection() {
        binding.graduationEduSection.setVisibility(View.GONE);
        binding.intermediateEduSection.setVisibility(View.GONE);
        binding.matriculationEduSection.setVisibility(View.GONE);
    }

    private void showEducationSection(RadioButton radioButton) {
        //hide visibility of all section
        hideEducationSection();
        switch (radioButton.getText().toString()){
            case "Graduation/Diploma":
                binding.graduationEduSection.setVisibility(View.VISIBLE);
                break;
            case "Class XII":
                binding.intermediateEduSection.setVisibility(View.VISIBLE);
                break;
            case "Class X":
                binding.matriculationEduSection.setVisibility(View.VISIBLE);
                break;
            case "Post Graduate":
                Toast.makeText(this, "PG coming soon", Toast.LENGTH_SHORT).show();
                break;
            case "Doctorate":
                Toast.makeText(this, "Doctorate coming soon", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void resetGenderSelection() {
        hideEducationSection();
        for (int i = 0; i < binding.courseLevel.getChildCount(); i++) {
            View view = binding.courseLevel.getChildAt(i);
            if (view instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) view;
                radioButton.setVisibility(View.VISIBLE);
                radioButton.setChecked(false);
//                System.out.println("radio button deselected: "+radioButton.getText());
                radioButton.setBackground(ContextCompat.getDrawable(this, R.drawable.gender_unselected));
                radioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }
        }
    }
}