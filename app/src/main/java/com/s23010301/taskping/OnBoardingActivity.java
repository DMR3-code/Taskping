package com.s23010301.taskping;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class OnBoardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext, btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);

        OnBoardingAdapter adapter = new OnBoardingAdapter(this);
        viewPager.setAdapter(adapter);

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < 2) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                startLogin();
            }
        });

        btnSkip.setOnClickListener(v -> startLogin());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                btnNext.setText(position == 2 ? "Get Started" : "Next");
            }
        });
    }

    private void startLogin() {
        startActivity(new Intent(OnBoardingActivity.this, LoginActivity.class));
        finish();
    }
}
