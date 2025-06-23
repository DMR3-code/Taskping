package com.s23010301.taskping;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private PriorityTaskAdapter priorityAdapter;
    private DailyTaskAdapter dailyAdapter;
    private List<PriorityTask> priorityTasks = new ArrayList<>();
    private List<DailyTask> dailyTasks = new ArrayList<>();

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

        // Set dynamic date
        TextView todayDate = findViewById(R.id.todayDate);
        String currentDate = new SimpleDateFormat("EEEE, MMM dd yyyy", Locale.getDefault()).format(new Date());
        todayDate.setText(currentDate);

        // Get username from intent or SharedPreferences
        SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
        String username = getIntent().getStringExtra("username");
        if (username == null) {
            username = prefs.getString("username", "User");
        }

        // Set welcome message
        TextView welcomeText = findViewById(R.id.welcomeText);
        welcomeText.setText("Welcome " + username);

        // Priority RecyclerView setup
        RecyclerView priorityRecyclerView = findViewById(R.id.priorityRecyclerView);
        priorityRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        priorityAdapter = new PriorityTaskAdapter(priorityTasks);
        priorityRecyclerView.setAdapter(priorityAdapter);

        List<PriorityTask> tasks = new ArrayList<>();
        tasks.add(new PriorityTask("Shopping List", 10, 0, R.drawable.ic_location));
        tasks.add(new PriorityTask("Mobile Project", 20, 25, R.drawable.ic_location));
        tasks.add(new PriorityTask("Laravel Task", 30, 15, R.drawable.ic_location));

        PriorityTaskAdapter adapter = new PriorityTaskAdapter(tasks);
        priorityRecyclerView.setAdapter(adapter);

        // Sample tasks
        List<DailyTask> dailyTasks = new ArrayList<>();
        dailyTasks.add(new DailyTask("Submit assignment", false,true));
        dailyTasks.add(new DailyTask("Team meeting", false,false));
        dailyTasks.add(new DailyTask("Grocery shopping", true,true));

        // Daily RecyclerView setup
        RecyclerView dailyRecyclerView = findViewById(R.id.dailyRecyclerView);
        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dailyAdapter = new DailyTaskAdapter(dailyTasks);
        dailyRecyclerView.setAdapter(dailyAdapter);

        loadPriorityTasks();
        loadDailyTasks();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        NavigationHelper.setupBottomNavigation(bottomNav, this);

    }
    private void loadPriorityTasks() {
        db.collection("tasks")
                .whereEqualTo("type", "Priority")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    priorityTasks.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        int progress = doc.getLong("progress").intValue();
                        int icon = R.drawable.ic_location; // Replace if dynamic
                        priorityTasks.add(new PriorityTask(title, progress, 0, icon));
                    }
                    priorityAdapter.notifyDataSetChanged();
                });
    }
    private void loadDailyTasks() {
        db.collection("tasks")
                .whereEqualTo("type", "Daily")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    dailyTasks.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        boolean isDone = Boolean.TRUE.equals(doc.getBoolean("done"));
                        boolean hasLocation = Boolean.TRUE.equals(doc.getBoolean("hasLocation"));
                        dailyTasks.add(new DailyTask(title, isDone, hasLocation));
                    }
                    dailyAdapter.notifyDataSetChanged();
                });
    }
}
