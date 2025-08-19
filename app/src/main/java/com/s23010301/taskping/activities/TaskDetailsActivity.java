package com.s23010301.taskping.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

        // Setup back button (matches SignUpActivity style)
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Rest of your existing code remains the same...
        TextView title = findViewById(R.id.detailTitle);
        TextView desc = findViewById(R.id.detailDescription);
        TextView endDate = findViewById(R.id.detailEndDate);
        TextView remaining = findViewById(R.id.detailRemainingTime);
        TextView locationLabel = findViewById(R.id.locationLabel);
        TextView location = findViewById(R.id.detailLocation);
        View btnViewOnMap = findViewById(R.id.btnViewOnMap);

        Intent intent = getIntent();
        String taskTitle = intent.getStringExtra("title");
        String taskDescription = intent.getStringExtra("description");
        String endDateStr = intent.getStringExtra("endDate");
        boolean hasLocation = intent.getBooleanExtra("hasLocation", false);
        String locationStr = intent.getStringExtra("location");

        // Set task details
        title.setText(taskTitle);
        desc.setText(taskDescription != null && !taskDescription.isEmpty() ?
                taskDescription : "No description provided");

        // Format and set end date
        if (endDateStr != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(endDateStr);
                endDate.setText(outputFormat.format(date));
            } catch (ParseException e) {
                endDate.setText(endDateStr);
            }
        } else {
            endDate.setText("No due date");
        }

        remaining.setText(getRemainingText(endDateStr));

        // Handle location data
        if (hasLocation && locationStr != null) {
            locationLabel.setVisibility(View.VISIBLE);
            location.setVisibility(View.VISIBLE);
            btnViewOnMap.setVisibility(View.VISIBLE);

            String[] coords = locationStr.split(",");
            if (coords.length == 2) {
                try {
                    double lat = Double.parseDouble(coords[0].trim());
                    double lng = Double.parseDouble(coords[1].trim());
                    location.setText(String.format(Locale.getDefault(),
                            "Location: %.4f, %.4f", lat, lng));
                } catch (NumberFormatException e) {
                    location.setText("Location: " + locationStr);
                }
            } else {
                location.setText("Location: " + locationStr);
            }

            btnViewOnMap.setOnClickListener(v -> {
                Intent mapIntent = new Intent(this, MapPickerActivity.class);
                mapIntent.putExtra("lat", Double.parseDouble(coords[0].trim()));
                mapIntent.putExtra("lng", Double.parseDouble(coords[1].trim()));
                startActivity(mapIntent);
            });
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