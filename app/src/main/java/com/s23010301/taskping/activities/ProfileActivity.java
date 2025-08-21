package com.s23010301.taskping.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.s23010301.taskping.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;



public class ProfileActivity extends BaseActivity {

    private TextView profileName, profileRole, profileLocation, profileTasksCompleted;
    private MaterialCardView statisticsCard, locationCard, settingsCard, logoutCard;
    private FirebaseFirestore db;
    private String currentUserId;

    // User statistics
    private int totalTasks = 0;
    private int completedTasks = 0;
    private int pendingTasks = 0;
    private String joinDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_bottom_nav);

        // Inflate profile content
        FrameLayout container = findViewById(R.id.content_container);
        getLayoutInflater().inflate(R.layout.activity_profile_content, container);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

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
        // Load user data from Firestore
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        updateUIWithUserData(documentSnapshot);
                    } else {
                        // Create user document if it doesn't exist
                        createUserDocument();
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to SharedPreferences if Firestore fails
                    loadFromSharedPreferences();
                });

        // Load task statistics
        loadTaskStatistics();
    }

    private void createUserDocument() {
        SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "User");

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", username);
        userData.put("role", "Member");
        userData.put("location", "Not set");
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        userData.put("joinDate", currentDate);
        userData.put("tasksCompleted", 0);

        joinDate = currentDate;

        db.collection("users").document(currentUserId)
                .set(userData)
                .addOnSuccessListener(aVoid -> loadUserData())
                .addOnFailureListener(e -> setDefaultUserData());
    }

    private void updateUIWithUserData(DocumentSnapshot document) {
        String name = document.getString("name");
        String role = document.getString("role");
        String location = document.getString("location");
        String documentJoinDate = document.getString("joinDate");

        profileName.setText(name != null ? name : "User");
        profileRole.setText(role != null ? role : "Member");
        profileLocation.setText(location != null ? location : "Not set");

        joinDate = (documentJoinDate != null) ? documentJoinDate : "";
    }

    private void setDefaultUserData() {
        SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "User");

        profileName.setText(username);
        profileRole.setText("Member");
        profileLocation.setText("Not set");
        joinDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
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

    private void loadTaskStatistics() {
        // Load all tasks for the user
        db.collection("tasks")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalTasks = queryDocumentSnapshots.size();
                    completedTasks = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Boolean isDone = doc.getBoolean("done");
                        if (isDone != null && isDone) {
                            completedTasks++;
                        }
                    }

                    pendingTasks = totalTasks - completedTasks;
                    profileTasksCompleted.setText(completedTasks + " Tasks Completed");

                    // Save to SharedPreferences for offline use
                    SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
                    prefs.edit()
                            .putInt("totalTasks", totalTasks)
                            .putInt("completedTasks", completedTasks)
                            .putInt("pendingTasks", pendingTasks)
                            .apply();
                })
                .addOnFailureListener(e -> {
                    // Use cached values if query fails
                    SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
                    completedTasks = prefs.getInt("completedTasks", 0);
                    totalTasks = prefs.getInt("totalTasks", 0);
                    pendingTasks = prefs.getInt("pendingTasks", 0);
                    profileTasksCompleted.setText(completedTasks + " Tasks Completed");
                });
    }

    private void setupClickListeners() {
        // Statistics Card
        statisticsCard.setOnClickListener(v -> showStatisticsDialog());

        // Location Card
        locationCard.setOnClickListener(v -> showLocationDialog());

        // Settings Card
        settingsCard.setOnClickListener(v -> showSettingsDialog());

        // Logout Card
        logoutCard.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void showStatisticsDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_statistics, null);

        TextView totalTasksText = dialogView.findViewById(R.id.totalTasksCount);
        TextView completedTasksText = dialogView.findViewById(R.id.completedTasksCount);
        TextView pendingTasksText = dialogView.findViewById(R.id.pendingTasksCount);
        TextView joinDateText = dialogView.findViewById(R.id.joinDateText);
        TextView completionRateText = dialogView.findViewById(R.id.completionRateText);

        totalTasksText.setText(String.valueOf(totalTasks));
        completedTasksText.setText(String.valueOf(completedTasks));
        pendingTasksText.setText(String.valueOf(pendingTasks));
        joinDateText.setText(joinDate.isEmpty() ? "Not available" : joinDate);

        joinDateText.setText((joinDate == null || joinDate.isEmpty()) ? "Not available" : joinDate);

        int completionRate = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;
        completionRateText.setText(completionRate + "%");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Your Statistics")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLocationDialog() {
        EditText locationInput = new EditText(this);
        locationInput.setHint("Enter your location");
        locationInput.setText(profileLocation.getText().toString().equals("Not set") ?
                "" : profileLocation.getText().toString());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Location")
                .setMessage("Enter your current location:")
                .setView(locationInput)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newLocation = locationInput.getText().toString().trim();
                    if (!newLocation.isEmpty()) {
                        updateLocation(newLocation);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateLocation(String newLocation) {
        // Update in Firestore
        db.collection("users").document(currentUserId)
                .update("location", newLocation)
                .addOnSuccessListener(aVoid -> {
                    profileLocation.setText(newLocation);

                    // Update SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
                    prefs.edit().putString("location", newLocation).apply();

                    showToast("Location updated successfully");
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to update location");
                });
    }

    private void showSettingsDialog() {
        String[] options = {"Edit Profile", "Change Theme", "Notification Settings", "Privacy Policy", "About"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Settings")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditProfileDialog();
                            break;
                        case 1:
                            showThemeDialog();
                            break;
                        case 2:
                            showNotificationSettings();
                            break;
                        case 3:
                            showPrivacyPolicy();
                            break;
                        case 4:
                            showAboutDialog();
                            break;
                    }
                })
                .show();
    }

    private void showEditProfileDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_profile, null);

        // Use EditText instead of TextInputEditText since your XML uses EditText
        EditText nameInput = dialogView.findViewById(R.id.nameInput);
        EditText roleInput = dialogView.findViewById(R.id.roleInput);

        // Set current values
        nameInput.setText(profileName.getText().toString());
        roleInput.setText(profileRole.getText().toString());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    String newRole = roleInput.getText().toString().trim();

                    if (!newName.isEmpty()) {
                        updateProfile(newName, newRole);
                    } else {
                        showToast("Name cannot be empty");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProfile(String name, String role) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("role", role.isEmpty() ? "Member" : role);

        db.collection("users").document(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    profileName.setText(name);
                    profileRole.setText(role.isEmpty() ? "Member" : role);

                    // Update SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
                    prefs.edit().putString("username", name).apply();

                    showToast("Profile updated successfully");
                })
                .addOnFailureListener(e -> showToast("Failed to update profile"));
    }

    private void showThemeDialog() {
        String[] themes = {"Light", "Dark", "System Default"};
        SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
        int currentTheme = prefs.getInt("theme", 0);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
                    prefs.edit().putInt("theme", which).apply();
                    showToast("Theme updated. Restart app to apply changes.");
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNotificationSettings() {
        SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
        boolean[] checkedItems = {
                prefs.getBoolean("notif_task_reminders", true),
                prefs.getBoolean("notif_task_updates", true),
                prefs.getBoolean("notif_achievements", true)
        };

        String[] items = {"Task Reminders", "Task Updates", "Achievements"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Notification Settings")
                .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("notif_task_reminders", checkedItems[0]);
                    editor.putBoolean("notif_task_updates", checkedItems[1]);
                    editor.putBoolean("notif_achievements", checkedItems[2]);
                    editor.apply();
                    showToast("Notification settings saved");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPrivacyPolicy() {
        String privacyText = "Privacy Policy\n\n" +
                "TaskPing is committed to protecting your privacy. This app collects minimal data necessary for functionality:\n\n" +
                "• Account information (name, email)\n" +
                "• Task data you create\n" +
                "• Location (if provided)\n" +
                "• App usage statistics\n\n" +
                "Your data is stored securely and never shared with third parties without your consent.\n\n" +
                "For questions, contact: privacy@taskping.com";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Privacy Policy")
                .setMessage(privacyText)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAboutDialog() {
        String aboutText = "TaskPing v1.0.0\n\n" +
                "A powerful task management app designed to help you stay organized and productive.\n\n" +
                "Features:\n" +
                "• Create and manage tasks\n" +
                "• Track progress\n" +
                "• Set reminders\n" +
                "• View statistics\n\n" +
                "Developed by: Your Name\n" +
                "© 2024 TaskPing. All rights reserved.";

        new MaterialAlertDialogBuilder(this)
                .setTitle("About TaskPing")
                .setMessage(aboutText)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
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
                .remove("totalTasks")
                .remove("pendingTasks")
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