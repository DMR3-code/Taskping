package com.s23010301.taskping.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.s23010301.taskping.R;

public class ProfileActivity extends BaseActivity {

    private TextView profileName, profileRole, profileLocation, profileTasksCompleted;
    private MaterialCardView statisticsCard, locationCard, settingsCard, logoutCard;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_bottom_nav);

        // Inflate profile content
        FrameLayout container = findViewById(R.id.content_container);
        getLayoutInflater().inflate(R.layout.activity_profile_content, container);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Load user data
        loadUserData();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        profileName = findViewById(R.id.profileName);
        profileRole = findViewById(R.id.profileRole);
        profileLocation = findViewById(R.id.profileLocation);
        profileTasksCompleted = findViewById(R.id.profileTasksCompleted);

        statisticsCard = findViewById(R.id.statisticsCard);
        locationCard = findViewById(R.id.locationCard);
        settingsCard = findViewById(R.id.settingsCard);
        logoutCard = findViewById(R.id.logoutCard);
    }

    private void loadUserData() {
        // Get current user from Firebase Auth
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load user data from Firestore
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        updateUIWithUserData(documentSnapshot);
                    } else {
                        // Use default values if no user data found
                        setDefaultUserData();
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to SharedPreferences if Firestore fails
                    loadFromSharedPreferences();
                });

        // Load completed tasks count
        loadCompletedTasksCount(userId);
    }

    private void updateUIWithUserData(DocumentSnapshot document) {
        String name = document.getString("name");
        String role = document.getString("role");
        String location = document.getString("location");
        Integer tasksCompleted = document.getLong("tasksCompleted") != null ?
                document.getLong("tasksCompleted").intValue() : 0;

        profileName.setText(name != null ? name : "User");
        profileRole.setText(role != null ? role : "Member");
        profileLocation.setText(location != null ? location : "Not set");
        profileTasksCompleted.setText(tasksCompleted + " Tasks Completed");
    }

    private void setDefaultUserData() {
        SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "User");

        profileName.setText(username);
        profileRole.setText("Member");
        profileLocation.setText("Not set");
        profileTasksCompleted.setText("0 Tasks Completed");
    }

    private void loadFromSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "User");
        String location = prefs.getString("location", "Not set");
        int tasksCompleted = prefs.getInt("tasksCompleted", 0);

        profileName.setText(username);
        profileRole.setText("Member");
        profileLocation.setText(location);
        profileTasksCompleted.setText(tasksCompleted + " Tasks Completed");
    }

    private void loadCompletedTasksCount(String userId) {
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("done", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int completedCount = queryDocumentSnapshots.size();
                    profileTasksCompleted.setText(completedCount + " Tasks Completed");

                    // Save to SharedPreferences for offline use
                    SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
                    prefs.edit().putInt("tasksCompleted", completedCount).apply();
                })
                .addOnFailureListener(e -> {
                    // Use cached value if query fails
                    SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
                    int completedCount = prefs.getInt("tasksCompleted", 0);
                    profileTasksCompleted.setText(completedCount + " Tasks Completed");
                });
    }

    private void setupClickListeners() {
        // Statistics Card
        statisticsCard.setOnClickListener(v -> {
            // TODO: Implement statistics screen
            Toast.makeText(this, "Statistics feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // Location Card
        locationCard.setOnClickListener(v -> {
            // TODO: Implement location management
            Toast.makeText(this, "Location settings coming soon", Toast.LENGTH_SHORT).show();
        });

        // Settings Card
        settingsCard.setOnClickListener(v -> {
            // TODO: Implement settings screen
            Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // Logout Card
        logoutCard.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logoutUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        // Clear user-specific preferences
        SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
        prefs.edit()
                .remove("username")
                .remove("location")
                .remove("tasksCompleted")
                .apply();

        // Navigate to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected int getCurrentNavItem() {
        return R.id.nav_profile;
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}