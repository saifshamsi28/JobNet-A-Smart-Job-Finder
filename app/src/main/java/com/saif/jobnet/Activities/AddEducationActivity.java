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
import com.saif.jobnet.Models.Education.Class10Details;
import com.saif.jobnet.Models.Education.Class12Details;
import com.saif.jobnet.Models.Education.GraduationDetails;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.R;
import com.saif.jobnet.Utils.SimpleTextWatcher;
import com.saif.jobnet.databinding.ActivityAddEducationBinding;

import java.util.ArrayList;
import java.util.Calendar;
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
    private List<Course> courses=new ArrayList<>();
    private Course selectedCourse;
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
                courses=db.jobDao().getAllCourses();

                //if there exists education details then set the visibility of education section
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(user.getEducationDetailsList()!=null && !user.getEducationDetailsList().isEmpty()){
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


        //graduation button click listeners
        binding.graduationCourseName.setOnClickListener(view -> {

            if(courses.isEmpty())
                fetchCourses();
            else
                filterUnderGraduateCourses(courses,"course","courseSheet");
        });
        binding.courseSpecialization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open Bottom Sheet after filtering
                filterUnderGraduateCourses(courses,"specialization", "SpecializationSheet");
            }
        });
        binding.gpaObtained.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String newText) {
                if (newText.isEmpty()) return; // Prevent crash when text is empty
                try {
                    double value = Double.parseDouble(newText); // Convert input to a number
                    double maxLimit = 10; // Default max (GPA out of 10)

                    if (binding.gpaOutOf04.isChecked()) {
                        maxLimit = 4;
                    } else if (binding.percentage.isChecked()) {
                        maxLimit = 100;
                    }

                    if (value > maxLimit) {
                        binding.gpaObtainedLayout.setError("Value cannot be greater than " + maxLimit);
                        binding.saveButton.setEnabled(false);
                    } else {
                        binding.gpaObtainedLayout.setError(null); // Clear error if valid
                        binding.saveButton.setEnabled(true);
                    }

                } catch (NumberFormatException e) {
                    binding.gpaObtained.setError("Invalid input");
                }
            }
        });

        //graduation start and end year click listeners
        binding.graduationStartYear.setOnClickListener(view -> showYearSelectionBottomSheet("start year"));
        binding.graduationEndYear.setOnClickListener(view -> showYearSelectionBottomSheet("end year"));
    }

    private void showYearSelectionBottomSheet(String field) {
        // Get Current Year
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        //add 10 year
        currentYear+=10;

        // Generate List of Years (Last 20 years)
        ArrayList<String> yearList = new ArrayList<>();
        for (int i = currentYear; i >= currentYear - 30; i--) {
            yearList.add(String.valueOf(i));
        }

        // Open Bottom Sheet
        BottomSheetFragment bottomSheet = new BottomSheetFragment(this, yearList, field);
        bottomSheet.show(getSupportFragmentManager(), "YearBottomSheet");
    }

    private void setEducationDetails(String educationSection) {
//        EducationDetails educationDetails = getEducationByLevel(educationSection);
//        System.out.println("Setting details for: " + educationSection + " -> " + educationDetails);

//        if (educationDetails == null) {
//            Toast.makeText(this, "No details found for " + educationSection, Toast.LENGTH_SHORT).show();
//            return;
//        }

        switch (educationSection) {
            case "Graduation/Diploma":
//                if () {
//                    setGraduationDetails((GraduationDetails) educationDetails);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            GraduationDetails graduationDetails=db.jobDao().getGraduationDetailsByUserId(user.getId());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setGraduationDetails(graduationDetails);
                                }
                            });
                        }
                    }).start();
//                }
                break;
            case "Class XII":
//                if (educationDetails instanceof Class12Details) {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Class12Details class12Details=db.jobDao().getGraduationDetailsByUserId(user.getId());
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                setGraduationDetails(graduationDetails);
//                            }
//                        });
//                    }
//                }).start();
//                }
                break;
            case "Class X":
//                if (educationDetails instanceof Class10Details) {
//                    setClass10Details((Class10Details) educationDetails);
//                }
                break;
        }
    }

    // Get EducationDetails by Level
//    private EducationDetails getEducationByLevel(String level) {
//        if (user == null || user.getEducationDetailsList() == null) return null;
//
//        for (EducationDetails edu : user.getEducationDetailsList()) {
//            System.out.println("leve="+level+", edu found in user: "+edu);
//            if (edu.getEducationLevel().equals(level) || edu.getEducationLevel().contains(level)) {
//                return edu; // Found the education object
//            }else {
//                Toast.makeText(this, "Education level missing", Toast.LENGTH_SHORT).show();
//            }
//        }
//        return null; // No matching education found
//    }

    // Set Graduation Details
    private void setGraduationDetails(GraduationDetails graduationDetails) {
        binding.graduationEduSection.setVisibility(View.VISIBLE);

        // Set Course Level
        showSelectedRadioButton(binding.graduationOrDiploma, binding.courseLevel);

        // Set Course Details
        binding.graduationCourseName.setText(graduationDetails.getCourse());
        binding.courseSpecialization.setText(graduationDetails.getSpecialization());
        binding.graduationCollegeName.setText(graduationDetails.getCollege());

        // Set Course Type
        switch (graduationDetails.getCourseType()) {
            case "Full Time":
                showSelectedRadioButton(binding.fullTime, binding.courseTypeFlexLayout);
                break;
            case "Part Time":
                showSelectedRadioButton(binding.partTime, binding.courseTypeFlexLayout);
                break;
            case "Correspondence":
                showSelectedRadioButton(binding.correspondence, binding.courseTypeFlexLayout);
                break;
        }

        // Set GPA Scale
        switch (graduationDetails.getGpaScale()) {
            case "10":
                showSelectedRadioButton(binding.gpaOutOf10, binding.gradingSystemFlexLayout);
                break;
            case "4":
                showSelectedRadioButton(binding.gpaOutOf04, binding.gradingSystemFlexLayout);
                break;
            case "Percentage":
                showSelectedRadioButton(binding.percentage, binding.gradingSystemFlexLayout);
                break;
            default:
                showSelectedRadioButton(binding.courseRequiresAPass, binding.gradingSystemFlexLayout);
        }

        // Set CGPA
        binding.gpaObtained.setText(graduationDetails.getCgpaObtained());

        // Set Years
        binding.graduationStartYear.setText(graduationDetails.getEnrollmentYear());
        binding.graduationEndYear.setText(graduationDetails.getPassingYear());
    }

    private void setClass12Details(Class12Details class12Details) {
        binding.intermediateEduSection.setVisibility(View.VISIBLE);

        binding.boardName12th.setText(class12Details.getBoard());
        binding.schoolName12th.setText(class12Details.getSchoolName());
        binding.schoolMedium12th.setText(class12Details.getMedium());
        binding.stream12th.setText(class12Details.getStream());
        binding.marks12th.setText(class12Details.getTotalMarks());
        binding.englishMarks12th.setText(class12Details.getEnglishMarks());
        binding.mathsMarks12th.setText(class12Details.getMathsMarks());
        binding.passOutYear12th.setText(class12Details.getPassingYear());
    }

    private void setClass10Details(Class10Details class10Details) {
        binding.matriculationEduSection.setVisibility(View.VISIBLE);

        binding.boardName10th.setText(class10Details.getBoard());
        binding.schoolMedium10th.setText(class10Details.getMedium());
        binding.marks10th.setText(class10Details.getMarks());
        binding.passOutYear10th.setText(class10Details.getPassingYear());
        binding.schoolName10th.setText(class10Details.getSchoolName());
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


    //save the education details
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

        Class12Details class12Details = new Class12Details(user.getId(), "Class XII",
                board, schoolName, medium, stream, totalMarks, englishMarks, mathsMarks, passingYear);

        user.getEducationDetailsList().add(class12Details);

        new Thread(() -> {
            db.jobDao().insertOrUpdateUser(user);
            db.jobDao().insertEducation(class12Details);
            runOnUiThread(() -> Toast.makeText(AddEducationActivity.this, "Class 12 added!", Toast.LENGTH_SHORT).show());
        }).start();

    }

    private void saveGraduationDetails() {
        System.out.println("saveGraduationDetails");
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

        GraduationDetails graduationDetails = new GraduationDetails(user.getId(), "Graduation",
                course, specialization, collegeName, courseType, gpaScale, gpa, startYear, endYear);

        user.getEducationDetailsList().add(graduationDetails);

        System.out.println("saving to database");
        new Thread(() -> {
            db.jobDao().insertOrUpdateUser(user);
            db.jobDao().insertEducation(graduationDetails);
            runOnUiThread(() -> {
                Toast.makeText(AddEducationActivity.this, "Graduation added!", Toast.LENGTH_SHORT).show();
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
                    filterUnderGraduateCourses(response.body().getRecords(),"course", "courseSheet");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("API_ERROR", "Error fetching data: " + t.getMessage());
            }
        });
    }

    private void filterUnderGraduateCourses(List<Course> records, String field, String sheetTag) {
        coursesList.clear();

        courses.addAll(records);

        Set<String> uniqueCourses = new HashSet<>();

        if(field.contains("course")){
            //filter for course
            for (Course course : records) {
                uniqueCourses.add(course.getProgramme());
            }
            coursesList.addAll(uniqueCourses);
        }else {
           //filter for specialization
            for (Course course : records) {
                uniqueCourses.add(course.getDiscipline());
            }
            coursesList.addAll(uniqueCourses);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.jobDao().insertCourses(courses);
            }
        }).start();

        // Open Bottom Sheet after filtering
        BottomSheetFragment bottomSheet = new BottomSheetFragment(AddEducationActivity.this, coursesList,field);
        bottomSheet.show(getSupportFragmentManager(), sheetTag);
    }

    public void setSelectedCourse(String course, String field) {
        switch (field){
            case "course":
                binding.graduationCourseName.setText(course);
                break;
            case "specialization":
                binding.courseSpecialization.setText(course);
                break;
            case "start year":
                binding.graduationStartYear.setText(course);
                break;
            case "end year":
                binding.graduationEndYear.setText(course);
                break;
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
//                        System.out.println("selected radio button category: "+radioButtonCategory.getChildAt(0));
                        showEducationSection(radioButton);
                    } else if (radioButtonCategory.getId()==binding.gradingSystemFlexLayout.getId()) {
//                        System.out.println("called grading flex");
                        //if none of the grading system radio button is selected, hide GPA obtained layout
                        if (binding.gradingSystemFlexLayout.isSelected()) {
                            binding.gpaObtainedLayout.setVisibility(View.GONE);
                            binding.gpaObtained.setVisibility(View.GONE);
                        }else {
                            if(!selected.getText().toString().contains("Course require")){
                                binding.gpaObtained.setVisibility(VISIBLE);
                                binding.gpaObtainedLayout.setVisibility(VISIBLE);

                                // Set the hint on TextInputLayout, not on EditText
                                binding.gpaObtainedLayout.setHint(selected.getText().toString()+"*");

                                binding.gpaObtainedLayout.setHintAnimationEnabled(true);
                                binding.gpaObtainedLayout.setHintEnabled(true);
                            }
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