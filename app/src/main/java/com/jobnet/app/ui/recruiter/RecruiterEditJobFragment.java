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
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.model.WorkMode;
import com.jobnet.app.data.repository.JobNetRepository;

import java.util.Locale;

/**
 * Allows a recruiter to edit a posted job or toggle its open/closed status.
 * Receives "job" (Serializable Job) via Bundle arguments.
 */
public class RecruiterEditJobFragment extends Fragment {

    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private JobNetRepository repository;
    private Job currentJob;
    private boolean isJobClosed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recruiter_edit_job, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());
        applyInsets(view);
        setupDropdowns(view);

        if (getArguments() != null) {
            currentJob = (Job) getArguments().getSerializable("job");
        }

        view.findViewById(R.id.btn_back_edit_job).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        if (currentJob != null) {
            prefillFields(view);
        }

        view.findViewById(R.id.btn_save_job_edits).setOnClickListener(v ->
                saveEdits(view));

        view.findViewById(R.id.btn_toggle_job_status).setOnClickListener(v ->
                toggleJobStatus(view));
    }

    private void prefillFields(View view) {
        String status = currentJob.getStatus() == null ? "" : currentJob.getStatus().trim().toUpperCase(Locale.ROOT);
        isJobClosed = STATUS_CLOSED.equals(status);
        setText(view, R.id.input_edit_title, currentJob.getTitle());
        setText(view, R.id.input_edit_company, currentJob.getCompany());
        setText(view, R.id.input_edit_location, currentJob.getLocation());
        setText(view, R.id.input_edit_salary, currentJob.getSalary());
        setText(view, R.id.input_edit_openings, currentJob.getOpenings());
        setText(view, R.id.input_edit_job_type, currentJob.getJobType() != null ? currentJob.getJobType() : EmploymentType.FULL_TIME.label());
        setText(view, R.id.input_edit_work_mode, currentJob.getWorkMode() != null ? currentJob.getWorkMode() : WorkMode.ONSITE.label());
        setText(view, R.id.input_edit_short_desc, currentJob.getDescription());
        setText(view, R.id.input_edit_full_desc, currentJob.getDescription());
        updateLocationInputForWorkMode(view, getText(view, R.id.input_edit_work_mode));
        updateToggleButton(view);
    }

    private void saveEdits(View view) {
        if (currentJob == null) {
            Toast.makeText(requireContext(), "No job loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = getText(view, R.id.input_edit_title);
        String company = getText(view, R.id.input_edit_company);
        String location = getText(view, R.id.input_edit_location);
        String salary = getText(view, R.id.input_edit_salary);
        String openings = getText(view, R.id.input_edit_openings);
        String employmentType = getText(view, R.id.input_edit_job_type);
        String workMode = getText(view, R.id.input_edit_work_mode);
        String shortDesc = getText(view, R.id.input_edit_short_desc);
        String fullDesc = getText(view, R.id.input_edit_full_desc);

        if (title.isEmpty() || company.isEmpty() || location.isEmpty()) {
            Toast.makeText(requireContext(), "Title, company, and location are required", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialButton saveBtn = view.findViewById(R.id.btn_save_job_edits);
        ProgressBar progress = view.findViewById(R.id.progress_edit_job);
        setLoading(true, saveBtn, progress);

        repository.updateRecruiterJob(
                currentJob.getId(),
            title, company, location, salary, openings, shortDesc, fullDesc, employmentType, workMode,
                new JobNetRepository.DataCallback<>() {
                    @Override
                    public void onSuccess(com.jobnet.app.data.model.Job data) {
                        if (!isAdded()) return;
                        setLoading(false, saveBtn, progress);
                        Toast.makeText(requireContext(), "Job updated successfully", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(view).navigateUp();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (!isAdded()) return;
                        setLoading(false, saveBtn, progress);
                        String msg = throwable.getMessage() != null
                                ? throwable.getMessage()
                                : "Failed to update job";
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void toggleJobStatus(View view) {
        if (currentJob == null) return;
        String newStatus = isJobClosed ? STATUS_PUBLISHED : STATUS_CLOSED;

        MaterialButton saveBtn = view.findViewById(R.id.btn_save_job_edits);
        ProgressBar progress = view.findViewById(R.id.progress_edit_job);
        setLoading(true, saveBtn, progress);

        repository.updateJobStatus(currentJob.getId(), newStatus, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(com.jobnet.app.data.model.Job data) {
                if (!isAdded()) return;
                setLoading(false, saveBtn, progress);
                isJobClosed = STATUS_CLOSED.equals(newStatus);
                currentJob.setStatus(newStatus);
                updateToggleButton(view);
                String label = isJobClosed ? "Job closed" : "Job reopened";
                Toast.makeText(requireContext(), label, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) return;
                setLoading(false, saveBtn, progress);
                String msg = throwable.getMessage() != null
                        ? throwable.getMessage()
                        : "Failed to update job status";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateToggleButton(View view) {
        TextView btn = view.findViewById(R.id.btn_toggle_job_status);
        if (isJobClosed) {
            btn.setText(getString(R.string.open_job));
            btn.setTextColor(requireContext().getColor(R.color.success));
            btn.setBackgroundResource(R.drawable.bg_tag_green);
        } else {
            btn.setText(getString(R.string.close_job));
            btn.setTextColor(requireContext().getColor(R.color.error));
            btn.setBackgroundResource(R.drawable.bg_tag_error);
        }
    }

    private void setLoading(boolean loading, MaterialButton saveBtn, ProgressBar progress) {
        saveBtn.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveBtn.setText(loading ? "Saving..." : "Save Changes");
    }

    private void setText(View root, int viewId, String value) {
        TextView textView = root.findViewById(viewId);
        if (textView != null && value != null) {
            textView.setText(value);
        }
    }

    private String getText(View root, int viewId) {
        TextView textView = root.findViewById(viewId);
        if (textView == null || textView.getText() == null) return "";
        return textView.getText().toString().trim();
    }

    private void setupDropdowns(View root) {
        MaterialAutoCompleteTextView employmentType = root.findViewById(R.id.input_edit_job_type);
        MaterialAutoCompleteTextView workMode = root.findViewById(R.id.input_edit_work_mode);
        if (employmentType == null || workMode == null) {
            return;
        }

        employmentType.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new String[]{
                        EmploymentType.FULL_TIME.label(),
                        EmploymentType.PART_TIME.label(),
                        EmploymentType.INTERNSHIP.label()
                }
        ));

        workMode.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new String[]{
                        WorkMode.ONSITE.label(),
                        WorkMode.HYBRID.label(),
                        WorkMode.REMOTE.label()
                }
        ));

        workMode.setOnItemClickListener((parent, view, position, id) ->
                updateLocationInputForWorkMode(root, getText(root, R.id.input_edit_work_mode)));
    }

    private void updateLocationInputForWorkMode(View root, String modeValue) {
        TextInputEditText location = root.findViewById(R.id.input_edit_location);
        if (location == null) {
            return;
        }

        WorkMode mode = WorkMode.from(modeValue);
        if (mode == WorkMode.REMOTE) {
            location.setEnabled(false);
            location.setText("Remote (Anywhere)");
            return;
        }
        boolean wasRemoteAny = "Remote (Anywhere)".equalsIgnoreCase(location.getText() == null ? "" : location.getText().toString().trim());
        location.setEnabled(true);
        if (wasRemoteAny) {
            location.setText("");
        }
    }

    private void applyInsets(View root) {
        View toolbar = root.findViewById(R.id.toolbar_edit_job);
        View actionBar = root.findViewById(R.id.edit_job_action_bar);
        View scroll = root.findViewById(R.id.edit_job_scroll);
        final int toolbarTop = toolbar.getPaddingTop();
        final int actionBottom = actionBar.getPaddingBottom();
        final int scrollBottom = scroll.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(toolbar.getPaddingLeft(), toolbarTop + bars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            actionBar.setPadding(actionBar.getPaddingLeft(), actionBar.getPaddingTop(), actionBar.getPaddingRight(), actionBottom + bars.bottom);
            scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(), scroll.getPaddingRight(), scrollBottom + bars.bottom + dp(86));
            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }
}
