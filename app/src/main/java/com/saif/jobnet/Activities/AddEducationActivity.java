package com.saif.jobnet.Activities;

import static android.view.View.VISIBLE;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Models.Education.Class12Details;
import com.saif.jobnet.Models.Education.GraduationDetails;
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
    private RadioButton courseLevelSelectedRadioButton;
    private User user;
    private AppDatabase db;
    private ProgressDialog progressDialog;
    private ArrayList<String> coursesList = new ArrayList<>();
    private ArrayList<Course> courses=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityAddEducationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        progressDialog=new ProgressDialog(this);
        db= DatabaseClient.getInstance(this).getAppDatabase();

        //fetch user from local database
        String userId=getIntent().getStringExtra("userId");
        String educationSection=getIntent().getStringExtra("educationSection");
        new Thread(new Runnable() {
            @Override
            public void run() {
                user=db.jobDao().getCurrentUser(userId);

                //if there exists education details then set the visibility of education section
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(user.getGraduationDetailsList()!=null && !user.getGraduationDetailsList().isEmpty()){
                            setEducationDetails(educationSection);
                        }
                    }
                });
            }
        }).start();

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

        //action buttons
        binding.saveButton.setOnClickListener(view -> savedEducationDetails());
        binding.cancelButton.setOnClickListener(view -> finish());
        binding.backButton.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());


        binding.graduationCourseName.setOnClickListener(view -> {
            fetchCourses();
        });

        binding.courseSpecialization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open Bottom Sheet after filtering
                filterUnderGraduateCourses(courses,"specialization");
            }
        });

    }

    private void setEducationDetails(String educationSection) {
        GraduationDetails graduationDetails = getEducationByLevel(educationSection);

        System.out.println("setting the graduationDetails: "+ graduationDetails);

        if (graduationDetails == null) {
            Toast.makeText(this, "No details found for " + educationSection, Toast.LENGTH_SHORT).show();
            return;
        }

        switch (educationSection) {
            case "Graduation/Diploma":
                setGraduationDetails(graduationDetails);
                break;
            case "Class XII":
//                setIntermediateDetails(graduationDetails);
                break;
            case "Class X":
//                setMatriculationDetails(graduationDetails);
                break;
        }
    }

    // ✅ Helper method to find GraduationDetails object by level
    private GraduationDetails getEducationByLevel(String level) {
        if (user == null || user.getGraduationDetailsList() == null) return null;

        for (GraduationDetails edu : user.getGraduationDetailsList()) {
            if (edu.getEducationLevel().equalsIgnoreCase(level)) {
                return edu; // Found the education object
            }
        }
        return null; // No matching education found
    }

    // ✅ Set Graduation Details
    private void setGraduationDetails(GraduationDetails graduationDetails) {
        binding.graduationEduSection.setVisibility(View.VISIBLE);

        // ✅ Set "Graduation/Diploma" Radio Button
        if (graduationDetails.getEducationLevel().equalsIgnoreCase("Graduation/Diploma")) {
            showSelectedRadioButton(binding.graduationOrDiploma, binding.courseLevel);
        }

        // ✅ Set Course & Specialization
        binding.graduationCourseName.setText(graduationDetails.getCourse());
        binding.courseSpecialization.setText(graduationDetails.getSpecialization());
        binding.graduationCollegeName.setText(graduationDetails.getCollege());

        // ✅ Set "Full Time / Part Time / Distance" Radio Button
        if (graduationDetails.getCourseType().equalsIgnoreCase("Full Time")) {
            showSelectedRadioButton(binding.fullTime, binding.courseTypeFlexLayout);
        } else if (graduationDetails.getCourseType().equalsIgnoreCase("Part Time")) {
            showSelectedRadioButton(binding.partTime, binding.courseTypeFlexLayout);
        } else {
            showSelectedRadioButton(binding.correspondence, binding.courseTypeFlexLayout);
        }

        // ✅ Set GPA Scale (Grading System)
        if (graduationDetails.getGpaScale().equals("10")) {
            showSelectedRadioButton(binding.gpaOutOf10, binding.gradingSystemFlexLayout);
        } else if (graduationDetails.getGpaScale().equals("4")) {
            showSelectedRadioButton(binding.gpaOutOf04, binding.gradingSystemFlexLayout);
        } else if (graduationDetails.getGpaScale().equals("100")) {
            showSelectedRadioButton(binding.percentage, binding.gradingSystemFlexLayout);
        }else{
            showSelectedRadioButton(binding.courseRequiresAPass, binding.gradingSystemFlexLayout);
        }

        // ✅ Set CGPA / GPA / Percentage
        binding.gpaObtained.setText(graduationDetails.getCgpaObtained());

        // ✅ Set Start & End Year
        binding.graduationStartYear.setText(graduationDetails.getEnrollmentYear());
        binding.graduationEndYear.setText(graduationDetails.getPassingYear());
    }


    // ✅ Set Intermediate (Class 12) Details
//    private void setIntermediateDetails(GraduationDetails education) {
//        binding.class12EduSection.setVisibility(View.VISIBLE);
//        binding.class12SchoolName.setText(education.getCollege()); // Assuming college field holds school name
//        binding.class12Stream.setText(education.getSpecialization());
//        binding.class12Board.setText(education.getCourseType()); // Example: "CBSE", "State Board"
//        binding.class12Percentage.setText(education.getCgpaObtained()); // Assuming it's stored as GPA
//        binding.class12StartYear.setText(education.getEnrollmentYear());
//        binding.class12EndYear.setText(education.getPassingYear());
//    }

    // ✅ Set Matriculation (Class 10) Details
//    private void setMatriculationDetails(GraduationDetails education) {
//        binding.class10EduSection.setVisibility(View.VISIBLE);
//        binding.class10SchoolName.setText(education.getCollege());
//        binding.class10Board.setText(education.getCourseType());
//        binding.class10Percentage.setText(education.getCgpaObtained());
//        binding.class10StartYear.setText(education.getEnrollmentYear());
//        binding.class10EndYear.setText(education.getPassingYear());
//    }

    private void savedEducationDetails() {
        if(courseLevelSelectedRadioButton!=null){
            switch (courseLevelSelectedRadioButton.getText().toString()){
                case "Graduation/Diploma":
                    saveGraduationDetails();
                    break;
                case "Class XII":
                    saveIntermediateDetails();
                    break;
                case "Class X":
                    saveMatriculationDetails();
                    break;
                case "Post Graduate":
                    Toast.makeText(this, "PG coming soon", Toast.LENGTH_SHORT).show();
                    break;
                case "Doctorate":
                    Toast.makeText(this, "Doctorate coming soon", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void saveMatriculationDetails() {
    }

    private void saveIntermediateDetails() {
        if(binding.boardName12th.getText()==null || binding.boardName12th.getText().toString().isEmpty()){
            binding.boardName12th.setError("Please enter board name");
            return;
        }
        if(binding.schoolMedium12th.getText()==null || binding.schoolMedium12th.getText().toString().isEmpty()){
            binding.schoolMedium12th.setError("Please enter school medium");
            return;
        }
        if(binding.schoolName12th.getText()==null || binding.schoolName12th.getText().toString().isEmpty()) {
            binding.schoolName12th.setError("Please enter school name");
            return;
        }
        if(binding.passOutYear12th.getText()==null || binding.passOutYear12th.getText().toString().isEmpty()){
            binding.passOutYear12th.setError("Please enter pass out year");
            return;
        }
        if(binding.marks12th.getText()==null || binding.marks12th.getText().toString().isEmpty()){
            binding.marks12th.setError("Please enter obtained marks");
            return;
        }
        if(binding.englishMarks12th.getText()==null || binding.englishMarks12th.getText().toString().isEmpty()){
            binding.englishMarks12th.setError("Please enter elglish");
            return;
        }
        if(binding.mathsMarks12th.getText()==null || binding.mathsMarks12th.getText().toString().isEmpty()){
            binding.mathsMarks12th.setError("Please enter maths marks");
            return;
        }

        if(binding.stream12th.getText()==null || binding.stream12th.getText().toString().isEmpty()){
            binding.stream12th.setError("Please enter stream");
            return;
        }

        String board=binding.boardName12th.getText().toString().trim();
        String schoolName=binding.schoolName12th.getText().toString().trim();
        String medium=binding.schoolMedium12th.getText().toString().trim();
        String stream=binding.stream12th.getText().toString().trim();
        String totalMarks=binding.marks12th.getText().toString().trim();
        String englishMarks=binding.englishMarks12th.getText().toString().trim();
        String mathsMarks=binding.mathsMarks12th.getText().toString().trim();
        String passingYear=binding.passOutYear12th.getText().toString().trim();
        Class12Details class12Details=new Class12Details(board,schoolName,medium,stream,totalMarks,englishMarks,mathsMarks,passingYear,"Class XII");
        if (user.getGraduationDetailsList() == null || user.getGraduationDetailsList().isEmpty()) {
            System.out.println("class12Details list is null, creating new list");
            user.setGraduationDetailsList(new ArrayList<>());
        }
        user.getGraduationDetailsList().add(class12Details);
        System.out.println("size of class12Details list after: "+user.getGraduationDetailsList().size());
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.jobDao().insertOrUpdateUser(user);
                db.jobDao().insertEducation(class12Details);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AddEducationActivity.this, "Class12Details added successfully", Toast.LENGTH_SHORT).show();
                        System.out.println("user details: "+user);
                        finish();
                    }
                });

            }
        }).start();

    }

    private void saveGraduationDetails() {
        if (binding.graduationCourseName.getText()==null || binding.graduationCourseName.getText().toString().isEmpty()) {
            binding.graduationCourseName.setError("Please select a course");
            return;
        }
        if (binding.courseSpecialization.getText()==null || binding.courseSpecialization.getText().toString().isEmpty()) {
            binding.courseSpecialization.setError("Please select a specialization");
            return;
        }
        if (binding.graduationCollegeName.getText()==null || binding.graduationCollegeName.getText().toString().isEmpty()) {
            binding.graduationCollegeName.setError("Please enter a college name");
            return;
        }

        //check if any grading system radio button not selected then through error
        if (binding.gpaObtained.getText()==null || binding.gpaObtained.getText().toString().isEmpty()) {
            binding.gpaObtained.setError("Please enter GPA");
            binding.gpaObtained.setVisibility(VISIBLE);
            return;
        }

        if(binding.graduationStartYear.getText()==null || binding.graduationStartYear.getText().toString().isEmpty()) {
            binding.graduationStartYear.setError("Please enter start year");
            return;
        }
        if(binding.graduationEndYear.getText()==null || binding.graduationEndYear.getText().toString().isEmpty()) {
            binding.graduationEndYear.setError("Please enter end year");
            return;
        }

        //check course type radio button is selected or not
        if (!binding.fullTime.isChecked() && !binding.partTime.isChecked() && !binding.correspondence.isChecked()) {
            binding.courseTypeHeading.setError("Please select a course type");
            return;
        }

        String courseLevel=courseLevelSelectedRadioButton.getText().toString();
        String course=binding.graduationCourseName.getText().toString().trim();
        String specialization=binding.courseSpecialization.getText().toString().trim();
        String collegeName=binding.graduationCollegeName.getText().toString().trim();
        String startYear=binding.graduationStartYear.getText().toString().trim();
        String endYear=binding.graduationEndYear.getText().toString().trim();
        String gpa=binding.gpaObtained.getText().toString().trim();
        String gpaScale="";
        if(binding.gpaOutOf10.isChecked()){
            gpaScale="10";
        }else if(binding.gpaOutOf04.isChecked()){
            gpaScale="4";
        }else if(binding.percentage.isChecked()){
            gpaScale="Percentage";
        }else{
            gpaScale="Course Requires A Pass";
        }
        String courseType="";
        if(binding.fullTime.isChecked()){
            courseType="Full Time";
        }else if(binding.partTime.isChecked()){
            courseType="Part Time";
        }else if(binding.correspondence.isChecked()){
            courseType="Correspondence";
        }

//        UGDetails ugDetails=new UGDetails(course,specialization,collegeName,courseType,gpaScale,gpa,startYear,endYear);
        GraduationDetails graduationDetails =new GraduationDetails(user.getId(),courseLevel,course,specialization,collegeName,courseType,gpaScale,gpa,startYear,endYear);
        if (user.getGraduationDetailsList() == null) {
            System.out.println("graduationDetails list is null, creating new list");
            user.setGraduationDetailsList(new ArrayList<>());
        }
        System.out.println("size of graduationDetails list before: "+user.getGraduationDetailsList().size());
        user.getGraduationDetailsList().add(graduationDetails);
        System.out.println("size of graduationDetails list after: "+user.getGraduationDetailsList().size());

        new Thread(() -> {
            db.jobDao().insertOrUpdateUser(user);
            db.jobDao().insertEducation(graduationDetails);
            runOnUiThread(() -> {
                Toast.makeText(AddEducationActivity.this, "GraduationDetails added successfully", Toast.LENGTH_SHORT).show();
                System.out.println("user details: "+user);
                finish();
            });
        }).start();
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
                    filterUnderGraduateCourses(response.body().getRecords(),"name");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("API_ERROR", "Error fetching data: " + t.getMessage());
            }
        });
    }

    private void filterUnderGraduateCourses(List<Course> records, String filed) {
        coursesList.clear();

        courses.addAll(records);

        Set<String> uniqueCourses = new HashSet<>();

        if(filed.contains("name")){
            for (Course course : records) {
                uniqueCourses.add(course.getProgramme());
//                System.out.println("course: " + course);
            }
            coursesList.addAll(uniqueCourses);
        }else {
//            System.out.println("extracting discipline");
            for (Course course : records) {
                uniqueCourses.add(course.getDiscipline());
//                System.out.println("course: " + course);
            }
            coursesList.addAll(uniqueCourses);
        }

        // Open Bottom Sheet after filtering
        BottomSheetFragment bottomSheet = new BottomSheetFragment(AddEducationActivity.this, coursesList,filed);
        bottomSheet.show(getSupportFragmentManager(), "BottomSheet");
    }

    public void setSelectedCourse(String course, String field) {
        if (field.contains("name")){
            binding.graduationCourseName.setText(course);
        }else {
            binding.courseSpecialization.setText(course);
        }
    }

    private void handleSelection(RadioButton selected, FlexboxLayout radioButtonCategory) {
        if (selected == lastSelected) {
            // User clicked the same button, so reset everything
            resetRadioButtonsSelection(radioButtonCategory);
            lastSelected = null;
            courseLevelSelectedRadioButton = null;
        } else {
            // New selection, show only this and hide others
            showSelectedRadioButton(selected, radioButtonCategory);
            lastSelected = selected;
            if (radioButtonCategory.getId()==binding.courseLevel.getId()) {
                courseLevelSelectedRadioButton = selected;
            }
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
                    radioButton.setVisibility(VISIBLE);
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
                            binding.gpaObtained.setVisibility(VISIBLE);
                            binding.gpaObtainedLayout.setVisibility(VISIBLE);

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
                binding.graduationEduSection.setVisibility(VISIBLE);
                break;
            case "Class XII":
                binding.intermediateEduSection.setVisibility(VISIBLE);
                break;
            case "Class X":
                binding.matriculationEduSection.setVisibility(VISIBLE);
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
                radioButton.setVisibility(VISIBLE);
                radioButton.setChecked(false);
//                System.out.println("radio button deselected: "+radioButton.getText());
                radioButton.setBackground(ContextCompat.getDrawable(this, R.drawable.gender_unselected));
                radioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }
        }
    }
}