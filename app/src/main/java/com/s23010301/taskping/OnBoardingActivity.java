package com.s23010301.taskping;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.List;
import java.util.ArrayList;

public class OnBoardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext, btnSkip;
    private LinearLayout dotsLayout;
    private TextView[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);
        dotsLayout = findViewById(R.id.layoutDots);

        List<OnBoardingItem> items = new ArrayList<>();
        items.add(new OnBoardingItem(R.drawable.onboard1, "Easy Time Management", "With management based on priority and daily tasks, it will give you convenience in managing and determining the tasks that must be done first"));
        items.add(new OnBoardingItem(R.drawable.onboard2, "Increase Work Effectiveness", "Time management and the determination of more important tasks will give your job statistics better and always improve"));
        items.add(new OnBoardingItem(R.drawable.onboard3, "Reminder Notification", "The advantage of this application is that it also provides reminders so you don’t forget to do your assignments on time and location"));

        OnBoardingAdapter adapter = new OnBoardingAdapter(items);
        viewPager.setAdapter(adapter);

        viewPager.setAdapter(adapter);

        addDots(0); // initial dot
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                addDots(position);
                btnNext.setText(position == 2 ? "Get Started" : "Next");
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < 2) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                startLogin();
            }
        });

        btnSkip.setOnClickListener(v -> startLogin());
    }

    private void addDots(int position) {
        dots = new TextView[3]; // 3 screens
        dotsLayout.removeAllViews();

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText("●");
            dots[i].setTextSize(18);
            dots[i].setTextColor(getResources().getColor(i == position ?
                    R.color.blue : R.color.gray, getTheme()));
            dotsLayout.addView(dots[i]);
        }
    }

    private void startLogin() {
        startActivity(new Intent(OnBoardingActivity.this, LoginActivity.class));
        finish();
    }
}
