package com.s23010301.taskping.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.s23010301.taskping.R;
import com.s23010301.taskping.adapters.TaskPagerAdapter;
import com.s23010301.taskping.models.Task;
import com.s23010301.taskping.models.TaskViewModel;
import com.s23010301.taskping.utils.DateUtils;
import com.s23010301.taskping.models.DateViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskListActivity extends BaseActivity {
    private final String[] tabTitles = new String[]{"Priority Task", "Daily Task"};
    private DateViewModel dateViewModel;
    private TaskViewModel taskViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_bottom_nav);

        FrameLayout container = findViewById(R.id.content_container);
        ViewGroup taskListContent = (ViewGroup) getLayoutInflater()
                .inflate(R.layout.activity_task_list_content, container);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        dateViewModel = new ViewModelProvider(this).get(DateViewModel.class);

        TabLayout tabLayout = taskListContent.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = taskListContent.findViewById(R.id.viewPager);
        TextView monthText = taskListContent.findViewById(R.id.monthText);
        MaterialButton btnAddTask = taskListContent.findViewById(R.id.btnAddTask);
        LinearLayout dateContainer = taskListContent.findViewById(R.id.dateContainer);

        monthText.setText(DateUtils.getCurrentDate("MMMM, yyyy"));

        String today = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
        if (dateViewModel.getDate().getValue() == null) {
            dateViewModel.setDate(today);
        }

        TaskPagerAdapter pagerAdapter = new TaskPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        generateDateStrip(dateContainer);
        btnAddTask.setOnClickListener(v -> startActivity(new Intent(this, AddTaskActivity.class)));

        setupNavigationObserver();

    }

    private void setupNavigationObserver() {
        taskViewModel.navigateToTaskDetails().observe(this, event -> {
            Task task = event.getContentIfNotHandled();
            if (task != null) {
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
    protected int getCurrentNavItem() {
        return R.id.nav_tasks;
    }

    private void generateDateStrip(LinearLayout dateContainer) {
        dateContainer.removeAllViews();

        Calendar today = Calendar.getInstance();
        SimpleDateFormat fullFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat labelFormat = new SimpleDateFormat("EEE\ndd", Locale.getDefault());

        String selectedDate = dateViewModel.getDate().getValue();
        int totalDays = 7;

        for (int i = 0; i < totalDays; i++) {
            Calendar day = (Calendar) today.clone();
            day.add(Calendar.DATE, i);
            String fullDateStr = fullFormat.format(day.getTime());
            String label = labelFormat.format(day.getTime());

            MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(label);
            btn.setTag(fullDateStr);
            btn.setTextSize(12);
            btn.setAllCaps(false);
            btn.setPadding(24, 24, 24, 24);
            btn.setBackgroundResource(R.drawable.date_button_selector);
            btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            btn.setStrokeWidth(0);

            if (fullDateStr.equals(selectedDate)) {
                btn.setSelected(true);
            }

            btn.setOnClickListener(v -> {
                String clickedDate = (String) v.getTag();
                if (!clickedDate.equals(dateViewModel.getDate().getValue())) {
                    for (int j = 0; j < dateContainer.getChildCount(); j++) {
                        dateContainer.getChildAt(j).setSelected(false);
                    }
                    v.setSelected(true);
                    dateViewModel.setDate(clickedDate); // 🔹 will auto-update fragments
                }
            });

            dateContainer.addView(btn);
        }

        // Observe date change to update UI
        dateViewModel.getDate().observe(this, newDate -> {
            for (int i = 0; i < dateContainer.getChildCount(); i++) {
                View child = dateContainer.getChildAt(i);
                child.setSelected(child.getTag().equals(newDate));
            }
        });
    }
}
