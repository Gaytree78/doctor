package com.example.hostipal_info;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    View btnLogin, ivTogglePassword;
    CheckBox cbRemember;

    SharedPreferences prefs;

    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        // ✅ AUTO LOGIN CHECK
        if (prefs.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, DoctorDashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Views
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        cbRemember = findViewById(R.id.cbRemember);

        // LOGIN
        btnLogin.setOnClickListener(v -> {

            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (username.equals("admin") && password.equals("admin123")) {

                // ✅ SAVE LOGIN IF CHECKED
                if (cbRemember.isChecked()) {
                    prefs.edit()
                            .putBoolean("isLoggedIn", true)
                            .putString("username", username)
                            .apply();
                }

                startActivity(new Intent(this, DoctorDashboardActivity.class));
                finish();

            } else {
                Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
            }
        });

        // PASSWORD TOGGLE
        ivTogglePassword.setOnClickListener(v -> {

            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                isPasswordVisible = false;
                ivTogglePassword.setBackgroundResource(R.drawable.visibility);
            } else {
                etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                isPasswordVisible = true;
                ivTogglePassword.setBackgroundResource(R.drawable.visibility_off);
            }

            etPassword.setSelection(etPassword.getText().length());
        });
    }
}