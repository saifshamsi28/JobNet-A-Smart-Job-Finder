package com.jobnet.app.ui.main;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.annotation.IdRes;

import com.jobnet.app.R;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.databinding.ActivityMainBinding;
import com.jobnet.app.notifications.NotificationConstants;
import com.jobnet.app.notifications.NotificationSyncScheduler;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_NOTIFICATIONS = 2201;
    private ActivityMainBinding binding;
    private NavController navController;
    private int currentTopLevelDestination = R.id.homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        applyWindowInsets();
        maybeRequestNotificationPermission();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            binding.bottomNavigation.setOnItemSelectedListener(item ->
                    navigateToTopLevel(item.getItemId()));
            binding.bottomNavigation.setOnItemReselectedListener(item -> {
                // No-op to avoid duplicate stack entries.
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                SessionManager sessionManager = new SessionManager(this);
                String role = sessionManager.getUserRole();
                boolean isRecruiter = role != null && role.toUpperCase(Locale.ROOT).contains("RECRUITER");
                boolean isTopLevel = destinationId == R.id.homeFragment
                        || destinationId == R.id.searchFragment
                        || destinationId == R.id.savedFragment
                        || destinationId == R.id.profileFragment;
                boolean isAuthScreen = destination.getId() == R.id.loginFragment
                        || destination.getId() == R.id.registerFragment;
                boolean isDetailScreen = destination.getId() == R.id.jobDetailsFragment
                    || destination.getId() == R.id.applicationsFragment
                    || destination.getId() == R.id.applicationTimelineFragment
                    || destination.getId() == R.id.editProfileFragment
                    || destination.getId() == R.id.categoriesFragment
                    || destination.getId() == R.id.categoryJobsFragment
                    || destination.getId() == R.id.profileSettingsFragment;
                boolean isRecruiterScreen = destination.getId() == R.id.recruiterDashboardFragment
                    || destination.getId() == R.id.recruiterPostJobFragment
                    || destination.getId() == R.id.recruiterEditJobFragment
                    || destination.getId() == R.id.recruiterApplicantsFragment
                    || destination.getId() == R.id.recruiterAllJobsFragment
                    || destination.getId() == R.id.notificationsFragment
                    || destination.getId() == R.id.recruiterProfileFragment
                    || (isRecruiter && destination.getId() == R.id.profileFragment);

                if (isTopLevel && !isRecruiterScreen) {
                    currentTopLevelDestination = destinationId;
                    binding.bottomNavigation.setSelectedItemId(destinationId);
                }

                applyStatusBarAppearance(destinationId, isAuthScreen);

                binding.bottomNavContainer.setVisibility(isAuthScreen ? View.GONE : View.VISIBLE);
                if (isDetailScreen || isRecruiterScreen) {
                    binding.bottomNavContainer.setVisibility(View.GONE);
                }
            });

            enforceAuthGate();
            enforceRoleLandingForActiveSession();
            configureBackgroundNotificationSync();
            handleNotificationIntent(getIntent());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureBackgroundNotificationSync();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void applyStatusBarAppearance(int destinationId, boolean isAuthScreen) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), binding.getRoot());
        if (controller == null) {
            return;
        }

        boolean darkHeaderDestination = isAuthScreen
                || destinationId == R.id.recruiterDashboardFragment
                || destinationId == R.id.recruiterPostJobFragment
                || destinationId == R.id.recruiterEditJobFragment
                || destinationId == R.id.recruiterApplicantsFragment
                || destinationId == R.id.recruiterAllJobsFragment
                || destinationId == R.id.recruiterProfileFragment
                || destinationId == R.id.applicationsFragment
                || destinationId == R.id.applicationTimelineFragment
                || destinationId == R.id.editProfileFragment
                || destinationId == R.id.categoriesFragment
                || destinationId == R.id.categoryJobsFragment
                || destinationId == R.id.profileSettingsFragment
                || destinationId == R.id.notificationsFragment;

        // true => dark icons; false => light icons.
        controller.setAppearanceLightStatusBars(!darkHeaderDestination);
    }

    private void enforceAuthGate() {
        if (navController == null || new SessionManager(this).hasSession()) {
            return;
        }
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, true)
                .build();
        navController.navigate(R.id.loginFragment, null, options);
    }

    private void enforceRoleLandingForActiveSession() {
        if (navController == null) {
            return;
        }
        SessionManager session = new SessionManager(this);
        if (!session.hasSession()) {
            return;
        }
        String role = session.getUserRole();
        if (role == null || !role.toUpperCase(Locale.ROOT).contains("RECRUITER")) {
            return;
        }
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.homeFragment) {
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, true)
                    .setLaunchSingleTop(true)
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build();
            navController.navigate(R.id.recruiterDashboardFragment, null, options);
        }
    }

    private boolean navigateToTopLevel(@IdRes int destinationId) {
        if (navController == null) {
            return false;
        }

        if (currentTopLevelDestination == destinationId && navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == destinationId) {
            return true;
        }

        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
                .build();

        try {
            navController.navigate(destinationId, null, options);
            currentTopLevelDestination = destinationId;
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void applyWindowInsets() {
        ViewGroup rootView = binding.getRoot();
        final int navHostBottomPadding = binding.navHostFragment.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());

            int imePadding = imeVisible ? Math.max(0, ime.bottom - bars.bottom) : 0;
            binding.navHostFragment.setPadding(
                binding.navHostFragment.getPaddingLeft(),
                binding.navHostFragment.getPaddingTop(),
                binding.navHostFragment.getPaddingRight(),
                navHostBottomPadding + imePadding
            );

            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) binding.bottomNavContainer.getLayoutParams();
            params.bottomMargin = bars.bottom;
            binding.bottomNavContainer.setLayoutParams(params);
            return insets;
        });
    }

    private void configureBackgroundNotificationSync() {
        SessionManager session = new SessionManager(this);
        if (!session.hasSession()) {
            NotificationSyncScheduler.cancel(this);
            return;
        }
        String role = session.getUserRole();
        boolean recruiter = role != null && role.toUpperCase(Locale.ROOT).contains("RECRUITER");
        if (recruiter) {
            NotificationSyncScheduler.cancel(this);
            return;
        }
        NotificationSyncScheduler.schedule(this);
    }

    private void maybeRequestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT < 33) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQ_NOTIFICATIONS
        );
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null || navController == null) {
            return;
        }
        if (!intent.getBooleanExtra(NotificationConstants.EXTRA_OPEN_APPLICATION_TIMELINE, false)) {
            return;
        }

        SessionManager session = new SessionManager(this);
        if (!session.hasSession()) {
            return;
        }
        String role = session.getUserRole();
        if (role != null && role.toUpperCase(Locale.ROOT).contains("RECRUITER")) {
            return;
        }

        String applicationId = intent.getStringExtra(NotificationConstants.EXTRA_APPLICATION_ID);
        if (applicationId == null || applicationId.isBlank()) {
            return;
        }

        Bundle args = new Bundle();
        args.putString("applicationId", applicationId);
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build();

        try {
            navController.navigate(R.id.applicationTimelineFragment, args, options);
        } catch (Exception ignored) {
        }

        intent.removeExtra(NotificationConstants.EXTRA_OPEN_APPLICATION_TIMELINE);
        intent.removeExtra(NotificationConstants.EXTRA_APPLICATION_ID);
    }

    public void hideBottomNav() {
        binding.bottomNavContainer.setVisibility(View.GONE);
    }

    public void showBottomNav() {
        binding.bottomNavContainer.setVisibility(View.VISIBLE);
    }
}
