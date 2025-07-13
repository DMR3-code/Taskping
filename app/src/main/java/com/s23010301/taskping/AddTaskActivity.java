package com.s23010301.taskping;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private MaterialButton btnPriority, btnDaily, btnLocation, btnCreate;
    private boolean isPriority = true;
    private Calendar calendarStart = Calendar.getInstance();
    private Calendar calendarEnd = Calendar.getInstance();
    private ActivityResultLauncher<Intent> locationPickerLauncher;

    private FirebaseFirestore db;

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        inputTitle = findViewById(R.id.inputTitle);
        inputDescription = findViewById(R.id.inputDescription);
        inputStartDate = findViewById(R.id.inputStartDate);
        inputEndDate = findViewById(R.id.inputEndDate);
        btnPriority = findViewById(R.id.btnPriority);
        btnDaily = findViewById(R.id.btnDaily);
        btnLocation = findViewById(R.id.btnLocation);
        btnCreate = findViewById(R.id.btnCreate);
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);

        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedLat = result.getData().getDoubleExtra("lat", 0.0);
                        selectedLng = result.getData().getDoubleExtra("lng", 0.0);
                        hasLocation = true;
                        Toast.makeText(this, "Picked: " + selectedLat + ", " + selectedLng, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        db = FirebaseFirestore.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkLocationPermissions();}
        inputStartDate.setOnClickListener(v -> showDatePicker(true));
        inputEndDate.setOnClickListener(v -> showDatePicker(false));
        btnPriority.setOnClickListener(v -> toggleTaskType(true));
        btnDaily.setOnClickListener(v -> toggleTaskType(false));
        btnLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            locationPickerLauncher.launch(intent);});
        btnCreate.setOnClickListener(v -> saveTask());
        updateDateField(inputStartDate, calendarStart);
        updateDateField(inputEndDate, calendarEnd);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            selectedLat = data.getDoubleExtra("lat", 0.0);
            selectedLng = data.getDoubleExtra("lng", 0.0);
            hasLocation = true;
            Toast.makeText(this, "Picked: " + selectedLat + ", " + selectedLng,
                    Toast.LENGTH_SHORT).show();
        }
    }


    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void saveTask() {
        String title = inputTitle.getText().toString().trim();
        String desc = inputDescription.getText().toString().trim();
        String start = inputStartDate.getText().toString();
        String end = inputEndDate.getText().toString();
        String type = isPriority ? "priority" : "daily";

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter task title", Toast.LENGTH_SHORT).show();
            return;
        }

        String taskId = UUID.randomUUID().toString();
        String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendarStart.getTime());


        Map<String, Object> taskData = new HashMap<>();
        taskData.put("id", taskId);
        taskData.put("title", title);
        taskData.put("description", desc);
        taskData.put("type", type);
        taskData.put("startDate", start);
        taskData.put("endDate", end);
        taskData.put("done", false);
        taskData.put("date", date);
        taskData.put("createdAt", FieldValue.serverTimestamp());

        if (hasLocation) {
            taskData.put("location", selectedLat + "," + selectedLng);
            taskData.put("hasLocation", true);
        } else {
            taskData.put("hasLocation", false);
        }


        db.collection("tasks")
                .document(taskId)
                .set(taskData)
                .addOnSuccessListener(unused -> {
                    if (hasLocation) {
                        addGeofence(taskId, selectedLat, selectedLng);
                    }

                    Toast.makeText(this, "Task saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        field.setText(sdf.format(calendar.getTime()));
    }
    private void checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            List<String> permissions = new ArrayList<>();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
            if (!permissions.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 1001);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

    }



    private void addGeofence(String taskId, double lat, double lng) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission denied: cannot add geofence", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Location (GPS) is disabled. Please enable it.", Toast.LENGTH_LONG).show();
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
        ).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Geofence added", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            String errorMessage = geofenceHelper.getErrorString(e);
            Toast.makeText(this, "Geofence failed: " + errorMessage, Toast.LENGTH_SHORT).show();
        });
    }



}