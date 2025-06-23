package com.s23010301.taskping;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private PriorityTaskAdapter priorityAdapter;
    private DailyTaskAdapter dailyAdapter;
    private final List<PriorityTask> priorityTasks = new ArrayList<>();
    private final List<DailyTask> dailyTasks = new ArrayList<>();

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Dynamic date
        TextView todayDate = findViewById(R.id.todayDate);
        String currentDate = new SimpleDateFormat("EEEE, MMM dd yyyy", Locale.getDefault()).format(new Date());
        todayDate.setText(currentDate);

        // Get username
        SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
        String username = getIntent().getStringExtra("username");
        if (username == null) {
            username = prefs.getString("username", "User");
        }

        TextView welcomeText = findViewById(R.id.welcomeText);
        welcomeText.setText("Welcome " + username);

        // Priority Task RecyclerView
        RecyclerView priorityRecyclerView = findViewById(R.id.priorityRecyclerView);
        priorityRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        priorityAdapter = new PriorityTaskAdapter(this,priorityTasks);
        priorityRecyclerView.setAdapter(priorityAdapter);

        // Daily Task RecyclerView
        RecyclerView dailyRecyclerView = findViewById(R.id.dailyRecyclerView);
        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dailyAdapter = new DailyTaskAdapter(this,dailyTasks);
        dailyRecyclerView.setAdapter(dailyAdapter);

        // Load tasks from Firestore
        loadPriorityTasks();
        loadDailyTasks();

        // Bottom nav setup
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        NavigationHelper.setupBottomNavigation(bottomNav, this);
    }

    private void loadPriorityTasks() {
        db.collection("tasks")
                .whereEqualTo("type", "priority")  // lowercase for consistency
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    priorityTasks.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String title = doc.getString("title");
                        int progress = doc.contains("progress") ? doc.getLong("progress").intValue() : 0;
                        int icon = R.drawable.ic_location;
                        priorityTasks.add(new PriorityTask(title, progress, 0, icon));
                    }
                    priorityAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load priority tasks", Toast.LENGTH_SHORT).show());
    }

    private void loadDailyTasks() {
        db.collection("tasks")
                .whereEqualTo("type", "daily")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    dailyTasks.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String title = doc.getString("title");
                        boolean isDone = Boolean.TRUE.equals(doc.getBoolean("done"));
                        boolean hasLocation = doc.contains("location");
                        dailyTasks.add(new DailyTask(title, isDone, hasLocation));
                    }
                    dailyAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load Daily Tasks: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
