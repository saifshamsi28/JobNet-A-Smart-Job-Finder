package com.saif.jobnet.Activities;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityEditBasicDetailsBinding;

import java.util.Calendar;

public class EditBasicDetailsActivity extends AppCompatActivity {

    ActivityEditBasicDetailsBinding binding;
    Drawable closeIcon;
    private RadioButton lastSelected = null; // Track the last selected button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBasicDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        binding.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        binding.dateOfBirth.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private final String ddmmyyyy = "DDMMYYYY";
            private final Calendar cal = Calendar.getInstance();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d]", ""); // Remove non-numeric
                    String cleanC = current.replaceAll("[^\\d]", "");

                    int cl = clean.length();
                    int sel = cl;
                    for (int i = 2; i <= cl && i < 6; i += 2) {
                        sel++;
                    }

                    if (clean.equals(cleanC)) sel--;

                    if (clean.length() < 8) {
                        clean = clean + ddmmyyyy.substring(clean.length());
                    } else {
                        int day = Integer.parseInt(clean.substring(0, 2));
                        int month = Integer.parseInt(clean.substring(2, 4));
                        int year = Integer.parseInt(clean.substring(4, 8));

                        month = Math.max(1, Math.min(12, month));
                        cal.set(Calendar.MONTH, month - 1);
                        year = Math.max(1900, Math.min(2100, year));
                        cal.set(Calendar.YEAR, year);
                        day = Math.min(day, cal.getActualMaximum(Calendar.DATE));

                        clean = String.format("%02d%02d%04d", day, month, year);
                    }

                    clean = String.format("%s/%s/%s",
                            clean.substring(0, 2),
                            clean.substring(2, 4),
                            clean.substring(4, 8));

                    sel = Math.max(0, Math.min(sel, clean.length()));
                    current = clean;
                    binding.dateOfBirth.setText(current);
                    binding.dateOfBirth.setSelection(sel);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        closeIcon = ContextCompat.getDrawable(this, R.drawable.cancel_icon);
        binding.radioMale.setOnClickListener(v -> handleSelection(binding.radioMale));
        binding.radioFemale.setOnClickListener(v -> handleSelection(binding.radioFemale));
        binding.radioTransgender.setOnClickListener(v -> handleSelection(binding.radioTransgender));
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
        for (int i = 0; i < binding.radioGroupGender.getChildCount(); i++) {
            View view = binding.radioGroupGender.getChildAt(i);
            if (view instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) view;
                if (radioButton == selected) {
                    radioButton.setChecked(true);
                    radioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, closeIcon, null);
                    radioButton.setVisibility(View.VISIBLE);
                    System.out.println("radio button selected: "+radioButton.getText());
                    radioButton.setBackground(ContextCompat.getDrawable(this, R.drawable.gender_selected));
                } else {
                    radioButton.setChecked(false);
                    radioButton.setVisibility(View.GONE);
                    radioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
            }
        }
    }

    private void resetGenderSelection() {
        for (int i = 0; i < binding.radioGroupGender.getChildCount(); i++) {
            View view = binding.radioGroupGender.getChildAt(i);
            if (view instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) view;
                radioButton.setVisibility(View.VISIBLE);
                radioButton.setChecked(false);
                System.out.println("radio button deselected: "+radioButton.getText());
                radioButton.setBackground(ContextCompat.getDrawable(this, R.drawable.gender_unselected));
                radioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }
        }
    }
}
