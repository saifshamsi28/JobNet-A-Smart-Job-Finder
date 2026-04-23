package com.jobnet.app.ui.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.jobnet.app.R;

public class ProfileSettingsFragment extends Fragment {

    private static final String PREFS = "jobnet_profile_settings";
    private static final String KEY_PUSH = "push_notifications";
    private static final String KEY_EMAIL = "email_updates";
    private static final String KEY_ALERTS = "job_alerts";
    private static final String KEY_PRIVATE = "private_profile";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyInsets(view);

        SwitchMaterial switchPush = view.findViewById(R.id.switch_settings_push);
        SwitchMaterial switchEmail = view.findViewById(R.id.switch_settings_email);
        SwitchMaterial switchAlerts = view.findViewById(R.id.switch_settings_job_alerts);
        SwitchMaterial switchPrivate = view.findViewById(R.id.switch_settings_private_profile);
        MaterialButton saveButton = view.findViewById(R.id.btn_save_profile_settings);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, 0);
        switchPush.setChecked(prefs.getBoolean(KEY_PUSH, true));
        switchEmail.setChecked(prefs.getBoolean(KEY_EMAIL, true));
        switchAlerts.setChecked(prefs.getBoolean(KEY_ALERTS, true));
        switchPrivate.setChecked(prefs.getBoolean(KEY_PRIVATE, false));

        view.findViewById(R.id.btn_back_profile_settings).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        saveButton.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean(KEY_PUSH, switchPush.isChecked())
                    .putBoolean(KEY_EMAIL, switchEmail.isChecked())
                    .putBoolean(KEY_ALERTS, switchAlerts.isChecked())
                    .putBoolean(KEY_PRIVATE, switchPrivate.isChecked())
                    .apply();
            Toast.makeText(requireContext(), "Settings updated", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
        });
    }

    private void applyInsets(View root) {
        View toolbar = root.findViewById(R.id.settings_toolbar);
        final int toolbarTop = toolbar.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(toolbar.getPaddingLeft(), toolbarTop + bars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            return insets;
        });
    }
}
