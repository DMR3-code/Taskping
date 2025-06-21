package com.s23010301.taskping;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "TaskPingPrefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Optional: setContentView(R.layout.activity_splash); // if you have a splash layout

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean hasCompletedOnboarding = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);

            Intent intent;
            if (hasCompletedOnboarding) {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, OnBoardingActivity.class);
            }

            startActivity(intent);
            finish();
        }, 3000);
    }
}
