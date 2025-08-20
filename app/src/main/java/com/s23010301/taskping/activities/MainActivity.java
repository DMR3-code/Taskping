package com.s23010301.taskping.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.s23010301.taskping.R;
import com.s23010301.taskping.adapters.DailyTaskAdapter;
import com.s23010301.taskping.adapters.PriorityTaskAdapter;
import com.s23010301.taskping.db.NotificationRepository;
import com.s23010301.taskping.db.TaskRepository;
import com.s23010301.taskping.fragments.NotificationDialogFragment;
import com.s23010301.taskping.helpers.NotificationHelper;
import com.s23010301.taskping.models.DailyTask;
import com.s23010301.taskping.models.Notification;
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
    private NotificationRepository notificationRepository;
    private ImageView btnNotification;
    private TextView notificationBadge;

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
        NotificationHelper.createNotificationChannel(this);

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
        setupNotificationSystem();
    }

    private void setupNotificationSystem() {
        notificationRepository = new NotificationRepository(this);
        btnNotification = findViewById(R.id.btnNotification);
        notificationBadge = findViewById(R.id.notificationBadge);

        // Observe unread notification count
        notificationRepository.getUnreadCount().observe(this, count -> {
            if (count != null && count > 0) {
                notificationBadge.setVisibility(View.VISIBLE);
                notificationBadge.setText(String.valueOf(count));
            } else {
                notificationBadge.setVisibility(View.GONE);
            }
        });

        btnNotification.setOnClickListener(v -> showNotificationsDialog());

        // Check if we should open notifications from intent
        checkNotificationIntent();
    }

    private void checkNotificationIntent() {
        if (getIntent() != null && getIntent().getBooleanExtra("open_notifications", false)) {
            showNotificationsDialog();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkNotificationIntent();
    }


    private void showNotificationsDialog() {
        // Create a simple dialog showing notification count
        notificationRepository.getUnreadCount().observe(this, count -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Notifications")
                    .setMessage("You have " + count + " unread notifications")
                    .setPositiveButton("View All", (dialog, which) -> {
                        showFullNotificationsList();
                    })
                    .setNegativeButton("Mark All Read", (dialog, which) -> {
                        notificationRepository.markAllAsRead();
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        });
    }

    private void showFullNotificationsList() {
        // For now, show a simple list - we'll enhance this later
        notificationRepository.getAllNotifications().observe(this, notifications -> {
            StringBuilder message = new StringBuilder();
            for (Notification notification : notifications) {
                String status = notification.isRead() ? "✓ " : "● ";
                message.append(status).append(notification.getTitle()).append("\n");
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("All Notifications")
                    .setMessage(message.toString())
                    .setPositiveButton("OK", null)
                    .show();
        });
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

