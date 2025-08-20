package com.s23010301.taskping.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.s23010301.taskping.R;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Auto-login
        if (mAuth.getCurrentUser() != null) {
            fetchUserDataAndGoToDashboard(mAuth.getCurrentUser().getUid());
            return;
        }

        // Views
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        TextView signUpLink = findViewById(R.id.signUpLink);
        TextView forgotPassword = findViewById(R.id.forgotPassword);

        btnLogin.setOnClickListener(v -> handleLogin());

        signUpLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        });

        forgotPassword.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void handleLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        fetchUserDataAndGoToDashboard(userId);
                    } else {
                        Toast.makeText(this, "Login failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserDataAndGoToDashboard(String userId) {
        // Try to get user data from Firestore first
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("name")) {
                        // User data exists in Firestore
                        String username = documentSnapshot.getString("name");
                        saveUsernameToPreferences(username);
                        goToDashboard(username);
                    } else {
                        // Fallback to email as username
                        assert mAuth.getCurrentUser() != null;
                        String email = mAuth.getCurrentUser().getEmail();
                        String fallbackUsername = email != null ?
                                email.split("@")[0] : "User"; // Use part before @ as username
                        saveUsernameToPreferences(fallbackUsername);
                        goToDashboard(fallbackUsername);
                    }
                })
                .addOnFailureListener(e -> {
                    // If Firestore fails, use email as fallback
                    assert mAuth.getCurrentUser() != null;
                    String email = mAuth.getCurrentUser().getEmail();
                    String fallbackUsername = email != null ?
                            email.split("@")[0] : "User";
                    saveUsernameToPreferences(fallbackUsername);
                    goToDashboard(fallbackUsername);
                });
    }

    private void saveUsernameToPreferences(String username) {
        SharedPreferences prefs = getSharedPreferences("TaskPingPrefs", MODE_PRIVATE);
        prefs.edit().putString("username", username).apply();
    }

    private void goToDashboard(String username) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }
}