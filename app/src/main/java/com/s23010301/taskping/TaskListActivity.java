package com.s23010301.taskping;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class TaskListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private final String[] tabTitles = new String[]{"Priority Task", "Daily Task"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        db = FirebaseFirestore.getInstance();

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TextView monthText = findViewById(R.id.monthText);
        MaterialButton btnAddTask = findViewById(R.id.btnAddTask);

        // Set current month
        String currentMonth = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault()).format(new Date());
        monthText.setText(currentMonth);

        // Setup ViewPager with Adapter
        TaskPagerAdapter  pagerAdapter = new TaskPagerAdapter (this);
        viewPager.setAdapter(pagerAdapter);

        // Attach TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        // Handle Add Task button
        btnAddTask.setOnClickListener(view -> {
            startActivity(new Intent(TaskListActivity.this, AddTaskActivity.class));
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        NavigationHelper.setupBottomNavigation(bottomNav, this);
    }
    private void fetchTasksFromFirestore() {
        db.collection("tasks")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DailyTask> dailyList = new ArrayList<>();
                    List<PriorityTask> priorityList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        String type = doc.getString("type");
                        boolean done = Boolean.TRUE.equals(doc.getBoolean("done"));

                        if ("daily".equals(type)) {
                            boolean hasLocation = doc.contains("location"); // Optional
                            dailyList.add(new DailyTask(title, done, hasLocation));
                        } else {
                            priorityList.add(new PriorityTask(title, 0, 0, R.drawable.ic_location));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load tasks", Toast.LENGTH_SHORT).show();
                });
    }
}
