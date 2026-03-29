package com.jobnet.app.ui.recruiter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.repository.JobNetRepository;

public class RecruiterPostJobFragment extends Fragment {

    private JobNetRepository repository;

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

        TextInputEditText inputTitle = view.findViewById(R.id.input_job_title);
        TextInputEditText inputCompany = view.findViewById(R.id.input_job_company);
        TextInputEditText inputLocation = view.findViewById(R.id.input_job_location);
        TextInputEditText inputSalary = view.findViewById(R.id.input_job_salary);
        TextInputEditText inputShortDescription = view.findViewById(R.id.input_job_short_description);
        TextInputEditText inputFullDescription = view.findViewById(R.id.input_job_full_description);

        MaterialButton publishButton = view.findViewById(R.id.btn_publish_job);
        ProgressBar progressBar = view.findViewById(R.id.publish_job_progress);

        publishButton.setOnClickListener(v -> {
            String title = text(inputTitle);
            String company = text(inputCompany);
            String location = text(inputLocation);
            String salary = text(inputSalary);
            String shortDescription = text(inputShortDescription);
            String fullDescription = text(inputFullDescription);

            if (title.isEmpty() || company.isEmpty() || location.isEmpty()) {
                Toast.makeText(requireContext(), "Title, company, and location are required", Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true, publishButton, progressBar);
            repository.createRecruiterJob(
                    title,
                    company,
                    location,
                    salary,
                    shortDescription,
                    fullDescription,
                    new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(Job data) {
                            if (!isAdded()) {
                                return;
                            }
                            setLoading(false, publishButton, progressBar);
                            Toast.makeText(requireContext(), R.string.create_job_success, Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(view).popBackStack();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (!isAdded()) {
                                return;
                            }
                            setLoading(false, publishButton, progressBar);
                            Toast.makeText(requireContext(), R.string.create_job_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void setLoading(boolean loading, MaterialButton button, ProgressBar progressBar) {
        button.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        button.setText(loading ? getString(R.string.create_job_loading) : getString(R.string.create_job_action));
    }
}
