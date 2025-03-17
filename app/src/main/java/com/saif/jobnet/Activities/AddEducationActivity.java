package com.saif.jobnet.Activities;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.flexbox.FlexboxLayout;
import com.saif.jobnet.Api.ApiService;
import com.saif.jobnet.ApiResponse;
import com.saif.jobnet.BottomSheetFragment;
import com.saif.jobnet.Course;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityAddEducationBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddEducationActivity extends AppCompatActivity {

    private ActivityAddEducationBinding binding;
    Drawable closeIcon;
    private RadioButton lastSelected = null; // Track the last selected button
    private User user;
    private AppDatabase db;
    private ProgressDialog progressDialog;
    private ArrayList<String> coursesList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityAddEducationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        progressDialog=new ProgressDialog(this);

        closeIcon = ContextCompat.getDrawable(this, R.drawable.cancel_icon);
        if (closeIcon != null) {
            closeIcon.setTint(ContextCompat.getColor(this, R.color.colorActionBarBackground));
        }

        //course level radio buttons
        binding.graduationOrDiploma.setOnClickListener(v -> handleSelection(binding.graduationOrDiploma,binding.courseLevel));
        binding.matriculation.setOnClickListener(v -> handleSelection(binding.matriculation, binding.courseLevel));
        binding.intermediate.setOnClickListener(v -> handleSelection(binding.intermediate, binding.courseLevel));

        //grading system radio buttons
        binding.gpaOutOf10.setOnClickListener(v -> handleSelection(binding.gpaOutOf10, binding.gradingSystemFlexLayout));
        binding.gpaOutOf04.setOnClickListener(v -> handleSelection(binding.gpaOutOf04, binding.gradingSystemFlexLayout));
        binding.percentage.setOnClickListener(v -> handleSelection(binding.percentage, binding.gradingSystemFlexLayout));
        binding.courseRequiresAPass.setOnClickListener(v -> handleSelection(binding.courseRequiresAPass, binding.gradingSystemFlexLayout));

        //course type radio buttons
        binding.fullTime.setOnClickListener(v -> handleSelection(binding.fullTime, binding.courseTypeFlexLayout));
        binding.partTime.setOnClickListener(v -> handleSelection(binding.partTime, binding.courseTypeFlexLayout));
        binding.correspondence.setOnClickListener(v -> handleSelection(binding.correspondence, binding.courseTypeFlexLayout));
//        binding.setOnClickListener(v -> handleSelection(binding.internship, binding.courseTypeFlexLayout));

        binding.saveButton.setOnClickListener(view -> Toast.makeText(AddEducationActivity.this, "Save button clicked", Toast.LENGTH_SHORT).show());
        binding.cancelButton.setOnClickListener(view -> finish());
        binding.backButton.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());


        binding.graduationCourseName.setOnClickListener(view -> {
            // Open Bottom Sheet
//            ArrayList<String> courses = new ArrayList<>();
//            courses.add("B.Tech");
//            courses.add("B.Com");
//            courses.add("B.Sc");
//            courses.add("B.A");
//            courses.add("BCA");
//            courses.add("MBA");
//
//            BottomSheetFragment bottomSheet = new BottomSheetFragment(this, courses);
//            bottomSheet.show(getSupportFragmentManager(), "BottomSheet");
            fetchCourses();
        });

    }
    private void fetchCourses() {
        progressDialog.setMessage("Fetching courses...");
        progressDialog.show();
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl("https://api.data.gov.in/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);

        apiService.getCourses().enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("API_RESPONSE", "Response successful");
                    Log.d("API_RESPONSE", "Response received: "+response.body());
                    filterUnderGraduateCourses(response.body().getRecords());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("API_ERROR", "Error fetching data: " + t.getMessage());
            }
        });
    }

    private void filterUnderGraduateCourses(List<Course> records) {
        coursesList.clear();

        Set<String> uniqueCourses = new HashSet<>();

        for (Course course : records) {
            uniqueCourses.add(course.getProgramme());
            System.out.println("course: "+course);
        }
        coursesList.addAll(uniqueCourses);

        // Open Bottom Sheet after filtering
        BottomSheetFragment bottomSheet = new BottomSheetFragment(AddEducationActivity.this, coursesList);
        bottomSheet.show(getSupportFragmentManager(), "BottomSheet");
    }

    public void setSelectedCourse(String course) {
        binding.graduationCourseName.setText(course);
    }


    private void handleSelection(RadioButton selected, FlexboxLayout radioButtonCategory) {
        if (selected == lastSelected) {
            // User clicked the same button, so reset everything
            resetRadioButtonsSelection(radioButtonCategory);
            lastSelected = null;
        } else {
            // New selection, show only this and hide others
            showSelectedRadioButton(selected, radioButtonCategory);
            lastSelected = selected;
        }
    }

    private void showSelectedRadioButton(RadioButton selected, FlexboxLayout radioButtonCategory) {
        for (int i = 0; i < radioButtonCategory.getChildCount(); i++) {
            View view = radioButtonCategory.getChildAt(i);
            if (view instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) view;
                if (radioButton == selected) {
                    radioButton.setChecked(true);
                    radioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, closeIcon, null);
                    radioButton.setVisibility(View.VISIBLE);
                    if (radioButtonCategory.getId()==binding.courseLevel.getId()) {
                        System.out.println("selected radio button category: "+radioButtonCategory.getChildAt(0));
                        showEducationSection(radioButton);
                    } else if (radioButtonCategory.getId()==binding.gradingSystemFlexLayout.getId()) {
                        System.out.println("called grading flex");
                        //if none of the grading system radio button is selected, hide GPA obtained layout
                        if (binding.gradingSystemFlexLayout.isSelected()) {
                            binding.gpaObtainedLayout.setVisibility(View.GONE);
                            binding.gpaObtained.setVisibility(View.GONE);
                        }else {
                            binding.gpaObtained.setVisibility(View.VISIBLE);
                            binding.gpaObtainedLayout.setVisibility(View.VISIBLE);

                            // Set the hint on TextInputLayout, not on EditText
                            binding.gpaObtainedLayout.setHint(selected.getText().toString()+"*");

                            binding.gpaObtainedLayout.setHintAnimationEnabled(true);
                            binding.gpaObtainedLayout.setHintEnabled(true);
                        }
                    }
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

    private void resetRadioButtonsSelection(FlexboxLayout radioButtonCategory) {
        //hide or show education section iff course level radio button is selected
        if (radioButtonCategory.getId()==binding.courseLevel.getId()) {
            System.out.println("selected radio button category: "+radioButtonCategory.getChildAt(0));
            hideEducationSection();
        } else if (radioButtonCategory.getId() == binding.gradingSystemFlexLayout.getId()) {
            binding.gpaObtainedLayout.setVisibility(View.GONE);
            binding.gpaObtained.setVisibility(View.GONE);
        }
        for (int i = 0; i < radioButtonCategory.getChildCount(); i++) {
            View view = radioButtonCategory.getChildAt(i);
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