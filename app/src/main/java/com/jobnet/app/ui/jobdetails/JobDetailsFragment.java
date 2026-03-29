package com.jobnet.app.ui.jobdetails;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.flexbox.FlexboxLayout;
import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.repository.JobNetRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JobDetailsFragment extends Fragment {

    private Job job;
    private JobNetRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_job_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());

        if (getArguments() != null) {
            job = (Job) getArguments().getSerializable("job");
        }

        applyInsets(view);

        if (job != null) {
            populateUI(view);
            loadFullDetails(view);
            loadApplicationStatus(view);
        }

        view.findViewById(R.id.iv_back).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        view.findViewById(R.id.btn_apply_now).setOnClickListener(v -> applyToJob(view));

        view.findViewById(R.id.iv_share).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Share link copied!", Toast.LENGTH_SHORT).show());

        ImageView ivBookmark = view.findViewById(R.id.iv_detail_bookmark);
        applyBookmarkAlpha(ivBookmark);

        View.OnClickListener saveClick = v -> toggleSave(ivBookmark);
        view.findViewById(R.id.btn_save_detail).setOnClickListener(saveClick);
        ivBookmark.setOnClickListener(saveClick);
    }

    private void populateUI(View view) {
        ((TextView) view.findViewById(R.id.tv_detail_title)).setText(defaultIfBlank(job.getTitle(), "Untitled Role"));
        ((TextView) view.findViewById(R.id.tv_detail_company)).setText(defaultIfBlank(job.getCompany(), "Unknown Company"));
        ((TextView) view.findViewById(R.id.tv_detail_salary)).setText(defaultIfBlank(job.getSalary(), getString(R.string.not_available)));
        ((TextView) view.findViewById(R.id.tv_detail_location)).setText(defaultIfBlank(job.getLocation(), getString(R.string.not_available)));
        ((TextView) view.findViewById(R.id.tv_detail_type)).setText(defaultIfBlank(job.getType(), getString(R.string.not_available)));
        ((TextView) view.findViewById(R.id.tv_company_name_detail)).setText(defaultIfBlank(job.getCompany(), "Unknown Company"));
        TextView descriptionView = view.findViewById(R.id.tv_description);
        String description = safe(job.getDescription());
        descriptionView.setText(description.isEmpty() ? getString(R.string.not_available) : description);

        LinearLayout llReq = view.findViewById(R.id.ll_requirements);
        llReq.removeAllViews();
        List<String> requirements = buildRequirements(job);
        for (String req : requirements) {
            View reqView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_requirement, llReq, false);
            ((TextView) reqView.findViewById(R.id.tv_requirement)).setText(req);
            llReq.addView(reqView);
        }

        FlexboxLayout flexSkills = view.findViewById(R.id.flex_skills);
        flexSkills.removeAllViews();
        List<String> skills = buildSkills(job);
        for (String skill : skills) {
            TextView tag = (TextView) LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_skill_tag, flexSkills, false);
            tag.setText(skill);
            flexSkills.addView(tag);
        }
    }

    private void loadFullDetails(View view) {
        repository.loadJobDetails(job, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(Job data) {
                if (!isAdded() || data == null) {
                    return;
                }
                job = data;
                populateUI(view);
            }

            @Override
            public void onError(Throwable throwable) {
                // Keep showing seed data.
            }
        });
    }

    private void toggleSave(ImageView ivBookmark) {
        if (job == null) {
            return;
        }

        boolean wantToSave = !job.isSaved();
        repository.toggleSave(job, wantToSave, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(Boolean data) {
                if (!isAdded()) {
                    return;
                }
                job.setSaved(wantToSave);
                applyBookmarkAlpha(ivBookmark);
                Toast.makeText(requireContext(), wantToSave ? "Saved!" : "Removed from saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                applyBookmarkAlpha(ivBookmark);
            }
        });
    }

    private void applyToJob(View view) {
        if (job == null) {
            return;
        }
        repository.applyToJob(job, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(ApplicationDto data) {
                if (!isAdded()) {
                    return;
                }
                updateApplicationStatusUI(view, data);
                Toast.makeText(requireContext(), R.string.apply_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), R.string.apply_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadApplicationStatus(View view) {
        if (job == null || safe(job.getId()).isEmpty()) {
            return;
        }
        repository.loadMyApplicationForJob(job.getId(), new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(ApplicationDto data) {
                if (!isAdded()) {
                    return;
                }
                updateApplicationStatusUI(view, data);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                updateApplicationStatusUI(view, null);
            }
        });
    }

    private void updateApplicationStatusUI(View view, ApplicationDto application) {
        TextView statusText = view.findViewById(R.id.tv_application_status);
        TextView applyLabel = view.findViewById(R.id.tv_apply_label);

        if (application == null || safe(application.status).isEmpty()) {
            statusText.setText(R.string.application_status_none);
            applyLabel.setText(R.string.apply_now_label);
            return;
        }

        String status = application.status.toUpperCase(Locale.ROOT).replace('_', ' ');
        statusText.setText(getString(R.string.application_status_format, status));
        applyLabel.setText(R.string.applied_label);
    }

    private void applyBookmarkAlpha(ImageView ivBookmark) {
        boolean saved = job != null && job.isSaved();
        ivBookmark.setImageResource(saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
//        btnSaveDetailBookMark.setImageResource(saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);

        ImageViewCompat.setImageTintList(ivBookmark, null);
        if (!saved) {
            ivBookmark.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            ivBookmark.clearColorFilter();
        }
        ivBookmark.setAlpha(1.0f);
    }

    private List<String> buildRequirements(Job source) {
        List<String> requirements = new ArrayList<>();
        String body = safe(source == null ? null : source.getDescription());
        if (!body.isEmpty()) {
            String normalized = body.replace("\r", "\n");
            String[] parts = normalized.split("[\\n\\.] ");
            for (String part : parts) {
                String line = part.trim();
                if (line.length() < 18) {
                    continue;
                }
                if (!line.endsWith(".")) {
                    line = line + ".";
                }
                requirements.add(line);
                if (requirements.size() == 5) {
                    break;
                }
            }
        }
        if (requirements.isEmpty()) {
            requirements.add(getString(R.string.job_requirements_unavailable));
        }
        return requirements;
    }

    private List<String> buildSkills(Job source) {
        Set<String> unique = new LinkedHashSet<>();
        if (source != null) {
            addWords(unique, source.getTitle());
            addWords(unique, source.getJobType());
            addWords(unique, source.getWorkMode());
        }

        String description = safe(source == null ? null : source.getDescription()).toLowerCase(Locale.ROOT);
        if (description.contains("java")) unique.add("Java");
        if (description.contains("spring")) unique.add("Spring");
        if (description.contains("react")) unique.add("React");
        if (description.contains("figma")) unique.add("Figma");
        if (description.contains("android")) unique.add("Android");
        if (description.contains("ui")) unique.add("UI");
        if (description.contains("ux")) unique.add("UX");

        if (unique.isEmpty()) {
            unique.add("Communication");
            unique.add("Teamwork");
            unique.add("Problem Solving");
        }

        List<String> skills = new ArrayList<>();
        for (String value : unique) {
            String skill = value.trim();
            if (skill.length() < 2 || skill.length() > 20) {
                continue;
            }
            skills.add(skill);
            if (skills.size() == 8) {
                break;
            }
        }
        return skills;
    }

    private void addWords(Set<String> set, String value) {
        String safeValue = safe(value);
        if (safeValue.isEmpty()) {
            return;
        }
        String[] parts = safeValue.split("[^A-Za-z0-9+#]+");
        for (String part : parts) {
            if (part.length() >= 3 && part.length() <= 16) {
                set.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
    }

    private void applyInsets(View root) {
        View hero = root.findViewById(R.id.hero_content);
        View toolbar = root.findViewById(R.id.toolbar);
        View scroll = root.findViewById(R.id.details_scroll);
        View bottom = root.findViewById(R.id.bottom_action_bar);

        final int heroTop = hero.getPaddingTop();
        final int toolbarTop = toolbar.getPaddingTop();
        final int scrollBottom = scroll.getPaddingBottom();
        final int bottomPadding = bottom.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            hero.setPadding(hero.getPaddingLeft(), heroTop + bars.top, hero.getPaddingRight(), hero.getPaddingBottom());
            toolbar.setPadding(toolbar.getPaddingLeft(), toolbarTop + bars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(), scroll.getPaddingRight(), scrollBottom + bars.bottom + dp(68));
            bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(), bottomPadding + bars.bottom);
            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        String candidate = safe(value);
        return candidate.isEmpty() ? fallback : candidate;
    }
}
