package com.s23010301.taskping.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.s23010301.taskping.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TaskDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        // Setup back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Initialize views
        TextView title = findViewById(R.id.detailTitle);
        TextView desc = findViewById(R.id.detailDescription);
        TextView startDate = findViewById(R.id.detailStartDate);
        TextView endDate = findViewById(R.id.detailEndDate);
        TextView remaining = findViewById(R.id.detailRemainingTime);
        LinearLayout locationSection = findViewById(R.id.locationSection);
        TextView location = findViewById(R.id.detailLocation);
        MaterialButton btnViewOnMap = findViewById(R.id.btnViewOnMap);

        Intent intent = getIntent();
        String taskTitle = intent.getStringExtra("title");
        String taskDescription = intent.getStringExtra("description");
        String startDateStr = intent.getStringExtra("startDate");
        String endDateStr = intent.getStringExtra("endDate");
        boolean hasLocation = intent.getBooleanExtra("hasLocation", false);
        String locationStr = intent.getStringExtra("location");

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

        // Handle location data
        if (hasLocation && locationStr != null) {
            locationSection.setVisibility(View.VISIBLE);

            String[] coords = locationStr.split(",");
            if (coords.length == 2) {
                try {
                    double lat = Double.parseDouble(coords[0].trim());
                    double lng = Double.parseDouble(coords[1].trim());
                    location.setText(String.format(Locale.getDefault(), "%.4f, %.4f", lat, lng));
                } catch (NumberFormatException e) {
                    location.setText(locationStr);
                }
            } else {
                location.setText(locationStr);
            }

            btnViewOnMap.setOnClickListener(v -> {
                Intent mapIntent = new Intent(this, MapPickerActivity.class);
                mapIntent.putExtra("lat", Double.parseDouble(coords[0].trim()));
                mapIntent.putExtra("lng", Double.parseDouble(coords[1].trim()));
                startActivity(mapIntent);
            });
        } else {
            locationSection.setVisibility(View.GONE);
        }
    }

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
            if (daysLeft == 0) {
                return "Due today!";
            } else if (daysLeft == 1) {
                return "Due tomorrow";
            } else {
                return String.format(Locale.getDefault(),
                        "%d days remaining", daysLeft);
            }
        } catch (ParseException e) {
            return "Unknown deadline";
        }
    }
}