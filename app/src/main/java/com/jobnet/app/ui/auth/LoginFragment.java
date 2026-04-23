package com.jobnet.app.ui.auth;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.jobnet.app.R;
import com.jobnet.app.data.repository.AuthRepository;
import com.jobnet.app.data.session.SessionManager;

import java.util.Locale;

public class LoginFragment extends Fragment {

    private AuthRepository authRepository;
    private boolean isPasswordHidden = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = AuthRepository.getInstance(requireContext());
        if (authRepository.session().hasSession()) {
            navigateByRole(view);
            return;
        }

        TextInputEditText inputIdentifier = view.findViewById(R.id.input_identifier);
        TextInputEditText inputPassword = view.findViewById(R.id.input_password);
        MaterialButton btnLogin = view.findViewById(R.id.btn_login);
        MaterialButton btnGoogle = view.findViewById(R.id.btn_google_placeholder);
        TextView goToRegister = view.findViewById(R.id.btn_go_register);
        ProgressBar loginProgress = view.findViewById(R.id.login_progress);
        TextView togglePassword = view.findViewById(R.id.btn_toggle_password);

        applyPasswordVisibility(inputPassword, togglePassword);
        togglePassword.setOnClickListener(v -> {
            isPasswordHidden = !isPasswordHidden;
            applyPasswordVisibility(inputPassword, togglePassword);
        });

        btnLogin.setOnClickListener(v -> {
            String identifier = inputIdentifier.getText() == null ? "" : inputIdentifier.getText().toString().trim();
            String password = inputPassword.getText() == null ? "" : inputPassword.getText().toString();

            setLoading(true, btnLogin, loginProgress);
            authRepository.login(identifier, password, new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) {
                        return;
                    }
                    setLoading(false, btnLogin, loginProgress);
                    navigateByRole(view);
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) {
                        return;
                    }
                    setLoading(false, btnLogin, loginProgress);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnGoogle.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Social login will be available soon", Toast.LENGTH_SHORT).show());

        goToRegister.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment));
    }

    private void setLoading(boolean loading, MaterialButton button, ProgressBar progressBar) {
        button.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        button.setText(loading ? "" : getString(R.string.login_title_cta));
    }

    private void navigateByRole(@NonNull View view) {
        SessionManager sessionManager = authRepository.session();
        String role = sessionManager.getUserRole();
        if (role != null && role.toUpperCase(Locale.ROOT).contains("RECRUITER")) {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_recruiterDashboardFragment);
            return;
        }
        Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_homeFragment);
    }

    private void applyPasswordVisibility(TextInputEditText inputPassword, TextView toggleView) {
        inputPassword.setTransformationMethod(isPasswordHidden
                ? PasswordTransformationMethod.getInstance()
                : HideReturnsTransformationMethod.getInstance());
        toggleView.setText(isPasswordHidden ? R.string.show_password : R.string.hide_password);
        inputPassword.setSelection(inputPassword.getText() == null ? 0 : inputPassword.getText().length());
    }
}
