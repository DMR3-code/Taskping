package com.s23010301.taskping;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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

        RecyclerView priorityRecyclerView = findViewById(R.id.priorityRecyclerView);
        priorityRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

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

        // Adapter and RecyclerView
        RecyclerView dailyRecyclerView = findViewById(R.id.dailyRecyclerView);
        DailyTaskAdapter dailyAdapter = new DailyTaskAdapter(dailyTasks);
        dailyRecyclerView.setAdapter(dailyAdapter);

    }
}
