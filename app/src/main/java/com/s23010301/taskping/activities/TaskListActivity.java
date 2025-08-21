package com.s23010301.taskping.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
    private final String[] tabTitles = new String[]{"Priority Tasks", "Daily Tasks"};
    private DateViewModel dateViewModel;
    private TaskViewModel taskViewModel;
    private LinearLayout selectedDateView;

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

        generateEnhancedDateStrip(dateContainer);

        // Enhanced button click with ripple effect
        btnAddTask.setOnClickListener(v -> {
            animateButtonClick(v);
            startActivity(new Intent(this, AddTaskActivity.class));
        });

        setupNavigationObserver();
    }

    private void animateButtonClick(View view) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);

        scaleDownX.start();
        scaleDownY.start();

        view.postDelayed(() -> {
            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f);
            scaleUpX.setDuration(100);
            scaleUpY.setDuration(100);
            scaleUpX.start();
            scaleUpY.start();
        }, 100);
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

    private void generateEnhancedDateStrip(LinearLayout dateContainer) {
        dateContainer.removeAllViews();

        Calendar today = Calendar.getInstance();
        SimpleDateFormat fullFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());

        String selectedDate = dateViewModel.getDate().getValue();
        int totalDays = 7;

        for (int i = 0; i < totalDays; i++) {
            Calendar day = (Calendar) today.clone();
            day.add(Calendar.DATE, i);

            String fullDateStr = fullFormat.format(day.getTime());
            String dayName = dayFormat.format(day.getTime());
            String dayNumber = dateFormat.format(day.getTime());

            // Create enhanced date card
            MaterialCardView dateCard = createEnhancedDateCard(dayName, dayNumber, fullDateStr, selectedDate);
            dateContainer.addView(dateCard);
        }

        // Observe date change to update UI
        dateViewModel.getDate().observe(this, newDate -> updateDateStripSelection(dateContainer, newDate));
    }

    private MaterialCardView createEnhancedDateCard(String dayName, String dayNumber, String fullDateStr, String selectedDate) {
        MaterialCardView dateCard = new MaterialCardView(this);

        // Card styling
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 76, getResources().getDisplayMetrics())
        );
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        cardParams.setMargins(margin, 0, margin, 0);
        dateCard.setLayoutParams(cardParams);

        dateCard.setRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()));
        dateCard.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
        dateCard.setTag(fullDateStr);

        // Inner layout
        LinearLayout dayLayout = new LinearLayout(this);
        dayLayout.setOrientation(LinearLayout.VERTICAL);
        dayLayout.setGravity(Gravity.CENTER);

        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        dayLayout.setPadding(padding, padding, padding, padding);

        // Day name TextView
        TextView dayNameView = new TextView(this);
        dayNameView.setText(dayName);
        dayNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        dayNameView.setGravity(Gravity.CENTER);
        dayNameView.setTypeface(null, Typeface.NORMAL);

        // Day number TextView
        TextView dayNumberView = new TextView(this);
        dayNumberView.setText(dayNumber);
        dayNumberView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        dayNumberView.setGravity(Gravity.CENTER);
        dayNumberView.setTypeface(null, Typeface.BOLD);

        dayLayout.addView(dayNameView);

        // Add some space between day name and number
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 4));
        dayLayout.addView(spacer);

        dayLayout.addView(dayNumberView);
        dateCard.addView(dayLayout);

        // Set initial styling
        boolean isSelected = fullDateStr.equals(selectedDate);
        updateDateCardStyling(dateCard, dayNameView, dayNumberView, isSelected);

        // Click listener with animation
        dateCard.setOnClickListener(v -> {
            String clickedDate = (String) v.getTag();
            if (!clickedDate.equals(dateViewModel.getDate().getValue())) {
                animateDateCardClick(dateCard);
                dateViewModel.setDate(clickedDate);
            }
        });

        return dateCard;
    }

    private void updateDateCardStyling(MaterialCardView dateCard, TextView dayNameView, TextView dayNumberView, boolean isSelected) {
        if (isSelected) {
            dateCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blue));
            dateCard.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics()));
            dayNameView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            dayNumberView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            selectedDateView = (LinearLayout) dateCard.getChildAt(0);
        } else {
            dateCard.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
            dateCard.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
            dayNameView.setTextColor(ContextCompat.getColor(this, R.color.gray));
            dayNumberView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
    }

    private void animateDateCardClick(View view) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.9f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.9f);
        scaleDownX.setDuration(150);
        scaleDownY.setDuration(150);

        scaleDownX.start();
        scaleDownY.start();

        view.postDelayed(() -> {
            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f);
            scaleUpX.setDuration(150);
            scaleUpY.setDuration(150);
            scaleUpX.start();
            scaleUpY.start();
        }, 150);
    }

    private void updateDateStripSelection(LinearLayout dateContainer, String newDate) {
        for (int i = 0; i < dateContainer.getChildCount(); i++) {
            MaterialCardView dateCard = (MaterialCardView) dateContainer.getChildAt(i);
            LinearLayout dayLayout = (LinearLayout) dateCard.getChildAt(0);
            String childDate = (String) dateCard.getTag();

            TextView dayNameView = (TextView) dayLayout.getChildAt(0);
            TextView dayNumberView = (TextView) dayLayout.getChildAt(2); // Account for spacer

            boolean isSelected = childDate.equals(newDate);
            updateDateCardStyling(dateCard, dayNameView, dayNumberView, isSelected);
        }
    }
}