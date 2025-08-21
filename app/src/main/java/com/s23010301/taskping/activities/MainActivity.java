package com.s23010301.taskping.activities;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.s23010301.taskping.R;
import com.s23010301.taskping.adapters.DailyTaskAdapter;
import com.s23010301.taskping.adapters.PriorityTaskAdapter;
import com.s23010301.taskping.db.NotificationRepository;
import com.s23010301.taskping.db.TaskRepository;
import com.s23010301.taskping.helpers.NotificationHelper;
import com.s23010301.taskping.models.DailyTask;
import com.s23010301.taskping.models.Notification;
import com.s23010301.taskping.models.PriorityTask;
import com.s23010301.taskping.models.Task;
import com.s23010301.taskping.models.TaskViewModel;
import com.s23010301.taskping.utils.DateUtils;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    // Adapters and Data
    private PriorityTaskAdapter priorityAdapter;
    private DailyTaskAdapter dailyAdapter;
    private final List<PriorityTask> priorityTasks = new ArrayList<>();
    private final List<DailyTask> dailyTasks = new ArrayList<>();

    // Repositories and ViewModels
    private TaskRepository repository;
    private TaskViewModel taskViewModel;
    private NotificationRepository notificationRepository;

    // UI Elements - Header
    private TextView todayDate;
    private TextView welcomeText;
    private ImageView btnNotification;
    private TextView notificationBadge;

    // UI Elements - Stats
    private TextView completedTasksCount;
    private TextView pendingTasksCount;
    private TextView priorityTasksCount;

    // UI Elements - Content
    private RecyclerView priorityRecyclerView;
    private RecyclerView dailyRecyclerView;
    private LinearLayout emptyStateLayout;
    private ImageView btnAddPriorityTask;
    private ImageView btnAddDailyTask;
    private MaterialButton btnAddFirstTask;

    // Data tracking
    private int totalCompletedTasks = 0;
    private int totalPendingTasks = 0;
    private int totalPriorityTasks = 0;

    private BroadcastReceiver taskCompletionReceiver;
    private static final String ACTION_TASK_COMPLETED = "com.s23010301.taskping.TASK_COMPLETED";
    private static final String ACTION_TASK_UNCOMPLETED = "com.s23010301.taskping.TASK_UNCOMPLETED";
    private static final String EXTRA_TASK_ID = "task_id";
    private static final String EXTRA_TASK_TYPE = "task_type";

    // Animation handler
    private Handler animationHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_bottom_nav);

        // Initialize core components
        initializeLayout();
        initializeViews();
        setupViewModels();
        setupRepositories();
        setupRecyclerViews();
        setupClickListeners();
        setupObservers();

        // Load initial data
        loadUserData();
        setupNotificationSystem();
        setupTaskCompletionReceiver();


        // Create notification channel
        NotificationHelper.createNotificationChannel(this);
    }

    private void initializeLayout() {
        FrameLayout container = findViewById(R.id.content_container);
        if (container != null) {
            ViewGroup mainContent = (ViewGroup) getLayoutInflater()
                    .inflate(R.layout.activity_main_content, container, false);
            container.addView(mainContent);
        }
    }

    private void initializeViews() {
        try {
            // Header elements
            todayDate = findViewById(R.id.todayDate);
            welcomeText = findViewById(R.id.welcomeText);
            btnNotification = findViewById(R.id.btnNotification);
            notificationBadge = findViewById(R.id.notificationBadge);

            // Stats elements
            completedTasksCount = findViewById(R.id.completedTasksCount);
            pendingTasksCount = findViewById(R.id.pendingTasksCount);
            priorityTasksCount = findViewById(R.id.priorityTasksCount);

            // RecyclerViews
            priorityRecyclerView = findViewById(R.id.priorityRecyclerView);
            dailyRecyclerView = findViewById(R.id.dailyRecyclerView);

            // Action buttons
            btnAddPriorityTask = findViewById(R.id.btnAddPriorityTask);
            btnAddDailyTask = findViewById(R.id.btnAddDailyTask);
            btnAddFirstTask = findViewById(R.id.btnAddFirstTask);

            // Empty state
            emptyStateLayout = findViewById(R.id.emptyStateLayout);

            // Set initial values
            setInitialValues();

        } catch (Exception e) {
            showErrorToast("Error initializing views: " + e.getMessage());
        }
    }

    private void setInitialValues() {
        if (todayDate != null) {
            todayDate.setText(DateUtils.getCurrentDate("EEEE, MMM dd yyyy"));
        }

        if (completedTasksCount != null) completedTasksCount.setText("0");
        if (pendingTasksCount != null) pendingTasksCount.setText("0");
        if (priorityTasksCount != null) priorityTasksCount.setText("0");

        if (notificationBadge != null) {
            notificationBadge.setVisibility(View.GONE);
        }

        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setupViewModels() {
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
    }

    private void setupRepositories() {
        repository = new TaskRepository(this);
        notificationRepository = new NotificationRepository(this);
    }

    private void setupRecyclerViews() {
        if (priorityRecyclerView != null) {
            LinearLayoutManager priorityLayoutManager = new LinearLayoutManager(
                    this, LinearLayoutManager.HORIZONTAL, false);
            priorityRecyclerView.setLayoutManager(priorityLayoutManager);
            priorityAdapter = new PriorityTaskAdapter(this, priorityTasks);
            priorityRecyclerView.setAdapter(priorityAdapter);
        }

        if (dailyRecyclerView != null) {
            LinearLayoutManager dailyLayoutManager = new LinearLayoutManager(this);
            dailyRecyclerView.setLayoutManager(dailyLayoutManager);
            dailyAdapter = new DailyTaskAdapter(this, dailyTasks);
            dailyRecyclerView.setAdapter(dailyAdapter);
        }
    }

    private void setupClickListeners() {
        // Priority task add button
        if (btnAddPriorityTask != null) {
            btnAddPriorityTask.setOnClickListener(v -> {
                animateButtonPress(btnAddPriorityTask);
                navigateToAddTask("priority");
            });
        }

        // Daily task add button
        if (btnAddDailyTask != null) {
            btnAddDailyTask.setOnClickListener(v -> {
                animateButtonPress(btnAddDailyTask);
                navigateToAddTask("daily");
            });
        }

        // First task button (empty state)
        if (btnAddFirstTask != null) {
            btnAddFirstTask.setOnClickListener(v -> {
                animateButtonPress(btnAddFirstTask);
                navigateToAddTask("");
            });
        }

        // Notification button
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                animateButtonPress(btnNotification);
                showNotificationsDialog();
            });
        }
    }

    private void setupObservers() {
        setupNavigationObserver();
        setupTaskDataObserver();
    }

    private void setupNavigationObserver() {
        if (taskViewModel != null) {
            taskViewModel.navigateToTaskDetails().observe(this, event -> {
                if (event != null) {
                    Task task = event.getContentIfNotHandled();
                    if (task != null) {
                        navigateToTaskDetails(task);
                    }
                }
            });
        }
    }

    private void setupTaskDataObserver() {
        String today = DateUtils.getCurrentDate("MMM dd, yyyy");

        if (repository != null) {
            repository.getTasksByDate(today).observe(this, tasks -> {
                if (tasks != null) {
                    updateTaskData(tasks);
                }
            });
        }
    }

    private void loadUserData() {
        try {
            // Load username
            String username = getIntent().getStringExtra("username");
            if (username == null) {
                SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
                username = prefs.getString("username", "User");
            }

            if (welcomeText != null) {
                welcomeText.setText(String.format("Welcome %s", username));
            }

            // Load user preferences (theme, settings, etc.)
            loadUserPreferences();

        } catch (Exception e) {
            showErrorToast("Error loading user data: " + e.getMessage());
        }
    }

    private void loadUserPreferences() {
        try {
            SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);

            // Load any saved preferences
            boolean showCompletedTasks = prefs.getBoolean("show_completed_tasks", true);
            boolean enableNotifications = prefs.getBoolean("enable_notifications", true);
            String themeMode = prefs.getString("theme_mode", "light");

            // Apply preferences
            applyUserPreferences(showCompletedTasks, enableNotifications, themeMode);

        } catch (Exception e) {
            showErrorToast("Error loading preferences: " + e.getMessage());
        }
    }

    private void applyUserPreferences(boolean showCompleted, boolean notifications, String theme) {
        // Apply theme if needed
        // Apply other preferences
    }

    private void setupNotificationSystem() {
        if (notificationRepository == null) return;

        try {
            // Observe unread notification count
            notificationRepository.getUnreadCount().observe(this, count -> {
                updateNotificationBadge(count);
            });

            // Check if opened from notification
            checkNotificationIntent();

        } catch (Exception e) {
            showErrorToast("Error setting up notifications: " + e.getMessage());
        }
    }

    private void setupTaskCompletionReceiver() {
        taskCompletionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String taskId = intent.getStringExtra(EXTRA_TASK_ID);
                String taskType = intent.getStringExtra(EXTRA_TASK_TYPE);

                if (ACTION_TASK_COMPLETED.equals(action)) {
                    handleTaskCompleted(taskId, taskType);
                } else if (ACTION_TASK_UNCOMPLETED.equals(action)) {
                    handleTaskUncompleted(taskId, taskType);
                }
            }
        };

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TASK_COMPLETED);
        filter.addAction(ACTION_TASK_UNCOMPLETED);
        LocalBroadcastManager.getInstance(this).registerReceiver(taskCompletionReceiver, filter);
    }

    private void handleTaskCompleted(String taskId, String taskType) {
        // Update local counters immediately for smooth UI
        if ("daily".equals(taskType)) {
            totalCompletedTasks++;
            totalPendingTasks = Math.max(0, totalPendingTasks - 1);

            // Update the daily task in the list
            updateDailyTaskCompletion(taskId, true);
        }

        // Update UI with animation
        updateStats(totalCompletedTasks, totalPendingTasks, totalPriorityTasks);

        // Show celebration effect
        showTaskCompletionCelebration();

        // Refresh data from repository to ensure consistency
        refreshTaskData();
    }

    private void handleTaskUncompleted(String taskId, String taskType) {
        // Update local counters
        if ("daily".equals(taskType)) {
            totalCompletedTasks = Math.max(0, totalCompletedTasks - 1);
            totalPendingTasks++;

            // Update the daily task in the list
            updateDailyTaskCompletion(taskId, false);
        }

        // Update UI
        updateStats(totalCompletedTasks, totalPendingTasks, totalPriorityTasks);

        // Refresh data from repository
        refreshTaskData();
    }

    private void updateDailyTaskCompletion(String taskId, boolean isCompleted) {
        for (DailyTask task : dailyTasks) {
            if (taskId.equals(String.valueOf(task.getId()))) {
                task.setDone(isCompleted);
                break;
            }
        }

        // Notify adapter of changes
        if (dailyAdapter != null) {
            dailyAdapter.notifyDataSetChanged();
        }
    }

    private void showTaskCompletionCelebration() {
        // Add a subtle celebration animation
        View completedCounter = findViewById(R.id.completedTasksCount);
        if (completedCounter != null) {
            completedCounter.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        completedCounter.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start();
                    })
                    .start();
        }
    }

    private void refreshTaskData() {
        // Delay refresh slightly to allow Firestore to sync
        animationHandler.postDelayed(() -> {
            String today = DateUtils.getCurrentDate("MMM dd, yyyy");
            if (repository != null) {
                repository.refreshTasksFromFirestore("daily", today);
                repository.refreshTasksFromFirestore("priority", today);
            }
        }, 500);
    }

    private void updateNotificationBadge(Integer count) {
        if (notificationBadge == null) return;

        try {
            if (count != null && count > 0) {
                notificationBadge.setVisibility(View.VISIBLE);
                notificationBadge.setText(String.valueOf(count));

                // Animate badge appearance
                animateBadgeAppearance();
            } else {
                notificationBadge.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            showErrorToast("Error updating notification badge: " + e.getMessage());
        }
    }

    private void animateBadgeAppearance() {
        if (notificationBadge == null) return;

        notificationBadge.setScaleX(0.5f);
        notificationBadge.setScaleY(0.5f);
        notificationBadge.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void checkNotificationIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("open_notifications", false)) {
            // Delay to allow UI to settle
            animationHandler.postDelayed(this::showNotificationsDialog, 500);
        }
    }

    private void updateTaskData(List<Task> tasks) {
        try {
            // Clear existing data
            priorityTasks.clear();
            dailyTasks.clear();

            // Reset counters
            int completed = 0, pending = 0, priority = 0;

            // Process tasks
            for (Task task : tasks) {
                if ("priority".equals(task.getType())) {
                    priorityTasks.add(new PriorityTask(task.getId(), task.getTitle(), 0));
                    priority++;
                } else if ("daily".equals(task.getType())) {
                    DailyTask dailyTask = new DailyTask(task.getId(), task.getTitle(), task.isDone(), false);
                    dailyTasks.add(dailyTask);

                    if (task.isDone()) {
                        completed++;
                    } else {
                        pending++;
                    }
                }
            }

            // Update UI
            updateAdapters();
            updateStats(completed, pending, priority);
            updateEmptyState();

            // Animate tasks in
            animationHandler.postDelayed(this::animateTasksIn, 100);

        } catch (Exception e) {
            showErrorToast("Error updating task data: " + e.getMessage());
        }
    }

    private void updateAdapters() {
        try {
            if (priorityAdapter != null) {
                priorityAdapter.setViewModel(taskViewModel);
                priorityAdapter.notifyDataSetChanged();
            }

            if (dailyAdapter != null) {
                dailyAdapter.setViewModel(taskViewModel);
                dailyAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            showErrorToast("Error updating adapters: " + e.getMessage());
        }
    }

    private void updateStats(int completed, int pending, int priority) {
        totalCompletedTasks = completed;
        totalPendingTasks = pending;
        totalPriorityTasks = priority;

        // Animate counter updates
        animateCounter(completedTasksCount, completed);
        animateCounter(pendingTasksCount, pending);
        animateCounter(priorityTasksCount, priority);
    }

    private void updateEmptyState() {
        if (emptyStateLayout == null) return;

        boolean hasAnyTasks = !priorityTasks.isEmpty() || !dailyTasks.isEmpty();
        int visibility = hasAnyTasks ? View.GONE : View.VISIBLE;

        if (emptyStateLayout.getVisibility() != visibility) {
            emptyStateLayout.setVisibility(visibility);

            if (visibility == View.VISIBLE) {
                animateEmptyStateIn();
            }
        }
    }

    private void animateEmptyStateIn() {
        if (emptyStateLayout == null) return;

        emptyStateLayout.setAlpha(0f);
        emptyStateLayout.setTranslationY(50f);
        emptyStateLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateCounter(TextView textView, int targetValue) {
        if (textView == null) return;

        try {
            int currentValue = 0;
            String currentText = textView.getText().toString();
            if (!currentText.isEmpty()) {
                currentValue = Integer.parseInt(currentText);
            }

            ValueAnimator animator = ValueAnimator.ofInt(currentValue, targetValue);
            animator.setDuration(300);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                if (textView != null) {
                    textView.setText(String.valueOf(animation.getAnimatedValue()));
                }
            });
            animator.start();

        } catch (Exception e) {
            // Fallback to direct update
            textView.setText(String.valueOf(targetValue));
        }
    }

    private void animateTasksIn() {
        // Animate priority tasks
        if (priorityRecyclerView != null) {
            for (int i = 0; i < priorityRecyclerView.getChildCount(); i++) {
                View child = priorityRecyclerView.getChildAt(i);
                if (child != null) {
                    child.setAlpha(0f);
                    child.setTranslationY(50f);
                    child.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(300)
                            .setStartDelay(i * 100)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }
        }

        // Animate daily tasks
        if (dailyRecyclerView != null) {
            for (int i = 0; i < dailyRecyclerView.getChildCount(); i++) {
                View child = dailyRecyclerView.getChildAt(i);
                if (child != null) {
                    child.setAlpha(0f);
                    child.setTranslationX(-50f);
                    child.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(300)
                            .setStartDelay(i * 80)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }
        }
    }

    private void animateButtonPress(View button) {
        if (button == null) return;

        ValueAnimator scaleDown = ValueAnimator.ofFloat(1f, 0.95f);
        scaleDown.setDuration(100);
        scaleDown.setInterpolator(new DecelerateInterpolator());
        scaleDown.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            button.setScaleX(scale);
            button.setScaleY(scale);
        });

        ValueAnimator scaleUp = ValueAnimator.ofFloat(0.95f, 1f);
        scaleUp.setDuration(100);
        scaleUp.setInterpolator(new DecelerateInterpolator());
        scaleUp.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            button.setScaleX(scale);
            button.setScaleY(scale);
        });

        scaleDown.start();
        scaleDown.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                scaleUp.start();
            }
        });
    }

    private void navigateToAddTask(String taskType) {
        try {
            Intent intent = new Intent(this, AddTaskActivity.class);
            if (!taskType.isEmpty()) {
                intent.putExtra("task_type", taskType);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } catch (Exception e) {
            showErrorToast("Error navigating to add task: " + e.getMessage());
        }
    }

    private void navigateToTaskDetails(Task task) {
        try {
            fadeOutAndNavigate(() -> {
                Intent intent = new Intent(this, TaskDetailsActivity.class);
                intent.putExtra("title", task.getTitle());
                intent.putExtra("description", task.getDescription());
                intent.putExtra("endDate", task.getEndDate());
                intent.putExtra("hasLocation", task.hasLocation());
                intent.putExtra("location", task.getLocation());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        } catch (Exception e) {
            showErrorToast("Error navigating to task details: " + e.getMessage());
        }
    }

    private void fadeOutAndNavigate(Runnable navigationAction) {
        View contentContainer = findViewById(R.id.content_container);
        if (contentContainer != null) {
            contentContainer.animate()
                    .alpha(0.7f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        navigationAction.run();
                        contentContainer.setAlpha(1f);
                    })
                    .start();
        } else {
            navigationAction.run();
        }
    }

    private void showNotificationsDialog() {
        if (notificationRepository == null) return;

        try {
            notificationRepository.getUnreadCount().observe(this, count -> {
                if (count == null || count == 0) {
                    showSimpleDialog("Notifications", "No new notifications", "OK");
                    return;
                }

                // Use default AlertDialog without custom styling first
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("📋 Notifications")
                        .setMessage("You have " + count + " unread notifications")
                        .setPositiveButton("View All", (dialog, which) -> {
                            showFullNotificationsList();
                        })
                        .setNegativeButton("Mark All Read", (dialog, which) -> {
                            markAllNotificationsRead();
                        })
                        .setNeutralButton("Cancel", null);

                AlertDialog dialog = builder.create();

                // Apply styling after creation to avoid button visibility issues
                dialog.setOnShowListener(dialogInterface -> {
                    try {
                        // Get buttons and ensure they're visible
                        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                    getResources().getColor(android.R.color.holo_blue_dark, getTheme()));
                        }
                        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                                    getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                        }
                        if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
                            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(
                                    getResources().getColor(android.R.color.darker_gray, getTheme()));
                        }
                    } catch (Exception e) {
                        // Fallback colors if custom colors fail
                        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFF2196F3);
                        }
                        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFF44336);
                        }
                        if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
                            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(0xFF757575);
                        }
                    }
                });

                dialog.show();
            });
        } catch (Exception e) {
            showErrorToast("Error showing notifications: " + e.getMessage());
        }
    }

    private void showFullNotificationsList() {
        if (notificationRepository == null) return;

        try {
            notificationRepository.getAllNotifications().observe(this, notifications -> {
                if (notifications == null || notifications.isEmpty()) {
                    showSimpleDialog("Notifications", "No notifications available", "OK");
                    return;
                }

                StringBuilder message = new StringBuilder();
                for (int i = 0; i < notifications.size(); i++) {
                    Notification notification = notifications.get(i);
                    String status = notification.isRead() ? "✅ " : "🔔 ";
                    message.append(status)
                            .append(notification.getTitle())
                            .append("\n");

                    if (i < notifications.size() - 1) {
                        message.append("─────────────────\n");
                    }
                }

                // Use default styling without custom background
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("📱 All Notifications")
                        .setMessage(message.toString())
                        .setPositiveButton("Mark All Read", (dialog, which) -> {
                            markAllNotificationsRead();
                        })
                        .setNegativeButton("Close", null);

                AlertDialog dialog = builder.create();

                // Apply button colors after showing
                dialog.setOnShowListener(dialogInterface -> {
                    try {
                        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFF2196F3);
                        }
                        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF757575);
                        }
                    } catch (Exception e) {
                        // Buttons will use default styling
                    }
                });

                dialog.show();
            });
        } catch (Exception e) {
            showErrorToast("Error showing notification list: " + e.getMessage());
        }
    }

    private void markAllNotificationsRead() {
        try {
            notificationRepository.markAllAsRead();
            showSuccessToast("All notifications marked as read");
        } catch (Exception e) {
            showErrorToast("Error marking notifications as read: " + e.getMessage());
        }
    }

    private void showSimpleDialog(String title, String message, String buttonText) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(buttonText, null)
                    .create();

            dialog.setOnShowListener(dialogInterface -> {
                try {
                    if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFF2196F3);
                    }
                } catch (Exception e) {
                    // Use default styling
                }
            });

            dialog.show();
        } catch (Exception e) {
            showErrorToast("Error showing dialog: " + e.getMessage());
        }
    }

    private void showErrorToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccessToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkNotificationIntent();
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            String today = DateUtils.getCurrentDate("MMM dd, yyyy");

            // Refresh data from Firestore
            if (repository != null) {
                repository.refreshTasksFromFirestore("priority", today);
                repository.refreshTasksFromFirestore("daily", today);
            }

        } catch (Exception e) {
            showErrorToast("Error refreshing data: " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Cancel any pending animations
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            // Unregister broadcast receiver
            if (taskCompletionReceiver != null) {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(taskCompletionReceiver);
            }

            // Clean up animations
            View contentContainer = findViewById(R.id.content_container);
            if (contentContainer != null) {
                contentContainer.clearAnimation();
            }

            // Clean up handler
            if (animationHandler != null) {
                animationHandler.removeCallbacksAndMessages(null);
                animationHandler = null;
            }

            // Clear adapters
            if (priorityAdapter != null) {
                priorityAdapter = null;
            }
            if (dailyAdapter != null) {
                dailyAdapter = null;
            }

        } catch (Exception e) {
            // Log error but don't show toast in onDestroy
        }
    }

    @Override
    protected int getCurrentNavItem() {
        return R.id.nav_home;
    }

    // Public methods for testing or external access
    public int getTotalCompletedTasks() {
        return totalCompletedTasks;
    }

    public int getTotalPendingTasks() {
        return totalPendingTasks;
    }

    public int getTotalPriorityTasks() {
        return totalPriorityTasks;
    }

    public boolean hasAnyTasks() {
        return !priorityTasks.isEmpty() || !dailyTasks.isEmpty();
    }
}