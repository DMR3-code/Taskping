package com.s23010301.taskping;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskListActivity extends AppCompatActivity {

    private final String[] tabTitles = new String[]{"Priority Task", "Daily Task"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

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
    }
}
