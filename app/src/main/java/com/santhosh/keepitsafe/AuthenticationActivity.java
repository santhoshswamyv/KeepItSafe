package com.santhosh.keepitsafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;

public class AuthenticationActivity extends AppCompatActivity {

    private EditText editTextPassword;
    private Button btnUnlock, btnBiometric;
    private TextView txtInstruction;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_PASSWORD = "UserPassword";
    private static final String KEY_BIOMETRIC_ENABLED = "BiometricEnabled"; // Track biometric use
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private int biometricFailCount = 0; // Track biometric failures

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        editTextPassword = findViewById(R.id.editTextPassword);
        btnUnlock = findViewById(R.id.btnUnlock);
        btnBiometric = findViewById(R.id.btnBiometric);
        //txtInstruction = findViewById(R.id.txtInstruction);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String savedPassword = sharedPreferences.getString(KEY_PASSWORD, null);
        boolean biometricEnabled = sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false);

        if (savedPassword == null) {
            //txtInstruction.setText("Set a new password");
            btnUnlock.setText("Set Password");
            btnBiometric.setVisibility(View.GONE); // Hide biometric if no password is set
        } else {
            //txtInstruction.setText("Enter your password");
            btnUnlock.setText("Unlock");
            if (biometricEnabled) {
                setupBiometricAuth();
            } else {
                btnBiometric.setVisibility(View.GONE);
            }
        }

        btnUnlock.setOnClickListener(v -> handlePassword(savedPassword));
    }

    private void handlePassword(String savedPassword) {
        String enteredPassword = editTextPassword.getText().toString().trim();

        if (enteredPassword.isEmpty()) {
            Toast.makeText(this, "Password cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (savedPassword == null) {
            // Set the password for the first time
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_PASSWORD, enteredPassword);
            editor.putBoolean(KEY_BIOMETRIC_ENABLED, true); // Enable biometric after setting password
            editor.apply();

            Toast.makeText(this, "Password Set Successfully! Biometric Enabled.", Toast.LENGTH_SHORT).show();
            redirectToMain();
        } else {
            if (enteredPassword.equals(savedPassword)) {
                redirectToMain();
            } else {
                Toast.makeText(this, "Incorrect Password!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void redirectToMain() {
        Intent intent = new Intent(AuthenticationActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupBiometricAuth() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            Executor executor = ContextCompat.getMainExecutor(this);
            biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    biometricFailCount = 0; // Reset fail counter
                    redirectToMain();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    biometricFailCount++;

                    if (biometricFailCount >= 3) {
                        biometricPrompt.cancelAuthentication();
                        Toast.makeText(AuthenticationActivity.this, "Too many failed attempts! Enter password.", Toast.LENGTH_SHORT).show();
                        btnBiometric.setVisibility(View.GONE); // Hide biometric button
                        editTextPassword.setVisibility(View.VISIBLE);
                        btnUnlock.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(AuthenticationActivity.this, "Biometric authentication failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Authentication")
                    .setSubtitle("Use fingerprint to unlock")
                    .setNegativeButtonText("Cancel")
                    .build();

            btnBiometric.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));
        } else {
            btnBiometric.setVisibility(View.GONE); // Hide button if biometric is not available
        }
    }
}
