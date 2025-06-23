package com.s23010301.taskping;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddTaskActivity extends AppCompatActivity {
    private EditText inputTitle, inputDescription, inputStartDate, inputEndDate;
    private MaterialButton btnPriority, btnDaily, btnLocation, btnCreate;
    private boolean isPriority = true;
    private Calendar calendarStart = Calendar.getInstance();
    private Calendar calendarEnd = Calendar.getInstance();

    private FirebaseFirestore db;

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

        db = FirebaseFirestore.getInstance();

        inputStartDate.setOnClickListener(v -> showDatePicker(true));
        inputEndDate.setOnClickListener(v -> showDatePicker(false));

        btnPriority.setOnClickListener(v -> toggleTaskType(true));
        btnDaily.setOnClickListener(v -> toggleTaskType(false));

        btnLocation.setOnClickListener(v -> {
            Toast.makeText(this, "Location picker not implemented yet", Toast.LENGTH_SHORT).show();
        });

        btnCreate.setOnClickListener(v -> saveTask());

        updateDateField(inputStartDate, calendarStart);
        updateDateField(inputEndDate, calendarEnd);
    }

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

        Map<String, Object> taskData = new HashMap<>();
        taskData.put("id", taskId);
        taskData.put("title", title);
        taskData.put("description", desc);
        taskData.put("type", type);
        taskData.put("startDate", start);
        taskData.put("endDate", end);
        taskData.put("done", false);
        taskData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("tasks")
                .document(taskId)
                .set(taskData)
                .addOnSuccessListener(unused -> {
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

}