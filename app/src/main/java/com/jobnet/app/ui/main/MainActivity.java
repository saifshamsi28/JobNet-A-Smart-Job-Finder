package com.jobnet.app.ui.main;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.annotation.IdRes;

import com.jobnet.app.R;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.databinding.ActivityMainBinding;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

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
                boolean isTopLevel = destinationId == R.id.homeFragment
                        || destinationId == R.id.searchFragment
                        || destinationId == R.id.savedFragment
                        || destinationId == R.id.profileFragment;
                boolean isAuthScreen = destination.getId() == R.id.loginFragment
                        || destination.getId() == R.id.registerFragment;
                boolean isDetailScreen = destination.getId() == R.id.jobDetailsFragment;
                boolean isRecruiterScreen = destination.getId() == R.id.recruiterDashboardFragment
                    || destination.getId() == R.id.recruiterPostJobFragment;

                if (isTopLevel) {
                    currentTopLevelDestination = destinationId;
                    binding.bottomNavigation.setSelectedItemId(destinationId);
                }

                binding.bottomNavContainer.setVisibility(isAuthScreen ? View.GONE : View.VISIBLE);
                if (isDetailScreen || isRecruiterScreen) {
                    binding.bottomNavContainer.setVisibility(View.GONE);
                }
            });

            enforceAuthGate();
            enforceRoleLandingForActiveSession();
        }
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
            navController.navigate(R.id.recruiterDashboardFragment);
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
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) binding.bottomNavContainer.getLayoutParams();
            params.bottomMargin = bars.bottom;
            binding.bottomNavContainer.setLayoutParams(params);
            return insets;
        });
    }

    public void hideBottomNav() {
        binding.bottomNavContainer.setVisibility(View.GONE);
    }

    public void showBottomNav() {
        binding.bottomNavContainer.setVisibility(View.VISIBLE);
    }
}
