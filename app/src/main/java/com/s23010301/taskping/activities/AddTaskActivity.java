package com.s23010301.taskping.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.s23010301.taskping.R;
import com.s23010301.taskping.helpers.GeofenceHelper;
import com.s23010301.taskping.helpers.LocalCacheHelper;
import com.s23010301.taskping.models.Task;
import com.s23010301.taskping.models.TaskReminderReceiver;
import com.s23010301.taskping.utils.DateUtils;
import com.s23010301.taskping.helpers.FirestoreHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddTaskActivity extends AppCompatActivity {
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private double selectedLat = 0.0;
    private double selectedLng = 0.0;
    private boolean hasLocation = false;
    private EditText inputTitle, inputDescription, inputStartDate, inputEndDate;
    private MaterialButton btnPriority, btnDaily;
    private boolean isPriority = true;
    private final Calendar calendarStart = Calendar.getInstance();
    private final Calendar calendarEnd = Calendar.getInstance();
    private ActivityResultLauncher<Intent> locationPickerLauncher;

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        initializeViews();
        setupLocationPicker();
        setupButtonListeners();
        updateDateFields();

        // Check all required permissions
        checkAllPermissions();
    }

    private void initializeViews() {
        inputTitle = findViewById(R.id.inputTitle);
        inputDescription = findViewById(R.id.inputDescription);
        inputStartDate = findViewById(R.id.inputStartDate);
        inputEndDate = findViewById(R.id.inputEndDate);
        btnPriority = findViewById(R.id.btnPriority);
        btnDaily = findViewById(R.id.btnDaily);
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);
    }

    private void setupLocationPicker() {
        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleLocationResult(result.getData());
                    }
                }
        );
    }

    private void handleLocationResult(Intent data) {
        selectedLat = data.getDoubleExtra("lat", 0.0);
        selectedLng = data.getDoubleExtra("lng", 0.0);
        hasLocation = true;
        Toast.makeText(this, "Picked: " + selectedLat + ", " + selectedLng, Toast.LENGTH_SHORT).show();
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void setupButtonListeners() {
        findViewById(R.id.btnLocation).setOnClickListener(v -> launchMapPicker());
        findViewById(R.id.btnCreate).setOnClickListener(v -> saveTask());
        btnPriority.setOnClickListener(v -> toggleTaskType(true));
        btnDaily.setOnClickListener(v -> toggleTaskType(false));
        inputStartDate.setOnClickListener(v -> showDatePicker(true));
        inputEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void launchMapPicker() {
        Intent intent = new Intent(this, MapPickerActivity.class);
        locationPickerLauncher.launch(intent);
    }

    private void updateDateFields() {
        updateDateField(inputStartDate, calendarStart);
        updateDateField(inputEndDate, calendarEnd);
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void saveTask() {
        String title = inputTitle.getText().toString().trim();
        if (title.isEmpty()) {
            showToast("Please enter task title");
            return;
        }

        showLoading(true);

        Map<String, Object> taskData = createTaskData(title);

        FirestoreHelper.saveTask(taskData,
                unused -> {
                    // Insert into local Room DB immediately
                    LocalCacheHelper cache = LocalCacheHelper.getInstance(AddTaskActivity.this);

                    Task newTask = new Task(
                            (String) taskData.get("id"),
                            (String) taskData.get("title"),
                            (String) taskData.get("type"),
                            (String) taskData.get("date"),
                            false,
                            (String) taskData.get("description"),
                            (String) taskData.get("endDate"),
                            (boolean) taskData.get("hasLocation"),
                            (String) taskData.get("location")
                    );

                    cache.insertOrUpdateTasks(Collections.singletonList(newTask));

                    scheduleTaskReminder(taskData);

                    // Continue with geofencing if needed
                    if (hasLocation) {
                        addGeofenceWithCallback(
                                (String) taskData.get("id"),
                                selectedLat,
                                selectedLng,
                                this::handleSaveSuccess,
                                this::handleGeofenceError
                        );
                    } else {
                        handleSaveSuccess();
                    }
                },
                e -> {
                    showLoading(false);
                    handleSaveError(e);
                }
        );
    }

    private void scheduleTaskReminder(Map<String, Object> taskData) {
        try {
            String taskId = (String) taskData.get("id");
            String title = (String) taskData.get("title");
            String description = (String) taskData.get("description");
            String dateStr = (String) taskData.get("date");

            if (taskId == null || title == null || dateStr == null) {
                Log.e("AddTaskActivity", "Missing required task data for reminder");
                return;
            }

            Calendar reminderTime = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            reminderTime.setTime(sdf.parse(dateStr));

            // Set reminder for 9 AM on task date
            reminderTime.set(Calendar.HOUR_OF_DAY, 9);
            reminderTime.set(Calendar.MINUTE, 0);
            reminderTime.set(Calendar.SECOND, 0);

            TaskReminderReceiver.scheduleReminder(
                    this,
                    taskId,
                    title,
                    description,
                    reminderTime
            );
        } catch (ParseException e) {
            Log.e("AddTaskActivity", "Error parsing date for reminder", e);
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void addGeofenceWithCallback(String taskId, double lat, double lng,
                                         Runnable onSuccess, OnFailureListener onFailure) {
        if (!checkGeofencePrerequisites()) return;

        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            String error = GoogleApiAvailability.getInstance().getErrorString(status);
            Toast.makeText(this, "Play Services not available: " + error, Toast.LENGTH_LONG).show();
            Log.e("GeofenceSetup", "Google Play Services error: " + error);
            return;
        }

        Geofence geofence = new Geofence.Builder()
                .setRequestId(taskId)
                .setCircularRegion(lat, lng, 100)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();

        geofencingClient.addGeofences(
                        geofenceHelper.getGeofencingRequest(geofence),
                        geofenceHelper.getPendingIntent()
                ).addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> {
                    if (e instanceof ApiException) {
                        int code = ((ApiException) e).getStatusCode();
                        if (code == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
                            Log.e("Geofence", "GEOFENCE_NOT_AVAILABLE - will retry later");
                        }
                    }
                    onFailure.onFailure(e);
                });
    }

    private boolean checkGeofencePrerequisites() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            showToast("Location permission denied");
            return false;
        }
        if (!isLocationEnabled()) {
            showToast("Please enable GPS");
            return false;
        }

        return true;
    }

    private void handleSaveSuccess() {
        showLoading(false);
        showToast("Task saved successfully");
        setResult(RESULT_OK);
        finish();
    }

    private void handleSaveError(Exception e) {
        String errorMsg = "Failed to save task: " +
                (e != null ? e.getMessage() : "Unknown error");
        showToast(errorMsg);
        Log.e("AddTaskActivity", errorMsg, e);

        if (e instanceof FirebaseFirestoreException) {
            handleFirestoreError((FirebaseFirestoreException) e);
        }
    }

    private void handleGeofenceError(Exception e) {
        String errorMsg = "Task saved but geofence failed: " +
                (e instanceof ApiException
                        ? GeofenceStatusCodes.getStatusCodeString(((ApiException) e).getStatusCode())
                        : (e != null ? e.getMessage() : "Unknown error"));
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        Log.e("AddTaskActivity", errorMsg, e);
    }

    private void handleFirestoreError(FirebaseFirestoreException e) {
        switch (e.getCode()) {
            case PERMISSION_DENIED:
                showToast("You don't have permission to save tasks");
                break;
            case UNAVAILABLE:
                showToast("Network unavailable - try again later");
                break;
            default:
                showToast("Something went wrong. Try again later.");
        }
    }

    private void showLoading(boolean isLoading) {
        findViewById(R.id.btnCreate).setEnabled(!isLoading);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private Map<String, Object> createTaskData(String title) {
        Map<String, Object> taskData = new HashMap<>();
        String taskId = UUID.randomUUID().toString();

        String formattedDate = DateUtils.formatDate(calendarStart, "MMM dd, yyyy");

        // Get current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        taskData.put("id", taskId);
        taskData.put("userId", userId); // Add user ID for Firestore queries
        taskData.put("title", title);
        taskData.put("description", inputDescription.getText().toString().trim());
        taskData.put("type", isPriority ? "priority" : "daily");
        taskData.put("startDate", inputStartDate.getText().toString());
        taskData.put("endDate", inputEndDate.getText().toString());
        taskData.put("done", false);
        taskData.put("date", formattedDate);
        taskData.put("createdAt", FieldValue.serverTimestamp());
        taskData.put("hasLocation", hasLocation);

        if (hasLocation) {
            taskData.put("location", selectedLat + "," + selectedLng);
        }

        return taskData;
    }

    private void toggleTaskType(boolean prioritySelected) {
        isPriority = prioritySelected;

        btnPriority.setBackgroundTintList(getColorStateList(prioritySelected ? R.color.blue : R.color.gray_300));
        btnDaily.setBackgroundTintList(getColorStateList(prioritySelected ? R.color.gray_300 : R.color.blue));
        btnPriority.setTextColor(getColor(prioritySelected ? android.R.color.white : android.R.color.black));
        btnDaily.setTextColor(getColor(prioritySelected ? android.R.color.black : android.R.color.white));
    }

    private void showDatePicker(boolean isStart) {
        final Calendar calendar = isStart ? calendarStart : calendarEnd;

        new DatePickerDialog(this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateField(isStart ? inputStartDate : inputEndDate, calendar);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateDateField(EditText field, Calendar calendar) {
        field.setText(DateUtils.formatDate(calendar, "MMM dd, yyyy"));
    }

    private void checkAllPermissions() {
        List<String> permissions = new ArrayList<>();

        // Location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        // Alarm permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            boolean allPermissionsGranted = true;

            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    String permissionName = permissions[i];
                    Log.w("AddTaskActivity", "Permission denied: " + permissionName);

                    if (permissionName.equals(Manifest.permission.SCHEDULE_EXACT_ALARM)) {
                        showToast("Reminder notifications may not work without alarm permission");
                    } else if (permissionName.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showToast("Location features will be disabled");
                    }
                }
            }

            if (allPermissionsGranted) {
                showToast("All permissions granted");
            }
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}