package com.s23010301.taskping;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationHelper {

    public static void setupBottomNavigation(BottomNavigationView bottomNav, Activity activity) {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                if (!(activity instanceof MainActivity)) {
                    activity.startActivity(new Intent(activity, MainActivity.class));
                    activity.finish();
                }
                return true;

            } else if (itemId == R.id.nav_tasks) {
                if (!(activity instanceof TaskListActivity)) {
                    activity.startActivity(new Intent(activity, TaskListActivity.class));
                    activity.finish();
                }
                return true;

            } else if (itemId == R.id.nav_profile) {
                if (!(activity instanceof ProfileActivity)) {
                    activity.startActivity(new Intent(activity, ProfileActivity.class));
                    activity.finish();
                }
                return true;
            }

            return false;
        });
    }
}
