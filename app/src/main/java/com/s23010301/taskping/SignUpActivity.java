package com.s23010301.taskping;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {

    private EditText usernameInput, emailInput, passwordInput, confirmPasswordInput;
    private FirebaseAuth mAuth;
    private MaterialButton btnRegister;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();


        // Find views
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        btnRegister = findViewById(R.id.btnRegister);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Back button click
        btnBack.setOnClickListener(v -> finish());

        // Register button click
        btnRegister.setOnClickListener(v -> handleRegister());
    }

    private void handleRegister() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (username.isEmpty() || email.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialButton btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setEnabled(false);
        btnRegister.setText("Registering...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Register");

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        getSharedPreferences("TaskPingPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("username", username)
                                .apply();
                        Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Registration failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
