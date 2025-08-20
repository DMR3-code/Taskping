package com.s23010301.taskping.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.s23010301.taskping.R;

public abstract class BaseActivity extends AppCompatActivity {
    protected BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Common initialization can be added here if needed
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupBottomNavigation();
    }

    protected void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView == null) return;

        // Clear any existing selection
        bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }

        // Set current item
        bottomNavigationView.post(() -> {
            bottomNavigationView.setSelectedItemId(getCurrentNavItem());
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == getCurrentNavItem()) {
                return true; // Already on this tab
            }

            Class<?> destinationActivity = getDestinationActivity(itemId);
            if (!destinationActivity.equals(this.getClass())) {
                navigateToActivity(destinationActivity);
                return true;
            }
            return false;
        });
    }

    @IdRes
    protected abstract int getCurrentNavItem();

    @NonNull
    protected Class<?> getDestinationActivity(int itemId) {
        if (itemId == R.id.nav_home) {
            return MainActivity.class;
        } else if (itemId == R.id.nav_tasks) {
            return TaskListActivity.class;
        } else if (itemId == R.id.nav_profile) {
            return ProfileActivity.class;
        } else {
            throw new IllegalArgumentException("Unknown navigation item ID");
        }
    }

    protected void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}