package com.jobnet.app.ui.auth;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.jobnet.app.R;
import com.jobnet.app.data.repository.AuthRepository;

public class RegisterFragment extends Fragment {

    private AuthRepository authRepository;
    private boolean isPasswordHidden = true;
    private boolean isConfirmPasswordHidden = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authRepository = AuthRepository.getInstance(requireContext());

        TextInputEditText inputName = view.findViewById(R.id.input_name);
        TextInputEditText inputUsername = view.findViewById(R.id.input_username);
        TextInputEditText inputEmail = view.findViewById(R.id.input_email);
        TextInputEditText inputPassword = view.findViewById(R.id.input_password_register);
        TextInputEditText inputConfirm = view.findViewById(R.id.input_confirm_password);
        RadioGroup roleGroup = view.findViewById(R.id.group_role);
        LinearLayout seekerCard = view.findViewById(R.id.card_role_seeker);
        LinearLayout recruiterCard = view.findViewById(R.id.card_role_recruiter);

        MaterialButton btnRegister = view.findViewById(R.id.btn_register);
        ProgressBar registerProgress = view.findViewById(R.id.register_progress);
        TextView goToLogin = view.findViewById(R.id.btn_go_login);
        TextView togglePassword = view.findViewById(R.id.btn_toggle_register_password);
        TextView toggleConfirmPassword = view.findViewById(R.id.btn_toggle_register_confirm_password);

        roleGroup.setOnCheckedChangeListener((group, checkedId) ->
            applyRoleSelectionStyle(seekerCard, recruiterCard, checkedId));
        seekerCard.setOnClickListener(v -> roleGroup.check(R.id.radio_job_seeker));
        recruiterCard.setOnClickListener(v -> roleGroup.check(R.id.radio_recruiter));
        applyRoleSelectionStyle(seekerCard, recruiterCard, roleGroup.getCheckedRadioButtonId());

        applyPasswordVisibility(inputPassword, togglePassword, isPasswordHidden);
        applyPasswordVisibility(inputConfirm, toggleConfirmPassword, isConfirmPasswordHidden);

        togglePassword.setOnClickListener(v -> {
            isPasswordHidden = !isPasswordHidden;
            applyPasswordVisibility(inputPassword, togglePassword, isPasswordHidden);
        });

        toggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordHidden = !isConfirmPasswordHidden;
            applyPasswordVisibility(inputConfirm, toggleConfirmPassword, isConfirmPasswordHidden);
        });

        btnRegister.setOnClickListener(v -> {
            String fullName = text(inputName);
            String userName = text(inputUsername);
            String email = text(inputEmail);
            String password = text(inputPassword);
            String confirm = text(inputConfirm);
                String role = roleGroup.getCheckedRadioButtonId() == R.id.radio_recruiter
                    ? "RECRUITER"
                    : "JOB_SEEKER";

            if (!password.equals(confirm)) {
                Toast.makeText(requireContext(), R.string.password_mismatch, Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true, btnRegister, registerProgress);
            authRepository.register(fullName, userName, email, password, role, new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) {
                        return;
                    }
                    setLoading(false, btnRegister, registerProgress);
                    Toast.makeText(requireContext(), R.string.register_success, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).navigate(R.id.action_registerFragment_to_loginFragment);
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) {
                        return;
                    }
                    setLoading(false, btnRegister, registerProgress);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        goToLogin.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_registerFragment_to_loginFragment));
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void setLoading(boolean loading, MaterialButton button, ProgressBar progressBar) {
        button.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        button.setText(loading ? getString(R.string.register_loading) : getString(R.string.register_title_cta));
    }

    private void applyRoleSelectionStyle(LinearLayout seekerCard, LinearLayout recruiterCard, int checkedId) {
        boolean recruiterSelected = checkedId == R.id.radio_recruiter;

        setRoleCardState(seekerCard, !recruiterSelected);
        setRoleCardState(recruiterCard, recruiterSelected);
    }

    private void setRoleCardState(LinearLayout card, boolean selected) {
        card.setBackgroundResource(selected ? R.drawable.bg_role_card_selected : R.drawable.bg_role_card_unselected);

        int iconColor = ContextCompat.getColor(requireContext(), selected ? R.color.primary : R.color.text_tertiary);
        int titleColor = ContextCompat.getColor(requireContext(), selected ? R.color.primary : R.color.text_secondary);

        View first = card.getChildAt(0);
        View second = card.getChildAt(1);
        if (first instanceof ImageView) {
            ((ImageView) first).setColorFilter(iconColor);
        }
        if (second instanceof TextView) {
            ((TextView) second).setTextColor(titleColor);
        }
    }

    private void applyPasswordVisibility(TextInputEditText input, TextView toggle, boolean hidden) {
        input.setTransformationMethod(hidden
                ? PasswordTransformationMethod.getInstance()
                : HideReturnsTransformationMethod.getInstance());
        toggle.setText(hidden ? R.string.show_password : R.string.hide_password);
        input.setSelection(input.getText() == null ? 0 : input.getText().length());
    }
}
