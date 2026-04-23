package com.jobnet.app.ui.recruiter;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.jobnet.app.R;
import com.jobnet.app.data.network.dto.UserDto;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.data.session.SessionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RecruiterEditProfileFragment extends Fragment {

    private JobNetRepository repository;
    private SessionManager sessionManager;
    private boolean skipNextResumeRefresh = true;
    private int profileRequestVersion = 0;

    private TextInputEditText inputName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private TextInputEditText inputSkills;
    private MaterialButton btnSave;
    private ProgressBar progressSave;

    private final List<String> initialSkills = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recruiter_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = JobNetRepository.getInstance(requireContext());
        sessionManager = new SessionManager(requireContext());

        inputName = view.findViewById(R.id.input_recruiter_profile_name);
        inputEmail = view.findViewById(R.id.input_recruiter_profile_email);
        inputPhone = view.findViewById(R.id.input_recruiter_profile_phone);
        inputSkills = view.findViewById(R.id.input_recruiter_profile_skills);
        btnSave = view.findViewById(R.id.btn_save_recruiter_profile);
        progressSave = view.findViewById(R.id.progress_save_recruiter_profile);

        applyInsets(view);
        prefillFromSession();
        loadProfile();

        view.findViewById(R.id.btn_back_recruiter_edit_profile).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        btnSave.setOnClickListener(v -> submitChanges());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        loadProfile();
    }

    @Override
    public void onDestroyView() {
        profileRequestVersion++;
        super.onDestroyView();
    }

    private void prefillFromSession() {
        String userName = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();

        if (!isBlank(userName)) {
            inputName.setText(userName);
        }
        if (!isBlank(email)) {
            inputEmail.setText(email);
        }
    }

    private void loadProfile() {
        final int requestVersion = ++profileRequestVersion;
        repository.fetchProfile(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(UserDto data) {
                if (!isAdded() || requestVersion != profileRequestVersion || data == null) {
                    return;
                }

                String resolvedName = firstNonBlank(data.name, data.userName, sessionManager.getUserName());
                if (!isBlank(resolvedName)) {
                    inputName.setText(resolvedName);
                }
                if (!isBlank(data.email)) {
                    inputEmail.setText(data.email);
                }
                if (!isBlank(data.phoneNumber)) {
                    inputPhone.setText(data.phoneNumber);
                }

                initialSkills.clear();
                if (data.skills != null) {
                    for (String skill : data.skills) {
                        String cleaned = sanitize(skill);
                        if (cleaned != null) {
                            initialSkills.add(cleaned);
                        }
                    }
                }

                if (!initialSkills.isEmpty()) {
                    inputSkills.setText(TextUtils.join(", ", initialSkills));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                // Keep session-prefilled values if network fetch fails.
            }
        });
    }

    private void submitChanges() {
        String name = textOf(inputName);
        String email = textOf(inputEmail);
        String phone = textOf(inputPhone);
        List<String> skills = parseSkills(textOf(inputSkills));

        if (name.isEmpty()) {
            inputName.setError(getString(R.string.recruiter_edit_profile_invalid_name));
            inputName.requestFocus();
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError(getString(R.string.recruiter_edit_profile_invalid_email));
            inputEmail.requestFocus();
            return;
        }

        setLoading(true);
        repository.updateProfile(name, email, phone, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(UserDto data) {
                if (!isAdded()) {
                    return;
                }

                if (skillsEqual(initialSkills, skills)) {
                    completeSuccess();
                    return;
                }

                repository.updateSkills(skills, new JobNetRepository.DataCallback<>() {
                    @Override
                    public void onSuccess(Boolean updated) {
                        if (!isAdded()) {
                            return;
                        }
                        completeSuccess();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (!isAdded()) {
                            return;
                        }
                        setLoading(false);
                        String message = throwable != null && throwable.getMessage() != null
                                ? throwable.getMessage()
                                : getString(R.string.recruiter_edit_profile_save_failed);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                String message = throwable != null && throwable.getMessage() != null
                        ? throwable.getMessage()
                        : getString(R.string.recruiter_edit_profile_save_failed);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void completeSuccess() {
        setLoading(false);
        Toast.makeText(requireContext(), R.string.recruiter_edit_profile_saved, Toast.LENGTH_SHORT).show();
        View view = getView();
        if (view != null) {
            Navigation.findNavController(view).navigateUp();
        }
    }

    private List<String> parseSkills(String raw) {
        List<String> result = new ArrayList<>();
        if (isBlank(raw)) {
            return result;
        }

        String[] tokens = raw.split(",");
        Set<String> seen = new HashSet<>();
        for (String token : tokens) {
            String cleaned = sanitize(token);
            if (cleaned == null) {
                continue;
            }
            String key = cleaned.toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                result.add(cleaned);
            }
        }
        return result;
    }

    private boolean skillsEqual(List<String> first, List<String> second) {
        LinkedHashSet<String> a = normalizeSkills(first);
        LinkedHashSet<String> b = normalizeSkills(second);
        return a.equals(b);
    }

    private LinkedHashSet<String> normalizeSkills(List<String> source) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (source == null) {
            return normalized;
        }
        for (String value : source) {
            String cleaned = sanitize(value);
            if (cleaned != null) {
                normalized.add(cleaned.toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private String textOf(TextInputEditText input) {
        if (input == null || input.getText() == null) {
            return "";
        }
        return input.getText().toString().trim();
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private void setLoading(boolean loading) {
        btnSave.setEnabled(!loading);
        progressSave.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setText(loading ? R.string.recruiter_edit_profile_saving : R.string.recruiter_edit_profile_save);
    }

    private void applyInsets(View root) {
        View toolbar = root.findViewById(R.id.toolbar_recruiter_edit_profile);
        View actionBar = root.findViewById(R.id.recruiter_edit_profile_action_bar);
        View scroll = root.findViewById(R.id.recruiter_edit_profile_scroll);

        final int toolbarTop = toolbar.getPaddingTop();
        final int actionBottom = actionBar.getPaddingBottom();
        final int scrollBottom = scroll.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    toolbarTop + bars.top,
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom()
            );
            actionBar.setPadding(
                    actionBar.getPaddingLeft(),
                    actionBar.getPaddingTop(),
                    actionBar.getPaddingRight(),
                    actionBottom + bars.bottom
            );
            scroll.setPadding(
                    scroll.getPaddingLeft(),
                    scroll.getPaddingTop(),
                    scroll.getPaddingRight(),
                    scrollBottom + bars.bottom + dp(92)
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }
}
