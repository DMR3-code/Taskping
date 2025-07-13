package com.s23010301.taskping;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

        // Setup ViewPager
        TaskPagerAdapter pagerAdapter = new TaskPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Attach tabs
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        // Date Strip
        generateDateStrip();

        // Add Task Button
        btnAddTask.setOnClickListener(view -> {
            startActivity(new Intent(TaskListActivity.this, AddTaskActivity.class));
        });

        // Bottom Nav
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        NavigationHelper.setupBottomNavigation(bottomNav, this);
    }

    private void generateDateStrip() {
        LinearLayout dateContainer = findViewById(R.id.dateContainer);
        dateContainer.removeAllViews();

        Calendar today = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat fullFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        int totalDays = 7;

        for (int i = 0; i < totalDays; i++) {
            Calendar day = (Calendar) today.clone();
            day.add(Calendar.DATE, i);

            String displayText = dayFormat.format(day.getTime()) + "\n" + dateFormat.format(day.getTime());
            String fullDateStr = fullFormat.format(day.getTime());

            MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(displayText);
            btn.setTag(fullDateStr);
            btn.setTextSize(12);
            btn.setAllCaps(false);
            btn.setPadding(24, 24, 24, 24);
            btn.setBackgroundResource(R.drawable.date_button_selector);
            btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            btn.setStrokeWidth(0);

            if (i == 0) {
                btn.setSelected(true);
                DateSelectionHelper.setSelectedDate(fullDateStr);
            }

            btn.setOnClickListener(v -> {
                for (int j = 0; j < dateContainer.getChildCount(); j++) {
                    dateContainer.getChildAt(j).setSelected(false);
                }
                v.setSelected(true);
                String selectedDate = (String) v.getTag();
                DateSelectionHelper.setSelectedDate(selectedDate);

                // Trigger fragment refresh
                ViewPager2 viewPager = findViewById(R.id.viewPager);
                int currentTab = viewPager.getCurrentItem();
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + currentTab);
                if (currentFragment instanceof DailyTaskFragment) {
                    ((DailyTaskFragment) currentFragment).refreshTasks();
                } else if (currentFragment instanceof PriorityTaskFragment) {
                    ((PriorityTaskFragment) currentFragment).refreshTasks();
                }
            });

            dateContainer.addView(btn);
        }
    }
}
