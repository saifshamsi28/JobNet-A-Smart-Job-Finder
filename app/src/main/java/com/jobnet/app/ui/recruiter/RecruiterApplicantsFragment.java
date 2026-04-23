package com.jobnet.app.ui.recruiter;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jobnet.app.R;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.repository.JobNetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows all applicants for a specific recruiter job.
 * Receives "jobId" and "jobTitle" via Bundle arguments.
 */
public class RecruiterApplicantsFragment extends Fragment {

    private JobNetRepository repository;
    private RecruiterApplicantsAdapter adapter;
    private final List<ApplicationDto> applicants = new ArrayList<>();
    private final List<ApplicationDto> visibleApplicants = new ArrayList<>();
    private String activeFilter = "ALL";
    private String jobId;
    private boolean skipNextResumeRefresh = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recruiter_applicants, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());
        applyInsets(view);

        // Read arguments
        if (getArguments() != null) {
            jobId = getArguments().getString("jobId", "");
            String jobTitle = getArguments().getString("jobTitle", "Job");
            ((TextView) view.findViewById(R.id.tv_applicants_job_name)).setText(jobTitle);
        }

        view.findViewById(R.id.btn_back_applicants).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        view.findViewById(R.id.btn_retry_applicants).setOnClickListener(v ->
                loadApplicants(view));

        RecyclerView recycler = view.findViewById(R.id.recycler_applicants);
        adapter = new RecruiterApplicantsAdapter(visibleApplicants, new RecruiterApplicantsAdapter.OnActionListener() {
            @Override
            public void onShortlist(ApplicationDto application, int position) {
                showConfirmDialog(
                        getString(R.string.shortlist_confirm_title),
                        getString(R.string.shortlist_confirm_message),
                        getString(R.string.shortlist_action),
                        R.drawable.bg_dialog_button_primary,
                        () -> updateStatus(view, application, "SHORTLISTED", position)
                );
            }

            @Override
            public void onReject(ApplicationDto application, int position) {
                showConfirmDialog(
                        getString(R.string.reject_confirm_title),
                        getString(R.string.reject_confirm_message),
                        getString(R.string.reject_action),
                        R.drawable.bg_dialog_button_danger,
                        () -> updateStatus(view, application, "REJECTED", position)
                );
            }
        });
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        setupFilters(view);

        loadApplicants(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        View root = getView();
        if (root != null) {
            loadApplicants(root);
        }
    }

    private void loadApplicants(View view) {
        View emptyLayout = view.findViewById(R.id.layout_empty_applicants);
        View errorLayout = view.findViewById(R.id.layout_error_applicants);
        RecyclerView recycler = view.findViewById(R.id.recycler_applicants);
        TextView countBadge = view.findViewById(R.id.tv_applicants_count_badge);

        showApplicantsSkeleton(true);
        emptyLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
        recycler.setVisibility(View.VISIBLE);

        repository.loadJobApplicants(jobId, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<ApplicationDto> data) {
                if (!isAdded()) return;
                showApplicantsSkeleton(false);
                applicants.clear();
                if (data != null) applicants.addAll(data);
                applyFilters(view);

                if (visibleApplicants.isEmpty()) {
                    emptyLayout.setVisibility(View.VISIBLE);
                    recycler.setVisibility(View.GONE);
                } else {
                    emptyLayout.setVisibility(View.GONE);
                    recycler.setVisibility(View.VISIBLE);
                }
                countBadge.setText(String.valueOf(visibleApplicants.size()));
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) return;
                showApplicantsSkeleton(false);
                recycler.setVisibility(View.GONE);
                errorLayout.setVisibility(View.VISIBLE);
                String msg = throwable.getMessage() != null
                        ? throwable.getMessage()
                        : "Could not load applicants";
                ((TextView) view.findViewById(R.id.tv_applicants_error)).setText(msg);
            }
        });
    }

    private void updateStatus(View rootView, ApplicationDto application, String newStatus, int position) {
        if (application == null || application.id == null) {
            Toast.makeText(requireContext(), "Invalid application", Toast.LENGTH_SHORT).show();
            return;
        }
        repository.updateApplicationStatus(application.id, newStatus, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(ApplicationDto data) {
                if (!isAdded()) return;
                if (data != null) {
                    for (ApplicationDto applicant : applicants) {
                        if (applicant != null && applicant.id != null && applicant.id.equals(data.id)) {
                            applicant.status = data.status;
                            applicant.updatedAt = data.updatedAt;
                            break;
                        }
                    }
                }
                applyFilters(rootView);
                String label = "SHORTLISTED".equals(newStatus)
                        ? getString(R.string.applicant_shortlisted)
                        : getString(R.string.applicant_rejected);
                Toast.makeText(requireContext(), label, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) return;
                String msg = throwable.getMessage() != null
                        ? throwable.getMessage()
                        : "Failed to update status";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showConfirmDialog(String title, String message, String confirmLabel,
                                   int confirmBgRes, Runnable onConfirm) {
        if (!isAdded()) {
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        ((TextView) dialog.findViewById(R.id.tv_dialog_title)).setText(title);
        ((TextView) dialog.findViewById(R.id.tv_dialog_message)).setText(message);

        TextView btnConfirm = dialog.findViewById(R.id.btn_dialog_confirm);
        btnConfirm.setText(confirmLabel);
        btnConfirm.setBackgroundResource(confirmBgRes);
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });

        dialog.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupFilters(View root) {
        TextView chipAll = root.findViewById(R.id.chip_applicants_all);
        TextView chipApplied = root.findViewById(R.id.chip_applicants_applied);
        TextView chipShortlisted = root.findViewById(R.id.chip_applicants_shortlisted);
        TextView chipRejected = root.findViewById(R.id.chip_applicants_rejected);

        chipAll.setOnClickListener(v -> {
            activeFilter = "ALL";
            applyChipSelection(chipAll, chipApplied, chipShortlisted, chipRejected);
            applyFilters(root);
        });
        chipApplied.setOnClickListener(v -> {
            activeFilter = "APPLIED";
            applyChipSelection(chipAll, chipApplied, chipShortlisted, chipRejected);
            applyFilters(root);
        });
        chipShortlisted.setOnClickListener(v -> {
            activeFilter = "SHORTLISTED";
            applyChipSelection(chipAll, chipApplied, chipShortlisted, chipRejected);
            applyFilters(root);
        });
        chipRejected.setOnClickListener(v -> {
            activeFilter = "REJECTED";
            applyChipSelection(chipAll, chipApplied, chipShortlisted, chipRejected);
            applyFilters(root);
        });
        applyChipSelection(chipAll, chipApplied, chipShortlisted, chipRejected);
    }

    private void applyFilters(View root) {
        visibleApplicants.clear();
        for (ApplicationDto applicant : applicants) {
            if (applicant == null) {
                continue;
            }
            String normalized = normalizeStatus(applicant.status);
            if ("ALL".equals(activeFilter) || activeFilter.equals(normalized)) {
                visibleApplicants.add(applicant);
            }
        }
        adapter.notifyDataSetChanged();

        View emptyLayout = root.findViewById(R.id.layout_empty_applicants);
        RecyclerView recycler = root.findViewById(R.id.recycler_applicants);
        TextView countBadge = root.findViewById(R.id.tv_applicants_count_badge);
        countBadge.setText(String.valueOf(visibleApplicants.size()));
        emptyLayout.setVisibility(visibleApplicants.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(visibleApplicants.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "APPLIED";
        }
        String status = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("UNDER_REVIEW".equals(status) || "IN_REVIEW".equals(status)) {
            return "REVIEWED";
        }
        return status;
    }

    private void applyChipSelection(TextView chipAll,
                                    TextView chipApplied,
                                    TextView chipShortlisted,
                                    TextView chipRejected) {
        styleChip(chipAll, "ALL".equals(activeFilter));
        styleChip(chipApplied, "APPLIED".equals(activeFilter));
        styleChip(chipShortlisted, "SHORTLISTED".equals(activeFilter));
        styleChip(chipRejected, "REJECTED".equals(activeFilter));
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(requireContext().getColor(selected ? R.color.text_chip_selected : R.color.text_chip));
    }

    private void applyInsets(View root) {
        View toolbar = root.findViewById(R.id.toolbar_applicants);
        View content = root.findViewById(R.id.content_applicants);
        final int toolbarTop = toolbar.getPaddingTop();
        final int baseContentTop = ((ViewGroup.MarginLayoutParams) content.getLayoutParams()).topMargin;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(toolbar.getPaddingLeft(), toolbarTop + bars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) content.getLayoutParams();
            lp.topMargin = baseContentTop + bars.top;
            content.setLayoutParams(lp);
            return insets;
        });
    }

    private void showApplicantsSkeleton(boolean show) {
        if (!isAdded() || adapter == null) {
            return;
        }
        adapter.showSkeleton(show);
    }

    @Override
    public void onDestroyView() {
        showApplicantsSkeleton(false);
        super.onDestroyView();
    }
}
