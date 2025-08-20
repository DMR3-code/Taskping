package com.s23010301.taskping.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

    // Add retry mechanism for geofencing
    private static final int MAX_GEOFENCE_RETRIES = 3;
    private int geofenceRetryCount = 0;

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        initializeViews();
        setupLocationPicker();
        setupButtonListeners();
        updateDateFields();

        // Check Google Play Services first
        if (!checkGooglePlayServices()) {
            // If Play Services not available, disable location features
            findViewById(R.id.btnLocation).setEnabled(false);
            Toast.makeText(this, "Location features disabled - Google Play Services required", Toast.LENGTH_LONG).show();
        }

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
                        addGeofenceWithRetry(
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
    private void addGeofenceWithRetry(String taskId, double lat, double lng,
                                      Runnable onSuccess, OnFailureListener onFailure) {
        geofenceRetryCount = 0;
        attemptGeofenceCreation(taskId, lat, lng, onSuccess, onFailure);
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void attemptGeofenceCreation(String taskId, double lat, double lng,
                                         Runnable onSuccess, OnFailureListener onFailure) {
        if (!checkGeofencePrerequisites()) {
            onFailure.onFailure(new Exception("Geofence prerequisites not met"));
            return;
        }
        // Enhanced Google Play Services check
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            String error = GoogleApiAvailability.getInstance().getErrorString(status);
            Log.e("GeofenceSetup", "Google Play Services error: " + error);
            if (GoogleApiAvailability.getInstance().isUserResolvableError(status)) {
                GoogleApiAvailability.getInstance().getErrorDialog(this, status, 9000).show();
            }
            onFailure.onFailure(new Exception("Google Play Services not available: " + error));
            return;
        }
        // Validate coordinates
        if (Math.abs(lat) < 0.001 && Math.abs(lng) < 0.001) {
            Log.e("GeofenceSetup", "Invalid coordinates (near 0,0): " + lat + ", " + lng);
            onFailure.onFailure(new Exception("Invalid location coordinates - please pick a valid location"));
            return;
        }
        // Create geofence with enhanced parameters
        Geofence geofence = new Geofence.Builder()
                .setRequestId(taskId)
                .setCircularRegion(lat, lng, 200) // Increased radius to 200 meters for better reliability
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(30000) // 30 seconds loitering delay
                .build();
        Log.d("GeofenceSetup", "Attempting to add geofence for task: " + taskId + " at " + lat + ", " + lng);
        // Use individual pending intent for each geofence
        geofencingClient.addGeofences(
                        geofenceHelper.getGeofencingRequest(geofence),
                        geofenceHelper.getPendingIntentForGeofence(taskId) // Use individual intent
                ).addOnSuccessListener(unused -> {
                    Log.d("GeofenceSetup", "Geofence added successfully for task: " + taskId);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("GeofenceSetup", "Geofence failed for task: " + taskId, e);
                    handleGeofenceFailure(taskId, lat, lng, onSuccess, onFailure, e);
                });
    }


    private boolean checkGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 9000).show();
            } else {
                Log.e("AddTaskActivity", "This device does not support Google Play Services");
                Toast.makeText(this, "Google Play Services not available on this device", Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void handleGeofenceFailure(String taskId, double lat, double lng,
                                       Runnable onSuccess, OnFailureListener onFailure, Exception e) {
        if (e instanceof ApiException) {
            int statusCode = ((ApiException) e).getStatusCode();
            String statusMessage = GeofenceStatusCodes.getStatusCodeString(statusCode);

            Log.e("GeofenceSetup", "Geofence API error: " + statusMessage + " (code: " + statusCode + ")");

            switch (statusCode) {
                case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                    // Retry with delay if we haven't exceeded max retries
                    if (geofenceRetryCount < MAX_GEOFENCE_RETRIES) {
                        geofenceRetryCount++;
                        Log.d("GeofenceSetup", "Retrying geofence creation, attempt: " + geofenceRetryCount);

                        // Retry after 2 seconds
                        new Handler().postDelayed(() -> {
                            attemptGeofenceCreation(taskId, lat, lng, onSuccess, onFailure);
                        }, 2000);
                        return;
                    }
                    break;

                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                    // Remove old geofences and retry
                    removeAllGeofencesAndRetry(taskId, lat, lng, onSuccess, onFailure);
                    return;

                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                    Log.e("GeofenceSetup", "Too many pending intents - this is a code issue");
                    break;
            }
        }

        // If we reach here, either we've exceeded retries or it's a non-retryable error
        onFailure.onFailure(e);
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void removeAllGeofencesAndRetry(String taskId, double lat, double lng,
                                            Runnable onSuccess, OnFailureListener onFailure) {
        Log.d("GeofenceSetup", "Removing all geofences due to limit exceeded");

        geofencingClient.removeGeofences(geofenceHelper.getPendingIntent())
                .addOnSuccessListener(unused -> {
                    Log.d("GeofenceSetup", "Old geofences removed, retrying new geofence");
                    // Wait a moment then retry
                    new android.os.Handler().postDelayed(() -> {
                        attemptGeofenceCreation(taskId, lat, lng, onSuccess, onFailure);
                    }, 1000);
                })
                .addOnFailureListener(removeError -> {
                    Log.e("GeofenceSetup", "Failed to remove old geofences", removeError);
                    onFailure.onFailure(removeError);
                });
    }

    private boolean checkGeofencePrerequisites() {
        // Check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("GeofenceSetup", "Fine location permission not granted");
            showToast("Location permission required for location-based reminders");
            return false;
        }

        // Check background location for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w("GeofenceSetup", "Background location permission not granted - geofences may not work reliably");
                // Don't block geofence creation, but warn user
                showToast("Background location permission recommended for reliable location reminders");
            }
        }

        // Check if location services are enabled
        if (!isLocationEnabled()) {
            Log.e("GeofenceSetup", "Location services disabled");
            showToast("Please enable location services in device settings");
            return false;
        }

        // Check if high accuracy location is enabled
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!gpsEnabled && !networkEnabled) {
                Log.e("GeofenceSetup", "No location providers enabled");
                showToast("Please enable high accuracy location in device settings");
                return false;
            }
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
        String errorMsg;
        String userMsg;

        if (e instanceof ApiException) {
            int statusCode = ((ApiException) e).getStatusCode();
            errorMsg = "Geofence failed: " + GeofenceStatusCodes.getStatusCodeString(statusCode);

            switch (statusCode) {
                case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                    userMsg = "Task saved! Location reminders may not work - try enabling high accuracy location.";
                    break;
                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                    userMsg = "Task saved! Too many location reminders active - some older ones were removed.";
                    break;
                default:
                    userMsg = "Task saved! Location reminders may not work on this device.";
            }
        } else {
            errorMsg = "Geofence failed: " + (e != null ? e.getMessage() : "Unknown error");
            userMsg = "Task saved! Location reminders may not work properly.";
        }

        Toast.makeText(this, userMsg, Toast.LENGTH_LONG).show();
        Log.e("AddTaskActivity", errorMsg, e);

        // Still consider this a success since the task was saved
        setResult(RESULT_OK);
        showLoading(false);
        finish();
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

        // Background location for Android 10+ (important for geofencing)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
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
                    } else if (permissionName.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        showToast("Background location recommended for reliable location reminders");
                    }
                }
            }

            if (allPermissionsGranted) {
                showToast("All permissions granted");
            } else {
                // Provide guidance for enabling permissions
                showToast("Some permissions denied. Check Settings > Apps > [Your App] > Permissions for full functionality");
            }
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }
}