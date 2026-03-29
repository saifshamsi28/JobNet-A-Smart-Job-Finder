package com.jobnet.app.ui.profile;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.dto.UserDto;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.R;

import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

        private JobNetRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());
        applyInsets(view);
        hideStaticSearchSection(view);
        bindSessionIdentity(view);

        ProgressBar progressRing = view.findViewById(R.id.progress_profile);
        ObjectAnimator animator = ObjectAnimator.ofInt(progressRing, "progress", 0, 72);
        animator.setDuration(1200);
        animator.start();

        loadProfile(view);
        loadProfileStats(view);
        loadTopRecommendation(view);

        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Edit Profile", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btn_upload_resume).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Upload Resume", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btn_settings).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Settings", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btn_my_applications).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_profileFragment_to_applicationsFragment));

                SessionManager sessionManager = new SessionManager(requireContext());
                String role = sessionManager.getUserRole();
                if (role != null && role.toUpperCase(Locale.ROOT).contains("RECRUITER")) {
                        view.findViewById(R.id.btn_my_applications).setVisibility(View.GONE);
                        TextView profileTitle = view.findViewById(R.id.tv_profile_title);
                        profileTitle.setText(R.string.recruiter_role_title);
                }

        view.findViewById(R.id.btn_logout).setOnClickListener(v ->
                {
                    repository.clearSessionData();
                    NavOptions navOptions = new NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true)
                            .build();
                    Navigation.findNavController(view)
                            .navigate(R.id.action_profileFragment_to_loginFragment, null, navOptions);
                    Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();
                });

        view.findViewById(R.id.btn_see_recommended).setOnClickListener(v ->
                Toast.makeText(requireContext(), "52 Recommended Jobs", Toast.LENGTH_SHORT).show());
    }

        private void loadProfile(View view) {
                TextView profileName = view.findViewById(R.id.tv_profile_name);
                TextView profileTitle = view.findViewById(R.id.tv_profile_title);
                TextView profileCompany = view.findViewById(R.id.tv_profile_company);

                repository.fetchProfile(new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(UserDto data) {
                                if (!isAdded() || data == null) {
                                        return;
                                }
                                if (data.name != null && !data.name.isBlank()) {
                                        profileName.setText(data.name);
                                } else if (data.userName != null && !data.userName.isBlank()) {
                                        profileName.setText(data.userName);
                                }

                                String title = null;
                                if (data.skills != null && !data.skills.isEmpty()) {
                                        title = data.skills.get(0).toUpperCase() + " PROFESSIONAL";
                                }
                                if (title == null || title.isBlank()) {
                                        title = getString(R.string.default_profile_title);
                                }
                                profileTitle.setText(title);

                                if (data.email != null && !data.email.isBlank()) {
                                        profileCompany.setText(data.email);
                                }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                                // Keep session/default values when profile fetch fails.
                        }
                });
        }

        private void bindSessionIdentity(View view) {
                SessionManager sessionManager = new SessionManager(requireContext());
                TextView profileName = view.findViewById(R.id.tv_profile_name);
                TextView profileCompany = view.findViewById(R.id.tv_profile_company);
                String userName = sessionManager.getUserName();
                String email = sessionManager.getUserEmail();

                if (userName != null && !userName.isBlank()) {
                        profileName.setText(userName);
                }
                if (email != null && !email.isBlank()) {
                        profileCompany.setText(email);
                }
        }

        private void loadTopRecommendation(View view) {
                TextView count = view.findViewById(R.id.tv_recommended_count);
                TextView title = view.findViewById(R.id.tv_recommended_title);
                TextView company = view.findViewById(R.id.tv_recommended_company);

                repository.loadHomeData(new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(JobNetRepository.HomeData data) {
                                if (!isAdded() || data == null || data.recommended == null) {
                                        return;
                                }
                                count.setText(String.valueOf(data.recommended.size()));
                                if (!data.recommended.isEmpty()) {
                                        Job top = data.recommended.get(0);
                                        title.setText(top.getTitle());
                                        company.setText(top.getCompany());
                                }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                                // Keep existing card values when recommendation fetch fails.
                        }
                });
        }

        private void loadProfileStats(View view) {
                TextView saved = view.findViewById(R.id.tv_profile_stat_saved);
                TextView recommended = view.findViewById(R.id.tv_profile_stat_recommended);
                TextView openings = view.findViewById(R.id.tv_profile_stat_openings);

                repository.loadSavedJobs(new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(List<Job> data) {
                                if (!isAdded()) {
                                        return;
                                }
                                saved.setText(String.valueOf(data == null ? 0 : data.size()));
                        }

                        @Override
                        public void onError(Throwable throwable) {
                                if (!isAdded()) {
                                        return;
                                }
                                saved.setText("0");
                        }
                });

                repository.loadHomeData(new JobNetRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(JobNetRepository.HomeData data) {
                                if (!isAdded() || data == null) {
                                        return;
                                }
                                int recommendedCount = data.recommended == null ? 0 : data.recommended.size();
                                int openingsCount = data.featured == null ? 0 : data.featured.size();
                                recommended.setText(String.valueOf(recommendedCount));
                                openings.setText(String.valueOf(openingsCount));
                        }

                        @Override
                        public void onError(Throwable throwable) {
                                if (!isAdded()) {
                                        return;
                                }
                                recommended.setText("0");
                                openings.setText("0");
                        }
                });
        }

        private void hideStaticSearchSection(View view) {
                View section = view.findViewById(R.id.section_job_searches);
                if (section != null) {
                        section.setVisibility(View.GONE);
                }
        }

        private void applyInsets(View root) {
                View header = root.findViewById(R.id.profile_header);
                View toolbar = root.findViewById(R.id.profile_toolbar_row);
                View scroll = root.findViewById(R.id.profile_scroll);

                final int headerHeight = header.getLayoutParams().height;
                final int toolbarTop = toolbar.getPaddingTop();
                final int scrollBottom = scroll.getPaddingBottom();

                ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                        Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                        ViewGroup.LayoutParams lp = header.getLayoutParams();
                        lp.height = headerHeight + bars.top;
                        header.setLayoutParams(lp);
                        toolbar.setPadding(toolbar.getPaddingLeft(), toolbarTop + bars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
                        scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(), scroll.getPaddingRight(), scrollBottom + bars.bottom);
                        return insets;
                });
        }
}
