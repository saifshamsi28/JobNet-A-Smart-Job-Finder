package com.jobnet.app.ui.applications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.repository.JobNetRepository;

import java.util.List;
import java.util.Locale;

public class ApplicationsFragment extends Fragment {

    private JobNetRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_applications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());

        view.findViewById(R.id.btn_back_applications).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        loadApplications(view);
    }

    private void loadApplications(View view) {
        LinearLayout container = view.findViewById(R.id.container_applications);
        TextView emptyView = view.findViewById(R.id.tv_no_applications);

        repository.loadMyApplications(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<ApplicationDto> data) {
                if (!isAdded()) {
                    return;
                }
                container.removeAllViews();
                if (data == null || data.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    return;
                }
                emptyView.setVisibility(View.GONE);
                LayoutInflater inflater = LayoutInflater.from(requireContext());
                for (ApplicationDto application : data) {
                    View item = inflater.inflate(R.layout.item_application, container, false);
                    bindApplicationItem(item, application);
                    container.addView(item);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                container.removeAllViews();
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void bindApplicationItem(View item, ApplicationDto application) {
        TextView title = item.findViewById(R.id.tv_app_job_title);
        TextView company = item.findViewById(R.id.tv_app_company);
        TextView status = item.findViewById(R.id.tv_app_status);
        TextView updated = item.findViewById(R.id.tv_app_updated);

        title.setText(defaultIfBlank(application.jobTitle, "Untitled Role"));
        company.setText(defaultIfBlank(application.company, "Unknown Company"));

        String normalizedStatus = defaultIfBlank(application.status, "APPLIED").toUpperCase(Locale.ROOT);
        status.setText(normalizedStatus.replace('_', ' '));
        updated.setText(defaultIfBlank(application.updatedAt, defaultIfBlank(application.appliedAt, "")));

        item.setOnClickListener(v -> {
            Job seed = new Job();
            seed.setId(application.jobId);
            seed.setTitle(defaultIfBlank(application.jobTitle, "Untitled Role"));
            seed.setCompany(defaultIfBlank(application.company, "Unknown Company"));
            Bundle args = new Bundle();
            args.putSerializable("job", seed);
            Navigation.findNavController(requireView()).navigate(R.id.action_applicationsFragment_to_jobDetailsFragment, args);
        });
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
