package com.s23010301.taskping.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.s23010301.taskping.R;
import com.s23010301.taskping.adapters.DailyTaskAdapter;
import com.s23010301.taskping.adapters.PriorityTaskAdapter;
import com.s23010301.taskping.db.TaskRepository;
import com.s23010301.taskping.models.DailyTask;
import com.s23010301.taskping.models.PriorityTask;
import com.s23010301.taskping.models.Task;
import com.s23010301.taskping.models.TaskViewModel;
import com.s23010301.taskping.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {
    private PriorityTaskAdapter priorityAdapter;
    private DailyTaskAdapter dailyAdapter;
    private final List<PriorityTask> priorityTasks = new ArrayList<>();
    private final List<DailyTask> dailyTasks = new ArrayList<>();
    private TaskRepository repository;
    private TaskViewModel taskViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_bottom_nav);

        FrameLayout container = findViewById(R.id.content_container);
        ViewGroup mainContent = (ViewGroup) getLayoutInflater()
                .inflate(R.layout.activity_main_content, container);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        TextView todayDate = mainContent.findViewById(R.id.todayDate);
        TextView welcomeText = mainContent.findViewById(R.id.welcomeText);
        RecyclerView priorityRecyclerView = mainContent.findViewById(R.id.priorityRecyclerView);
        RecyclerView dailyRecyclerView = mainContent.findViewById(R.id.dailyRecyclerView);

        todayDate.setText(DateUtils.getCurrentDate("EEEE, MMM dd yyyy"));

        String username = getIntent().getStringExtra("username");
        if (username == null) {
            username = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE)
                    .getString("username", "User");
        }
        welcomeText.setText(String.format("Welcome %s", username));

        priorityRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        priorityAdapter = new PriorityTaskAdapter(this, priorityTasks);
        priorityRecyclerView.setAdapter(priorityAdapter);

        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dailyAdapter = new DailyTaskAdapter(this, dailyTasks);
        dailyRecyclerView.setAdapter(dailyAdapter);

        repository = new TaskRepository(this);

        setupNavigationObserver();
    }

    private void setupNavigationObserver() {
        taskViewModel.navigateToTaskDetails().observe(this, event -> {
            Task task = event.getContentIfNotHandled();
            if (task != null) {
                // This is your navigation logic
                Intent intent = new Intent(this, TaskDetailsActivity.class);
                intent.putExtra("title", task.getTitle());
                intent.putExtra("description", task.getDescription());
                intent.putExtra("endDate", task.getEndDate());
                intent.putExtra("hasLocation", task.hasLocation());
                intent.putExtra("location", task.getLocation());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String today = DateUtils.getCurrentDate("MMM dd, yyyy");

        priorityAdapter.setViewModel(taskViewModel);
        dailyAdapter.setViewModel(taskViewModel);

        repository.getTasksByDate(today).observe(this, tasks -> {
            priorityTasks.clear();
            dailyTasks.clear();

            for (Task task : tasks) {
                if ("priority".equals(task.getType())) {
                    priorityTasks.add(new PriorityTask(task.getId(), task.getTitle(), 0));
                } else if ("daily".equals(task.getType())) {
                    dailyTasks.add(new DailyTask(task.getId(), task.getTitle(), task.isDone(), false));
                }
            }

            priorityAdapter.notifyDataSetChanged();
            dailyAdapter.notifyDataSetChanged();
        });

        // Refresh both types in background
        repository.refreshTasksFromFirestore("priority", today);
        repository.refreshTasksFromFirestore("daily", today);

    }

    @Override
    protected int getCurrentNavItem() {
        return R.id.nav_home;
    }
}

