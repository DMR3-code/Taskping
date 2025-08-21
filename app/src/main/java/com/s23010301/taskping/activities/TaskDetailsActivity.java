package com.s23010301.taskping.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
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
    private MaterialButton btnDone;
    private LocalCacheHelper localCache;
    private boolean isTaskDone = false;

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

        // Setup back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Initialize views
        TextView title = findViewById(R.id.detailTitle);
        TextView desc = findViewById(R.id.detailDescription);
        TextView startDate = findViewById(R.id.detailStartDate);
        TextView endDate = findViewById(R.id.detailEndDate);
        TextView remaining = findViewById(R.id.detailRemainingTime);
        btnDone = findViewById(R.id.btnDone);

        Intent intent = getIntent();
        String taskId = intent.getStringExtra("taskId");
        String taskTitle = intent.getStringExtra("title");
        String taskDescription = intent.getStringExtra("description");
        String startDateStr = intent.getStringExtra("startDate");
        String endDateStr = intent.getStringExtra("endDate");
        boolean hasLocation = intent.getBooleanExtra("hasLocation", false);
        String locationStr = intent.getStringExtra("location");
        boolean isDone = intent.getBooleanExtra("isDone", false);

        // Set task details
        title.setText(taskTitle);
        desc.setText(taskDescription != null && !taskDescription.isEmpty() ?
                taskDescription : "No description provided");

        // Format and set dates
        if (startDateStr != null) {
            startDate.setText(formatDate(startDateStr, "MMM dd, yyyy", "MMMM dd, yyyy"));
        } else {
            startDate.setText("Not set");
        }

        if (endDateStr != null) {
            endDate.setText(formatDate(endDateStr, "MMM dd, yyyy", "MMMM dd, yyyy"));
        } else {
            endDate.setText("No due date");
        }

        remaining.setText(getRemainingText(endDateStr));

        // Handle task completion state
        isTaskDone = isDone;
        updateDoneButtonState();

        // Set up click listener for Done button
        btnDone.setOnClickListener(v -> {
            if (isTaskDone) {
                undoTaskCompletion(taskId);
            } else {
                showCompletionConfirmation(taskId);
            }
        });

        // Load full task data from local cache for better handling
        if (taskId != null) {
            loadFullTaskData(taskId);
        }
    }

    private void loadFullTaskData(String taskId) {
        localCache.getTaskById(taskId).observe(this, task -> {
            if (task != null) {
                currentTask = task;
                isTaskDone = task.isDone();
                updateDoneButtonState();
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