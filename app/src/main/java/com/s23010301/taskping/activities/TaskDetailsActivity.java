package com.s23010301.taskping.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.FieldValue;
import com.s23010301.taskping.R;
import com.s23010301.taskping.helpers.FirestoreHelper;
import com.s23010301.taskping.helpers.LocalCacheHelper;
import com.s23010301.taskping.models.Task;
import com.s23010301.taskping.receivers.TaskReminderReceiver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TaskDetailsActivity extends AppCompatActivity {

    private Task currentTask;
    private ExtendedFloatingActionButton btnDone;
    private LocalCacheHelper localCache;
    private boolean isTaskDone = false;
    private LinearProgressIndicator timeProgress;
    private TextView progressPercentage;
    private Chip chipStatus, chipPriority;
    private MaterialCardView locationCard;

    // Constants for broadcast actions
    private static final String ACTION_TASK_COMPLETED = "com.s23010301.taskping.TASK_COMPLETED";
    private static final String ACTION_TASK_UNCOMPLETED = "com.s23010301.taskping.TASK_UNCOMPLETED";
    private static final String EXTRA_TASK_ID = "task_id";
    private static final String EXTRA_TASK_TYPE = "task_type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        localCache = LocalCacheHelper.getInstance(this);

        // Setup toolbar and collapsing toolbar
        setupToolbar();

        // Initialize all UI elements
        initializeViews();

        // Setup click listeners
        setupClickListeners();

        // Get task data from intent and populate UI
        loadTaskDataFromIntent();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Handle back navigation
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initializeViews() {
        // Status and priority chips
        chipStatus = findViewById(R.id.chipStatus);
        chipPriority = findViewById(R.id.chipPriority);

        // Progress indicators
        timeProgress = findViewById(R.id.timeProgress);
        progressPercentage = findViewById(R.id.progressPercentage);

        // Location card
        locationCard = findViewById(R.id.locationCard);

        // Main action button
        btnDone = findViewById(R.id.btnDone);
    }

    private void setupClickListeners() {
        // FAB buttons in the app bar
        FloatingActionButton fabEdit = findViewById(R.id.fabEdit);
        FloatingActionButton fabShare = findViewById(R.id.fabShare);

        fabEdit.setOnClickListener(v -> editTask());
        fabShare.setOnClickListener(v -> shareTask());

        // Quick action buttons
        LinearLayout actionSetReminder = findViewById(R.id.actionSetReminder);
        LinearLayout actionAddNote = findViewById(R.id.actionAddNote);
        LinearLayout actionDuplicate = findViewById(R.id.actionDuplicate);

        actionSetReminder.setOnClickListener(v -> setReminder());
        actionAddNote.setOnClickListener(v -> addNote());
        actionDuplicate.setOnClickListener(v -> duplicateTask());

        // View on map button
        MaterialButton btnViewOnMap = findViewById(R.id.btnViewOnMap);
        btnViewOnMap.setOnClickListener(v -> viewOnMap());

        // Main done/undone button
        btnDone.setOnClickListener(v -> {
            if (isTaskDone) {
                undoTaskCompletion(currentTask.getId());
            } else {
                showCompletionConfirmation(currentTask.getId());
            }
        });
    }

    private void loadTaskDataFromIntent() {
        Intent intent = getIntent();
        String taskId = intent.getStringExtra("taskId");
        String taskTitle = intent.getStringExtra("title");
        String taskDescription = intent.getStringExtra("description");
        String startDateStr = intent.getStringExtra("startDate");
        String endDateStr = intent.getStringExtra("endDate");
        boolean hasLocation = intent.getBooleanExtra("hasLocation", false);
        String locationStr = intent.getStringExtra("location");
        boolean isDone = intent.getBooleanExtra("isDone", false);
        String priority = intent.getStringExtra("priority");
        String status = intent.getStringExtra("status");

        // Set collapsing toolbar title
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsingToolbar);
        collapsingToolbar.setTitle(taskTitle != null ? taskTitle : "Task Details");

        // Set task details in TextViews
        TextView detailDescription = findViewById(R.id.detailDescription);
        TextView detailStartDate = findViewById(R.id.detailStartDate);
        TextView detailEndDate = findViewById(R.id.detailEndDate);
        TextView detailStartTime = findViewById(R.id.detailStartTime);
        TextView detailEndTime = findViewById(R.id.detailEndTime);
        TextView detailRemainingTime = findViewById(R.id.detailRemainingTime);
        TextView detailLocation = findViewById(R.id.detailLocation);

        // Set description
        detailDescription.setText(taskDescription != null && !taskDescription.isEmpty() ?
                taskDescription : "No description provided");

        // Set dates and times
        if (startDateStr != null) {
            String[] dateTime = parseDateTime(startDateStr);
            detailStartDate.setText(dateTime[0]);
            detailStartTime.setText(dateTime[1]);
        } else {
            detailStartDate.setText("Not set");
            detailStartTime.setText("--");
        }

        if (endDateStr != null) {
            String[] dateTime = parseDateTime(endDateStr);
            detailEndDate.setText(dateTime[0]);
            detailEndTime.setText(dateTime[1]);
        } else {
            detailEndDate.setText("No due date");
            detailEndTime.setText("--");
        }

        // Set remaining time and progress
        setRemainingTimeAndProgress(startDateStr, endDateStr);

        // Set status and priority chips
        updateStatusChip(status, isDone);
        updatePriorityChip(priority);

        // Handle location
        if (hasLocation && locationStr != null) {
            locationCard.setVisibility(View.VISIBLE);
            detailLocation.setText(locationStr);
        } else {
            locationCard.setVisibility(View.GONE);
        }

        // Set completion state
        isTaskDone = isDone;
        updateDoneButtonState();

        // Load full task data from local cache for better handling
        if (taskId != null) {
            loadFullTaskData(taskId);
        }
    }

    private String[] parseDateTime(String dateTimeStr) {
        // Assuming the format is "MMM dd, yyyy HH:mm" or similar
        // Adjust this based on your actual date format
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateTimeStr);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            return new String[]{dateFormat.format(date), timeFormat.format(date)};
        } catch (ParseException e) {
            return new String[]{dateTimeStr, "09:00 AM"};
        }
    }

    private void setRemainingTimeAndProgress(String startDateStr, String endDateStr) {
        TextView detailRemainingTime = findViewById(R.id.detailRemainingTime);

        if (endDateStr == null) {
            detailRemainingTime.setText("No deadline set");
            timeProgress.setProgress(0);
            progressPercentage.setText("0%");
            return;
        }

        String remainingText = getRemainingText(endDateStr);
        detailRemainingTime.setText(remainingText);

        // Calculate progress based on start and end dates
        int progress = calculateProgress(startDateStr, endDateStr);
        timeProgress.setProgress(progress);
        progressPercentage.setText(progress + "%");
    }

    private int calculateProgress(String startDateStr, String endDateStr) {
        if (startDateStr == null || endDateStr == null) return 0;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);
            Date currentDate = new Date();

            if (startDate == null || endDate == null) return 0;

            long totalDuration = endDate.getTime() - startDate.getTime();
            long elapsedTime = currentDate.getTime() - startDate.getTime();

            if (totalDuration <= 0) return 100;
            if (elapsedTime <= 0) return 0;
            if (elapsedTime >= totalDuration) return 100;

            return (int) ((elapsedTime * 100) / totalDuration);
        } catch (ParseException e) {
            return 0;
        }
    }

    private void updateStatusChip(String status, boolean isDone) {
        if (isDone) {
            chipStatus.setText("Completed");
            chipStatus.setChipBackgroundColorResource(R.color.green);
            chipStatus.setChipIconResource(R.drawable.ic_check);
        } else if (status != null) {
            chipStatus.setText(status);
            // Set appropriate color based on status
            switch (status.toLowerCase()) {
                case "in progress":
                    chipStatus.setChipBackgroundColorResource(R.color.orange);
                    chipStatus.setChipIconResource(R.drawable.ic_clock);
                    break;
                case "pending":
                    chipStatus.setChipBackgroundColorResource(R.color.red);
                    chipStatus.setChipIconResource(R.drawable.ic_priority);
                    break;
                case "completed":
                    chipStatus.setChipBackgroundColorResource(R.color.green);
                    chipStatus.setChipIconResource(R.drawable.ic_check);
                    break;
                default:
                    chipStatus.setChipBackgroundColorResource(R.color.blue);
                    chipStatus.setChipIconResource(R.drawable.ic_clock);
                    break;
            }
        } else {
            chipStatus.setText("In Progress");
            chipStatus.setChipBackgroundColorResource(R.color.orange);
            chipStatus.setChipIconResource(R.drawable.ic_clock);
        }
    }

    private void updatePriorityChip(String priority) {
        if (priority != null) {
            chipPriority.setText(priority + " Priority");
            switch (priority.toLowerCase()) {
                case "high":
                    chipPriority.setChipBackgroundColorResource(R.color.red);
                    break;
                case "medium":
                    chipPriority.setChipBackgroundColorResource(R.color.orange);
                    break;
                case "low":
                    chipPriority.setChipBackgroundColorResource(R.color.green);
                    break;
                default:
                    chipPriority.setChipBackgroundColorResource(R.color.blue);
                    break;
            }
        } else {
            chipPriority.setText("High Priority");
            chipPriority.setChipBackgroundColorResource(R.color.red);
        }
    }

    // Quick action methods
    private void editTask() {
        // Navigate to edit task activity using AddTaskActivity
        Intent editIntent = new Intent(this, AddTaskActivity.class);
        editIntent.putExtra("mode", "edit");
        if (currentTask != null) {
            editIntent.putExtra("taskId", currentTask.getId());
            editIntent.putExtra("title", currentTask.getTitle());
            editIntent.putExtra("description", currentTask.getDescription());
            editIntent.putExtra("startDate", currentTask.getDate()); // Use getDate() instead of getStartDate()
            editIntent.putExtra("endDate", currentTask.getEndDate());
            editIntent.putExtra("type", currentTask.getType());
            editIntent.putExtra("hasLocation", currentTask.hasLocation());
            editIntent.putExtra("location", currentTask.getLocation());
        }
        startActivity(editIntent);
    }

    private void shareTask() {
        if (currentTask != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Task: " + currentTask.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, createShareText());
            startActivity(Intent.createChooser(shareIntent, "Share Task"));
        }
    }

    private String createShareText() {
        StringBuilder shareText = new StringBuilder();
        shareText.append("📋 Task: ").append(currentTask.getTitle()).append("\n");
        if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
            shareText.append("📝 Description: ").append(currentTask.getDescription()).append("\n");
        }
        shareText.append("📅 Due: ").append(currentTask.getEndDate()).append("\n");
        shareText.append("Status: ").append(isTaskDone ? "✅ Completed" : "⏳ Pending");
        return shareText.toString();
    }

    private void setReminder() {
        // Implement reminder functionality
        Toast.makeText(this, "Set reminder functionality", Toast.LENGTH_SHORT).show();
    }

    private void addNote() {
        // Implement add note functionality
        Toast.makeText(this, "Add note functionality", Toast.LENGTH_SHORT).show();
    }

    private void duplicateTask() {
        // Navigate to create task with current task data using AddTaskActivity
        Intent duplicateIntent = new Intent(this, AddTaskActivity.class);
        duplicateIntent.putExtra("mode", "duplicate");
        if (currentTask != null) {
            duplicateIntent.putExtra("title", currentTask.getTitle() + " (Copy)");
            duplicateIntent.putExtra("description", currentTask.getDescription());
            duplicateIntent.putExtra("type", currentTask.getType());
            duplicateIntent.putExtra("hasLocation", currentTask.hasLocation());
            duplicateIntent.putExtra("location", currentTask.getLocation());
        }
        startActivity(duplicateIntent);
    }

    private void viewOnMap() {
        // Implement map viewing functionality
        if (currentTask != null && currentTask.hasLocation()) {
            // Open maps app or show map fragment
            Toast.makeText(this, "Opening map...", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFullTaskData(String taskId) {
        localCache.getTaskById(taskId).observe(this, task -> {
            if (task != null) {
                currentTask = task;
                isTaskDone = task.isDone();
                updateDoneButtonState();
                // Use a default status since Task model doesn't have getStatus()
                updateStatusChip(isTaskDone ? "completed" : "in progress", isTaskDone);
            }
        });
    }

    private void updateDoneButtonState() {
        if (isTaskDone) {
            btnDone.setText("Mark as Undone");
            btnDone.setIconResource(R.drawable.ic_undo);
            btnDone.setBackgroundTintList(getColorStateList(R.color.green));
        } else {
            btnDone.setText("Mark as Done");
            btnDone.setIconResource(R.drawable.ic_check);
            btnDone.setBackgroundTintList(getColorStateList(R.color.blue));
        }
    }

    private void showCompletionConfirmation(String taskId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder
                .setTitle("Mark as Done?")
                .setMessage("Are you sure you want to mark this task as completed?")
                .setPositiveButton("Yes", (dialogInterface, which) -> markTaskAsDone(taskId))
                .setNegativeButton("Cancel", null)
                .create();

        // Fix button visibility issues
        dialog.setOnShowListener(dialogInterface -> {
            try {
                if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFF2196F3);
                }
                if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF757575);
                }
            } catch (Exception e) {
                // Use default styling
            }
        });

        dialog.show();
    }

    private void markTaskAsDone(String taskId) {
        // Show loading state
        btnDone.setEnabled(false);
        btnDone.setText("Updating...");

        // Update local cache first for instant feedback
        if (currentTask != null) {
            currentTask.setDone(true);
            localCache.insertOrUpdateTasks(Collections.singletonList(currentTask));
        }

        // Update Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("done", true);
        updates.put("completedAt", FieldValue.serverTimestamp());
        updates.put("lastUpdated", FieldValue.serverTimestamp());

        FirestoreHelper.updateTask(taskId, updates,
                unused -> {
                    // Success
                    isTaskDone = true;
                    updateDoneButtonState();
                    updateStatusChip("completed", true);
                    btnDone.setEnabled(true);

                    // Cancel any reminders for this task
                    cancelTaskReminders(taskId);

                    // Broadcast task completion to update MainActivity stats
                    broadcastTaskCompletion(taskId, currentTask != null ? currentTask.getType() : "daily");

                    // Show success message with celebration
                    showCompletionSuccess();
                },
                e -> {
                    // Error - revert local changes
                    if (currentTask != null) {
                        currentTask.setDone(false);
                        localCache.insertOrUpdateTasks(Collections.singletonList(currentTask));
                    }
                    isTaskDone = false;
                    updateDoneButtonState();
                    btnDone.setEnabled(true);

                    Toast.makeText(this, "Failed to update task. Please try again.", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void undoTaskCompletion(String taskId) {
        // Show loading state
        btnDone.setEnabled(false);
        btnDone.setText("Updating...");

        // Update local cache first
        if (currentTask != null) {
            currentTask.setDone(false);
            localCache.insertOrUpdateTasks(Collections.singletonList(currentTask));
        }

        // Update Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("done", false);
        updates.put("completedAt", null);
        updates.put("lastUpdated", FieldValue.serverTimestamp());

        FirestoreHelper.updateTask(taskId, updates,
                unused -> {
                    // Success
                    isTaskDone = false;
                    updateDoneButtonState();
                    updateStatusChip("in progress", false);
                    btnDone.setEnabled(true);

                    // Re-schedule reminders if needed
                    rescheduleTaskReminders(taskId);

                    // Broadcast task uncompletion to update MainActivity stats
                    broadcastTaskUncompletion(taskId, currentTask != null ? currentTask.getType() : "daily");

                    Toast.makeText(this, "Task marked as undone!", Toast.LENGTH_SHORT).show();
                },
                e -> {
                    // Error - revert local changes
                    if (currentTask != null) {
                        currentTask.setDone(true);
                        localCache.insertOrUpdateTasks(Collections.singletonList(currentTask));
                    }
                    isTaskDone = true;
                    updateDoneButtonState();
                    btnDone.setEnabled(true);

                    Toast.makeText(this, "Failed to update task. Please try again.", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void broadcastTaskCompletion(String taskId, String taskType) {
        Intent intent = new Intent(ACTION_TASK_COMPLETED);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putExtra(EXTRA_TASK_TYPE, taskType);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastTaskUncompletion(String taskId, String taskType) {
        Intent intent = new Intent(ACTION_TASK_UNCOMPLETED);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putExtra(EXTRA_TASK_TYPE, taskType);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void showCompletionSuccess() {
        // Show a celebration toast with emoji
        Toast.makeText(this, "🎉 Task completed! Great job!", Toast.LENGTH_LONG).show();

        // Optional: You could add animation here
        btnDone.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction(() -> {
                    btnDone.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void cancelTaskReminders(String taskId) {
        // Cancel any alarms/reminders for this task
        TaskReminderReceiver.cancelReminder(this, taskId);

        // Remove geofence if exists
        if (currentTask != null && currentTask.hasLocation()) {
            GeofencingClient geofencingClient = LocationServices.getGeofencingClient(this);
            geofencingClient.removeGeofences(Collections.singletonList(taskId));
        }
    }

    private void rescheduleTaskReminders(String taskId) {
        // Re-schedule reminders if task has future date
        if (currentTask != null && !isTaskOverdue(currentTask.getEndDate())) {
            // You'll need to implement this based on your reminder logic
            // This would re-create alarms/geofences
        }
    }

    private boolean isTaskOverdue(String endDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date end = sdf.parse(endDate);
            return end != null && end.before(new Date());
        } catch (ParseException e) {
            return false;
        }
    }

    // Helper methods for date formatting and remaining time calculation
    private String formatDate(String dateStr, String inputPattern, String outputPattern) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(inputPattern, Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat(outputPattern, Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private String getRemainingText(String endDateStr) {
        if (endDateStr == null) return "No deadline set";

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        try {
            Date end = sdf.parse(endDateStr);
            long diff = end.getTime() - System.currentTimeMillis();

            if (diff <= 0) {
                return "Due date passed";
            }

            long daysLeft = TimeUnit.MILLISECONDS.toDays(diff);
            long hoursLeft = TimeUnit.MILLISECONDS.toHours(diff) % 24;

            if (daysLeft == 0 && hoursLeft == 0) {
                return "Due today!";
            } else if (daysLeft == 0) {
                return String.format(Locale.getDefault(), "%d hours remaining", hoursLeft);
            } else if (daysLeft == 1) {
                return String.format(Locale.getDefault(), "1 day %d hours remaining", hoursLeft);
            } else {
                return String.format(Locale.getDefault(), "%d days %d hours remaining", daysLeft, hoursLeft);
            }
        } catch (ParseException e) {
            return "Unknown deadline";
        }
    }

    @Override
    public void finish() {
        super.finish();
        // Add exit animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}