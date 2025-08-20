package com.s23010301.taskping.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
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
        SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault()); // "E" gives abbreviated day name (Sat, Sun, etc.)
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault()); // Just the day number

        String selectedDate = dateViewModel.getDate().getValue();
        int totalDays = 7;

        // Calculate text size based on screen density for better responsiveness
        float textSizeSp = 12;
        float scaledTextSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, textSizeSp, getResources().getDisplayMetrics());

        for (int i = 0; i < totalDays; i++) {
            Calendar day = (Calendar) today.clone();
            day.add(Calendar.DATE, i);

            String fullDateStr = fullFormat.format(day.getTime());
            String dayName = dayFormat.format(day.getTime()); // "Sat", "Sun", etc.
            String dayNumber = dateFormat.format(day.getTime()); // "19", "20", etc.

            // Create a vertical LinearLayout to hold day name and date
            LinearLayout dayLayout = new LinearLayout(this);
            dayLayout.setOrientation(LinearLayout.VERTICAL);
            dayLayout.setGravity(Gravity.CENTER);
            dayLayout.setTag(fullDateStr);

            // Set padding and margins
            int padding = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
            dayLayout.setPadding(padding, padding, padding, padding);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            int margin = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
            layoutParams.setMargins(margin, 0, margin, 0);
            dayLayout.setLayoutParams(layoutParams);

            // Day name TextView (Sat, Sun, Mon, etc.)
            TextView dayNameView = new TextView(this);
            dayNameView.setText(dayName);
            dayNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
            dayNameView.setTextColor(getResources().getColor(R.color.gray));
            dayNameView.setGravity(Gravity.CENTER);

            // Day number TextView
            TextView dayNumberView = new TextView(this);
            dayNumberView.setText(dayNumber);
            dayNumberView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp + 2); // Slightly larger
            dayNumberView.setTextColor(getResources().getColor(R.color.black));
            dayNumberView.setGravity(Gravity.CENTER);
            dayNumberView.setTypeface(null, Typeface.BOLD);

            // Add TextViews to the layout
            dayLayout.addView(dayNameView);
            dayLayout.addView(dayNumberView);

            // Styling for selected date
            if (fullDateStr.equals(selectedDate)) {
                dayLayout.setBackgroundResource(R.drawable.date_button_selected);
                dayNameView.setTextColor(getResources().getColor(R.color.white));
                dayNumberView.setTextColor(getResources().getColor(R.color.white));
            } else {
                dayLayout.setBackgroundResource(R.drawable.date_button_selector);
            }

            dayLayout.setOnClickListener(v -> {
                String clickedDate = (String) v.getTag();
                if (!clickedDate.equals(dateViewModel.getDate().getValue())) {
                    // Update all buttons
                    for (int j = 0; j < dateContainer.getChildCount(); j++) {
                        View child = dateContainer.getChildAt(j);
                        TextView childDayName = (TextView) ((LinearLayout) child).getChildAt(0);
                        TextView childDayNumber = (TextView) ((LinearLayout) child).getChildAt(1);

                        child.setBackgroundResource(R.drawable.date_button_selector);
                        childDayName.setTextColor(getResources().getColor(R.color.gray));
                        childDayNumber.setTextColor(getResources().getColor(R.color.black));
                    }

                    // Set selected state
                    v.setBackgroundResource(R.drawable.date_button_selected);
                    TextView selectedDayName = (TextView) ((LinearLayout) v).getChildAt(0);
                    TextView selectedDayNumber = (TextView) ((LinearLayout) v).getChildAt(1);
                    selectedDayName.setTextColor(getResources().getColor(R.color.white));
                    selectedDayNumber.setTextColor(getResources().getColor(R.color.white));

                    dateViewModel.setDate(clickedDate);
                }
            });

            dateContainer.addView(dayLayout);
        }

        // Observe date change to update UI
        dateViewModel.getDate().observe(this, newDate -> {
            for (int i = 0; i < dateContainer.getChildCount(); i++) {
                LinearLayout child = (LinearLayout) dateContainer.getChildAt(i);
                String childDate = (String) child.getTag();
                TextView dayNameView = (TextView) child.getChildAt(0);
                TextView dayNumberView = (TextView) child.getChildAt(1);

                if (childDate.equals(newDate)) {
                    child.setBackgroundResource(R.drawable.date_button_selected);
                    dayNameView.setTextColor(getResources().getColor(R.color.white));
                    dayNumberView.setTextColor(getResources().getColor(R.color.white));
                } else {
                    child.setBackgroundResource(R.drawable.date_button_selector);
                    dayNameView.setTextColor(getResources().getColor(R.color.gray));
                    dayNumberView.setTextColor(getResources().getColor(R.color.black));
                }
            }
        });
    }
}
