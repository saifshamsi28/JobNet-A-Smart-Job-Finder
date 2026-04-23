package com.jobnet.app.ui.recruiter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.jobnet.app.R;
import com.jobnet.app.data.model.EmploymentType;
import com.jobnet.app.data.model.JobCategory;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.model.WorkMode;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.data.session.RecruiterJobDraftStore;
import com.jobnet.app.util.SampleData;

import java.util.ArrayList;
import java.util.List;

public class RecruiterPostJobFragment extends Fragment {

    private JobNetRepository repository;
    private RecruiterJobDraftStore draftStore;

    private TextInputEditText inputTitle;
    private TextInputEditText inputCompany;
    private TextInputEditText inputLocation;
    private TextInputEditText inputSalary;
    private TextInputEditText inputOpenings;
    private TextInputEditText inputShortDescription;
    private TextInputEditText inputFullDescription;
    private TextInputEditText inputRequiredSkills;
    private MaterialAutoCompleteTextView inputEmploymentType;
    private MaterialAutoCompleteTextView inputWorkMode;
    private MaterialAutoCompleteTextView inputCategory;

    private MaterialButton publishButton;
    private MaterialButton saveDraftButton;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recruiter_post_job, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());
        draftStore = new RecruiterJobDraftStore(requireContext());

        inputTitle = view.findViewById(R.id.input_job_title);
        inputCompany = view.findViewById(R.id.input_job_company);
        inputLocation = view.findViewById(R.id.input_job_location);
        inputSalary = view.findViewById(R.id.input_job_salary);
        inputOpenings = view.findViewById(R.id.input_job_openings);
        inputShortDescription = view.findViewById(R.id.input_job_short_description);
        inputFullDescription = view.findViewById(R.id.input_job_full_description);
        inputRequiredSkills = view.findViewById(R.id.input_job_required_skills);
        inputEmploymentType = view.findViewById(R.id.input_job_employment_type);
        inputWorkMode = view.findViewById(R.id.input_job_work_mode);
        inputCategory = view.findViewById(R.id.input_job_category);

        publishButton = view.findViewById(R.id.btn_publish_job);
        saveDraftButton = view.findViewById(R.id.btn_save_draft);
        progressBar = view.findViewById(R.id.publish_job_progress);

        applyInsets(view);
        setupDropdowns();

        hydrateDraftIfPresent();

        view.findViewById(R.id.btn_back_post_job).setOnClickListener(v ->
            Navigation.findNavController(view).navigateUp());

        publishButton.setOnClickListener(v -> publishNow());
        saveDraftButton.setOnClickListener(v -> saveDraft());
    }

    private void hydrateDraftIfPresent() {
        RecruiterJobDraftStore.DraftJob draft = draftStore.load();
        if (draft == null) {
            return;
        }
        inputTitle.setText(safe(draft.title));
        inputCompany.setText(safe(draft.company));
        inputLocation.setText(safe(draft.location));
        inputSalary.setText(safe(draft.salary));
        inputOpenings.setText(safe(draft.openings));
        inputEmploymentType.setText(safe(draft.employmentType), false);
        inputWorkMode.setText(safe(draft.workMode), false);
        inputCategory.setText(safe(draft.category), false);
        inputRequiredSkills.setText(safe(draft.requiredSkills));
        inputShortDescription.setText(safe(draft.shortDescription));
        inputFullDescription.setText(safe(draft.fullDescription));
        updateLocationInputForWorkMode(safe(draft.workMode));
    }

    private void publishNow() {
        RecruiterJobDraftStore.DraftJob draft = collectDraftFromInputs();
        if (isBlank(draft.title) || isBlank(draft.company) || isBlank(draft.location)) {
            Toast.makeText(requireContext(), R.string.post_job_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        repository.createRecruiterJob(
                draft.title,
                draft.company,
                draft.location,
                draft.salary,
                draft.openings,
                draft.employmentType,
                draft.workMode,
                draft.category,
                parseSkills(draft.requiredSkills),
                draft.shortDescription,
                draft.fullDescription,
                "PUBLISHED",
                new JobNetRepository.DataCallback<>() {
                    @Override
                    public void onSuccess(Job data) {
                        if (!isAdded()) {
                            return;
                        }
                        setLoading(false);
                        draftStore.clear();
                        clearInputs();
                        Toast.makeText(requireContext(), R.string.create_job_success, Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).popBackStack();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (!isAdded()) {
                            return;
                        }
                        setLoading(false);
                        draftStore.save(draft);

                        String message = throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()
                                ? getString(R.string.create_job_failed)
                                : throwable.getMessage();
                        if (message.toLowerCase().contains("network")) {
                            message = message + " " + getString(R.string.draft_saved_local);
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void saveDraft() {
        RecruiterJobDraftStore.DraftJob draft = collectDraftFromInputs();
        if (draft.isEmpty()) {
            Toast.makeText(requireContext(), R.string.draft_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        draftStore.save(draft);
        Toast.makeText(requireContext(), R.string.draft_saved_local, Toast.LENGTH_SHORT).show();

        repository.createRecruiterJob(
                draft.title,
                draft.company,
                draft.location,
                draft.salary,
                draft.openings,
                draft.employmentType,
                draft.workMode,
                draft.category,
                parseSkills(draft.requiredSkills),
                draft.shortDescription,
                draft.fullDescription,
                "DRAFT",
                new JobNetRepository.DataCallback<>() {
                    @Override
                    public void onSuccess(Job data) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(), R.string.draft_synced_server, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        // Local draft is already persisted; no-op for server sync failure.
                    }
                }
        );
    }

    private RecruiterJobDraftStore.DraftJob collectDraftFromInputs() {
        RecruiterJobDraftStore.DraftJob draft = new RecruiterJobDraftStore.DraftJob();
        draft.title = text(inputTitle);
        draft.company = text(inputCompany);
        draft.location = text(inputLocation);
        draft.salary = text(inputSalary);
        draft.openings = text(inputOpenings);
        draft.employmentType = text(inputEmploymentType);
        draft.workMode = text(inputWorkMode);
        draft.category = text(inputCategory);
        draft.requiredSkills = text(inputRequiredSkills);
        draft.shortDescription = text(inputShortDescription);
        draft.fullDescription = text(inputFullDescription);
        return draft;
    }

    private void clearInputs() {
        inputTitle.setText("");
        inputCompany.setText("");
        inputLocation.setText("");
        inputSalary.setText("");
        inputOpenings.setText("");
        inputEmploymentType.setText(EmploymentType.FULL_TIME.label(), false);
        inputWorkMode.setText(WorkMode.ONSITE.label(), false);
        inputCategory.setText("Engineering", false);
        inputRequiredSkills.setText("");
        updateLocationInputForWorkMode(WorkMode.ONSITE.label());
        inputShortDescription.setText("");
        inputFullDescription.setText("");
    }

    private String text(TextView editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void setupDropdowns() {
        ArrayAdapter<String> employmentAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new String[]{
                        EmploymentType.FULL_TIME.label(),
                        EmploymentType.PART_TIME.label(),
                        EmploymentType.INTERNSHIP.label()
                }
        );
        inputEmploymentType.setAdapter(employmentAdapter);

        ArrayAdapter<String> workModeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new String[]{
                        WorkMode.ONSITE.label(),
                        WorkMode.HYBRID.label(),
                        WorkMode.REMOTE.label()
                }
        );
        inputWorkMode.setAdapter(workModeAdapter);

        List<JobCategory> categories = SampleData.getCategories();
        List<String> categoryLabels = new ArrayList<>();
        for (JobCategory category : categories) {
            if (category != null && category.getName() != null && !category.getName().isBlank()) {
                categoryLabels.add(category.getName());
            }
        }
        if (categoryLabels.isEmpty()) {
            categoryLabels.add("Engineering");
        }
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                categoryLabels
        );
        inputCategory.setAdapter(categoryAdapter);

        if (text(inputEmploymentType).isEmpty()) {
            inputEmploymentType.setText(EmploymentType.FULL_TIME.label(), false);
        }
        if (text(inputWorkMode).isEmpty()) {
            inputWorkMode.setText(WorkMode.ONSITE.label(), false);
        }
        if (text(inputCategory).isEmpty()) {
            inputCategory.setText(categoryLabels.get(0), false);
        }
        updateLocationInputForWorkMode(text(inputWorkMode));

        inputWorkMode.setOnItemClickListener((parent, view, position, id) ->
                updateLocationInputForWorkMode(text(inputWorkMode)));
    }

    private void updateLocationInputForWorkMode(String modeLabel) {
        WorkMode mode = WorkMode.from(modeLabel);
        if (mode == WorkMode.REMOTE) {
            inputLocation.setEnabled(false);
            inputLocation.setText("Remote (Anywhere)");
            return;
        }
        boolean wasRemoteAny = "Remote (Anywhere)".equalsIgnoreCase(text(inputLocation));
        inputLocation.setEnabled(true);
        if (wasRemoteAny) {
            inputLocation.setText("");
        }
    }

    private void applyInsets(View root) {
        View header = root.findViewById(R.id.post_job_header);
        final int headerTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            header.setPadding(header.getPaddingLeft(), headerTop + bars.top, header.getPaddingRight(), header.getPaddingBottom());
            return insets;
        });
    }

    private void setLoading(boolean loading) {
        publishButton.setEnabled(!loading);
        saveDraftButton.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        publishButton.setText(loading ? getString(R.string.create_job_loading) : getString(R.string.create_job_action));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<String> parseSkills(String raw) {
        List<String> parsed = new ArrayList<>();
        if (isBlank(raw)) {
            return parsed;
        }

        String[] parts = raw.split(",");
        for (String part : parts) {
            String value = part == null ? "" : part.trim();
            if (value.isEmpty() || parsed.contains(value)) {
                continue;
            }
            parsed.add(value);
            if (parsed.size() == 12) {
                break;
            }
        }
        return parsed;
    }
}
