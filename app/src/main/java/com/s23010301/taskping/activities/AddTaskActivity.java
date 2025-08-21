package com.s23010301.taskping.activities;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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
import com.s23010301.taskping.receivers.TaskReminderReceiver;
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

    // UI Components
    private MaterialToolbar toolbar;
    private TextInputEditText inputTitle, inputDescription, inputStartDate, inputEndDate;
    private TextInputLayout titleInputLayout;
    private MaterialButton btnPriority, btnDaily, btnLocation;
    private ExtendedFloatingActionButton btnCreate;

    // Geofencing
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private double selectedLat = 0.0;
    private double selectedLng = 0.0;
    private boolean hasLocation = false;
    private static final int MAX_GEOFENCE_RETRIES = 3;
    private int geofenceRetryCount = 0;

    // Task Configuration
    private boolean isPriority = true;
    private final Calendar calendarStart = Calendar.getInstance();
    private final Calendar calendarEnd = Calendar.getInstance();

    // Activity Management
    private ActivityResultLauncher<Intent> locationPickerLauncher;

    // Edit mode variables
    private String editMode = "create"; // "create", "edit", or "duplicate"
    private String editTaskId = null;
    private Task currentTask = null;

    // Animation and UI State
    private boolean isFormValid = false;

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        initializeViews();
        setupToolbar();
        setupLocationPicker();
        setupButtonListeners();
        setupFormValidation();

        // Handle different modes (create, edit, duplicate)
        handleIntentExtras();

        // Initialize dates
        initializeDates();
        updateDateFields();

        // Check Google Play Services first
        if (!checkGooglePlayServices()) {
            // If Play Services not available, disable location features
            btnLocation.setEnabled(false);
            showLocationServiceWarning();
        }

        // Check all required permissions
        checkAllPermissions();

        // Setup initial animations
        setupInitialAnimations();
    }

    private void initializeViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);

        // Input fields
        inputTitle = findViewById(R.id.inputTitle);
        inputDescription = findViewById(R.id.inputDescription);
        inputStartDate = findViewById(R.id.inputStartDate);
        inputEndDate = findViewById(R.id.inputEndDate);

        // Input layouts
        titleInputLayout = findViewById(R.id.titleInputLayout);

        // Buttons
        btnPriority = findViewById(R.id.btnPriority);
        btnDaily = findViewById(R.id.btnDaily);
        btnLocation = findViewById(R.id.btnLocation);
        btnCreate = findViewById(R.id.btnCreate);

        // Geofencing
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            // Add animation before finishing
            animateExit();
        });
    }

    private void setupFormValidation() {
        inputTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validateForm() {
        String title = inputTitle.getText().toString().trim();
        boolean wasValid = isFormValid;
        isFormValid = !title.isEmpty();

        // Update title input layout state
        if (title.isEmpty() && inputTitle.hasFocus()) {
            titleInputLayout.setError("Task title is required");
        } else {
            titleInputLayout.setError(null);
        }

        // Animate FAB if validation state changed
        if (wasValid != isFormValid) {
            animateFab();
        }

        // Update FAB state
        btnCreate.setEnabled(isFormValid);
        btnCreate.setAlpha(isFormValid ? 1.0f : 0.6f);
    }

    private void handleIntentExtras() {
        Intent intent = getIntent();
        editMode = intent.getStringExtra("mode");
        if (editMode == null) editMode = "create";

        switch (editMode) {
            case "edit":
                editTaskId = intent.getStringExtra("taskId");
                loadTaskForEditing(intent);
                updateUIForEditMode();
                break;
            case "duplicate":
                loadTaskForDuplicating(intent);
                updateUIForDuplicateMode();
                break;
            default:
                updateUIForCreateMode();
                break;
        }
    }

    private void loadTaskForEditing(Intent intent) {
        editTaskId = intent.getStringExtra("taskId");
        inputTitle.setText(intent.getStringExtra("title"));
        inputDescription.setText(intent.getStringExtra("description"));

        String startDate = intent.getStringExtra("startDate");
        String endDate = intent.getStringExtra("endDate");
        String type = intent.getStringExtra("type");
        boolean hasLocationData = intent.getBooleanExtra("hasLocation", false);
        String location = intent.getStringExtra("location");

        if (startDate != null) inputStartDate.setText(startDate);
        if (endDate != null) inputEndDate.setText(endDate);

        // Set task type with animation
        if ("priority".equals(type)) {
            toggleTaskType(true, true);
        } else {
            toggleTaskType(false, true);
        }

        // Handle location data
        if (hasLocationData && location != null) {
            handleLocationData(location);
        }

        // Parse dates for calendar objects
        parseDatesFromStrings(startDate, endDate);
    }

    private void loadTaskForDuplicating(Intent intent) {
        // Similar to edit but don't set the task ID
        inputTitle.setText(intent.getStringExtra("title"));
        inputDescription.setText(intent.getStringExtra("description"));

        String type = intent.getStringExtra("type");
        boolean hasLocationData = intent.getBooleanExtra("hasLocation", false);
        String location = intent.getStringExtra("location");

        // Set task type with animation
        if ("priority".equals(type)) {
            toggleTaskType(true, true);
        } else {
            toggleTaskType(false, true);
        }

        // Handle location data
        if (hasLocationData && location != null) {
            handleLocationData(location);
        }

        // Set today's date as start date and tomorrow as end date for duplicated task
        calendarStart.setTimeInMillis(System.currentTimeMillis());
        calendarEnd.setTimeInMillis(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
    }

    private void handleLocationData(String location) {
        String[] coords = location.split(",");
        if (coords.length == 2) {
            try {
                selectedLat = Double.parseDouble(coords[0]);
                selectedLng = Double.parseDouble(coords[1]);
                hasLocation = true;
                updateLocationButton();
            } catch (NumberFormatException e) {
                Log.e("AddTaskActivity", "Error parsing location coordinates", e);
            }
        }
    }

    private void updateLocationButton() {
        if (hasLocation) {
            btnLocation.setText("Location Added ✓");
            btnLocation.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_location_on_24));
            btnLocation.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue_light));
            btnLocation.setTextColor(ContextCompat.getColor(this, R.color.blue));
            btnLocation.setStrokeColor(ContextCompat.getColorStateList(this, R.color.blue));

            // Add subtle animation
            animateLocationButton();
        } else {
            btnLocation.setText("Add Location");
            btnLocation.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_add_location_24));
            btnLocation.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.location_button_background));
            btnLocation.setTextColor(ContextCompat.getColor(this, R.color.blue));
            btnLocation.setStrokeColor(ContextCompat.getColorStateList(this, R.color.blue));
        }
    }

    private void parseDatesFromStrings(String startDate, String endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        if (startDate != null) {
            try {
                calendarStart.setTime(sdf.parse(startDate));
            } catch (ParseException e) {
                Log.e("AddTaskActivity", "Error parsing start date", e);
            }
        }

        if (endDate != null) {
            try {
                calendarEnd.setTime(sdf.parse(endDate));
            } catch (ParseException e) {
                Log.e("AddTaskActivity", "Error parsing end date", e);
            }
        }
    }

    private void updateUIForCreateMode() {
        toolbar.setTitle("Create New Task");
        btnCreate.setText("Create Task");
        btnCreate.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_check_24));
    }

    private void updateUIForEditMode() {
        toolbar.setTitle("Edit Task");
        btnCreate.setText("Update Task");
        btnCreate.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_check_24));
    }

    private void updateUIForDuplicateMode() {
        toolbar.setTitle("Duplicate Task");
        btnCreate.setText("Create Copy");
        btnCreate.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_check_24));
    }

    private void initializeDates() {
        // Set default dates if in create mode
        if ("create".equals(editMode)) {
            calendarStart.setTimeInMillis(System.currentTimeMillis());
            calendarEnd.setTimeInMillis(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        }
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
        updateLocationButton();
        showLocationSuccessMessage();
    }

    private void showLocationSuccessMessage() {
        Toast toast = Toast.makeText(this,
                "Location added successfully! You'll get reminders when you arrive.",
                Toast.LENGTH_LONG);
        toast.show();
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void setupButtonListeners() {
        btnLocation.setOnClickListener(v -> {
            if (hasLocation) {
                // Show options to edit or remove location
                showLocationOptions();
            } else {
                launchMapPicker();
            }
        });

        btnCreate.setOnClickListener(v -> {
            if (isFormValid) {
                animateButtonPress();
                saveTask();
            } else {
                showValidationError();
            }
        });

        btnPriority.setOnClickListener(v -> toggleTaskType(true, true));
        btnDaily.setOnClickListener(v -> toggleTaskType(false, true));

        inputStartDate.setOnClickListener(v -> showDatePicker(true));
        inputEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showLocationOptions() {
        // Create options dialog for editing or removing location
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Location Options")
                .setMessage("Current location has been set. What would you like to do?")
                .setPositiveButton("Edit Location", (dialog, which) -> launchMapPicker())
                .setNegativeButton("Remove Location", (dialog, which) -> removeLocation())
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void removeLocation() {
        hasLocation = false;
        selectedLat = 0.0;
        selectedLng = 0.0;
        updateLocationButton();
        Toast.makeText(this, "Location removed", Toast.LENGTH_SHORT).show();
    }

    private void showValidationError() {
        if (inputTitle.getText().toString().trim().isEmpty()) {
            inputTitle.requestFocus();
            titleInputLayout.setError("Please enter a task title");

            // Shake animation for the input field
            animateShake(titleInputLayout);
        }
    }

    private void launchMapPicker() {
        Intent intent = new Intent(this, MapPickerActivity.class);
        if (hasLocation) {
            intent.putExtra("currentLat", selectedLat);
            intent.putExtra("currentLng", selectedLng);
        }
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
            showValidationError();
            return;
        }

        showSavingState(true);

        if ("edit".equals(editMode) && editTaskId != null) {
            updateExistingTask();
        } else {
            createNewTask();
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void createNewTask() {
        Map<String, Object> taskData = createTaskData(inputTitle.getText().toString().trim());

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

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void updateExistingTask() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", inputTitle.getText().toString().trim());
        updates.put("description", inputDescription.getText().toString().trim());
        updates.put("type", isPriority ? "priority" : "daily");
        updates.put("startDate", inputStartDate.getText().toString());
        updates.put("endDate", inputEndDate.getText().toString());
        updates.put("hasLocation", hasLocation);
        updates.put("lastUpdated", FieldValue.serverTimestamp());

        if (hasLocation) {
            updates.put("location", selectedLat + "," + selectedLng);
        } else {
            updates.put("location", null);
        }

        FirestoreHelper.updateTask(editTaskId, updates,
                unused -> {
                    // Update local cache
                    LocalCacheHelper cache = LocalCacheHelper.getInstance(AddTaskActivity.this);
                    // You'll need to implement updateTask method in LocalCacheHelper
                    // or retrieve and update the existing task

                    // Handle geofencing updates
                    if (hasLocation) {
                        addGeofenceWithRetry(
                                editTaskId,
                                selectedLat,
                                selectedLng,
                                this::handleUpdateSuccess,
                                this::handleGeofenceError
                        );
                    } else {
                        // Remove existing geofence if location was removed
                        removeGeofenceForTask(editTaskId);
                        handleUpdateSuccess();
                    }
                },
                e -> {
                    showLoading(false);
                    handleSaveError(e);
                }
        );
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void removeGeofenceForTask(String taskId) {
        if (geofenceHelper != null) {
            geofencingClient.removeGeofences(Collections.singletonList(taskId))
                    .addOnSuccessListener(unused -> Log.d("AddTaskActivity", "Geofence removed for task: " + taskId))
                    .addOnFailureListener(e -> Log.e("AddTaskActivity", "Failed to remove geofence", e));
        }
    }

    private void handleUpdateSuccess() {
        showLoading(false);
        showToast("Task updated successfully");
        setResult(RESULT_OK);
        finish();
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

            // Set reminder for 7 AM on task date
            reminderTime.set(Calendar.HOUR_OF_DAY, 7);
            reminderTime.set(Calendar.MINUTE, 0);
            reminderTime.set(Calendar.SECOND, 0);

            TaskReminderReceiver.scheduleReminder(
                    this,
                    taskId, // Pass task ID
                    title,
                    description,
                    reminderTime
            );

            Log.d("AddTaskActivity", "Reminder scheduled for task: " + title);

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
        showSavingState(false);
        String message = "edit".equals(editMode) ? "Task updated successfully" : "Task saved successfully";

        // Show success animation
        btnCreate.setText("Success!");
        btnCreate.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_check_24));

        new Handler().postDelayed(() -> {
            setResult(RESULT_OK);
            animateExit();
        }, 1000);

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        btnCreate.setEnabled(!isLoading);
        if (isLoading) {
            btnCreate.setText("Saving...");
        } else {
            switch (editMode) {
                case "edit":
                    btnCreate.setText("Update Task");
                    break;
                case "duplicate":
                    btnCreate.setText("Create Copy");
                    break;
                default:
                    btnCreate.setText("Create Task");
                    break;
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private Map<String, Object> createTaskData(String title) {
        Map<String, Object> taskData = new HashMap<>();
        String taskId = editMode.equals("edit") && editTaskId != null ? editTaskId : UUID.randomUUID().toString();

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

        if (editMode.equals("edit")) {
            taskData.put("lastUpdated", FieldValue.serverTimestamp());
        } else {
            taskData.put("createdAt", FieldValue.serverTimestamp());
        }

        taskData.put("hasLocation", hasLocation);

        if (hasLocation) {
            taskData.put("location", selectedLat + "," + selectedLng);
        } else {
            taskData.put("location", null);
        }

        return taskData;
    }

    private void toggleTaskType(boolean prioritySelected, boolean animate) {
        isPriority = prioritySelected;

        if (animate) {
            animateTaskTypeToggle(prioritySelected);
        } else {
            updateTaskTypeButtons(prioritySelected);
        }
    }

    private void updateTaskTypeButtons(boolean prioritySelected) {
        // Update Priority button
        btnPriority.setBackgroundTintList(ContextCompat.getColorStateList(this,
                prioritySelected ? R.color.priority_color : R.color.priority_color_inactive));
        btnPriority.setTextColor(ContextCompat.getColor(this,
                prioritySelected ? android.R.color.white : R.color.text_secondary));

        // Update Daily button
        btnDaily.setBackgroundTintList(ContextCompat.getColorStateList(this,
                prioritySelected ? R.color.daily_color_inactive : R.color.daily_color));
        btnDaily.setTextColor(ContextCompat.getColor(this,
                prioritySelected ? R.color.text_secondary : android.R.color.white));
    }

    // Animation Methods
    private void setupInitialAnimations() {
        // Animate cards sliding in from bottom
        View[] cards = {
                findViewById(R.id.titleCard),
                findViewById(R.id.dateCard),
                findViewById(R.id.categoryCard),
                findViewById(R.id.locationCard),
                findViewById(R.id.descriptionCard)
        };

        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];
            card.setTranslationY(100f);
            card.setAlpha(0f);

            card.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(i * 50)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        // Animate FAB
        btnCreate.setTranslationY(200f);
        btnCreate.setAlpha(0f);
        btnCreate.animate()
                .translationY(0f)
                .alpha(isFormValid ? 1.0f : 0.6f)
                .setDuration(400)
                .setStartDelay(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void animateFab() {
        float targetAlpha = isFormValid ? 1.0f : 0.6f;
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(btnCreate, "alpha", targetAlpha);
        alphaAnimator.setDuration(200);
        alphaAnimator.start();

        if (isFormValid) {
            // Subtle scale animation when becoming valid
            btnCreate.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        btnCreate.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start();
                    })
                    .start();
        }
    }

    private void animateTaskTypeToggle(boolean prioritySelected) {
        // Scale animation for selected button
        MaterialButton selectedButton = prioritySelected ? btnPriority : btnDaily;
        MaterialButton unselectedButton = prioritySelected ? btnDaily : btnPriority;

        selectedButton.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(100)
                .withEndAction(() -> {
                    updateTaskTypeButtons(prioritySelected);
                    selectedButton.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void animateLocationButton() {
        btnLocation.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    btnLocation.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void animateButtonPress() {
        btnCreate.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    btnCreate.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void animateShake(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", 0f, -10f, 10f, -5f, 5f, 0f);
        animator.setDuration(400);
        animator.start();
    }

    private void animateExit() {
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void showSavingState(boolean saving) {
        btnCreate.setEnabled(!saving);
        if (saving) {
            btnCreate.setText("Saving...");
            btnCreate.setIcon(null);
        } else {
            switch (editMode) {
                case "edit":
                    btnCreate.setText("Update Task");
                    break;
                case "duplicate":
                    btnCreate.setText("Create Copy");
                    break;
                default:
                    btnCreate.setText("Create Task");
                    break;
            }
            btnCreate.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_check_24));
        }
    }

    private void showLocationServiceWarning() {
        Toast.makeText(this, "Location features disabled - Google Play Services required",
                Toast.LENGTH_LONG).show();
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

    @Override
    public void finish() {
        super.finish();
        // Add exit animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}