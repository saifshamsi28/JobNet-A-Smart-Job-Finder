package com.saif.jobnet.Activities;

import static com.saif.jobnet.Utils.Config.BASE_URL;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.saif.jobnet.Api.ApiService;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Models.BasicDetails;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityEditBasicDetailsBinding;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditBasicDetailsActivity extends AppCompatActivity {

    ActivityEditBasicDetailsBinding binding;
    Drawable closeIcon;
    private RadioButton lastSelected = null; // Track the last selected button
    private User user;
    private AppDatabase db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBasicDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        String userId = getIntent().getStringExtra("userId");
        db = DatabaseClient.getInstance(this).getAppDatabase();
        new Thread(new Runnable() {
            @Override
            public void run() {
                user = db.jobDao().getCurrentUser(userId).getValue();

                if (user != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setBasicDetails(user);
                        }
                    });
                }
//                System.out.println("user: "+user);
//                if (user == null) {
//                    getUserFromBackend(userId);
//                }
            }
        }).start();

        //set the details which is already saved

        binding.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        binding.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBasicDetails();
            }
        });

        binding.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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

    private void getUserFromBackend(String userId) {
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(new OkHttpClient()
                        .newBuilder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService=retrofit.create(ApiService.class);

    }

    private void setBasicDetails(User user) {
        binding.editFullName.setText(user.getName());
        binding.mobileNumber.setText(user.getPhoneNumber());
        BasicDetails basicDetails = user.getBasicDetails();
        if (basicDetails != null) {
            binding.editFullName.setText(user.getName());
            binding.mobileNumber.setText(user.getPhoneNumber());
            binding.currentCity.setText(basicDetails.getCurrentCity());
            binding.homeTown.setText(basicDetails.getHomeTown());
            if (basicDetails.getGender() != null) {
                switch (basicDetails.getGender()) {
                    case "Male":
                        showSelectedGender(binding.radioMale);
                        binding.radioMale.setCompoundDrawablesWithIntrinsicBounds(null, null, closeIcon, null);
                        lastSelected = binding.radioMale;
                        break;
                    case "Female":
                        showSelectedGender(binding.radioFemale);
                        binding.radioFemale.setCompoundDrawablesWithIntrinsicBounds(null, null, closeIcon, null);
                        lastSelected = binding.radioFemale;
                        break;
                    case "Transgender":
                        showSelectedGender(binding.radioTransgender);
                        binding.radioTransgender.setCompoundDrawablesWithIntrinsicBounds(null, null, closeIcon, null);
                        lastSelected = binding.radioTransgender;
                        break;
                    default:
                        //none is selected show all the radio button
                        resetGenderSelection();
                        break;
                }

//                binding.radioMale.setCompoundDrawablesWithIntrinsicBounds(null, null, closeIcon, null);
            }
            if (basicDetails.getDateOfBirth() != null) {
                String[] dateParts = basicDetails.getDateOfBirth().split("/");
                if (dateParts.length == 3) {
                    int day = Integer.parseInt(dateParts[0]);
                    int month = Integer.parseInt(dateParts[1]) - 1;
                    int year = Integer.parseInt(dateParts[2]);
                    binding.dateOfBirth.setText(String.format("%02d/%02d/%04d", day, month + 1, year));
                }
            }
            binding.currentCity.setText(basicDetails.getCurrentCity());
            binding.homeTown.setText(basicDetails.getHomeTown());
        }
    }

    private void saveBasicDetails() {
        String fullName = binding.editFullName.getText().toString().trim();
        String dateOfBirth = binding.dateOfBirth.getText().toString().trim();
        RadioButton genderRadioButton = binding.radioGroupGender.findViewById(binding.radioGroupGender.getCheckedRadioButtonId());
        String gender = genderRadioButton != null ? genderRadioButton.getText().toString() : "";
        String phoneNumber = binding.mobileNumber.getText().toString().trim();
        String currentCity = binding.currentCity.getText().toString().trim();
        String homeTown = binding.homeTown.getText().toString().trim();

        if (fullName.isEmpty() || dateOfBirth.isEmpty() || gender.isEmpty() || phoneNumber.isEmpty() || currentCity.isEmpty() || homeTown.isEmpty()) {
            Toast.makeText(this, "Please fill in all the fields", Toast.LENGTH_SHORT).show();
            return;
        }
        BasicDetails basicDetails = new BasicDetails(gender,dateOfBirth,currentCity,homeTown);
        user.setName(fullName);
        user.setPhoneNumber(phoneNumber);
        user.setBasicDetails(basicDetails);
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.jobDao().insertOrUpdateUser(user);
                System.out.println("updated user: "+user);
            }
            }).start();
        finish();
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
