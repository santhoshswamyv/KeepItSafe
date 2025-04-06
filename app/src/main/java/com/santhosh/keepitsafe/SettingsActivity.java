package com.santhosh.keepitsafe;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_PASSWORD = "UserPassword";
    private static final String KEY_BIOMETRIC_ENABLED = "BiometricEnabled"; // Track biometric use

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        Button btnClearData = findViewById(R.id.btnClearData);
        Button btnResetPassword = findViewById(R.id.btnResetPassword);

        btnClearData.setOnClickListener(v -> showClearDataDialog());
        btnResetPassword.setOnClickListener(v -> showResetPasswordDialog());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    // Show Confirmation Dialog Before Clearing Data
    private void showClearDataDialog() {
        CustomDialog.showDialog(
                this,
                "Clear All Data",
                "Yes",
                "Cancel",
                false,
                "Are you sure? This will delete all app data!",
                "",
                input -> {
                    if(input != null) {
                        clearAllData();
                        redirectToAuthentication();
                    }
                }
        );
    }

    // Clear All Data (SharedPreferences & Internal Storage)
    private void clearAllData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Clear all stored data
        editor.apply();

        File directory = new File(getFilesDir(), "private_files"); // Access the "private_files" folder
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles(); // List all files in the folder
            if (files != null) {
                for (File file : files) {
                    file.delete(); // Delete each file inside "private_files"
                }
            }
        }

        Toast.makeText(this, "All data cleared!", Toast.LENGTH_SHORT).show();
    }


    // Show Reset Password Dialog
    private void showResetPasswordDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        // Get references to dialog components
        TextView dialogTitle = dialogView.findViewById(R.id.txtDialogTitle);
        Button btnPositive = dialogView.findViewById(R.id.btnDialogConfirm);
        Button btnNegative = dialogView.findViewById(R.id.btnDialogCancel);
        EditText editText = dialogView.findViewById(R.id.edtDialogInput);
        TextView dialogMessage = dialogView.findViewById(R.id.txtDialogMessage);

        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        dialogTitle.setText("Reset Password");
        btnPositive.setText("Confirm");
        btnNegative.setText("Cancel");
        editText.setVisibility(View.VISIBLE);
        dialogMessage.setText("Enter a new password to reset.");
        dialogMessage.setVisibility(View.VISIBLE);

        // Show the dialog
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        // Handle positive button click (confirm reset)
        btnPositive.setOnClickListener(v -> {
            String newPassword = editText.getText().toString().trim();

            if (newPassword.isEmpty()) {
                Toast.makeText(SettingsActivity.this, "Password cannot be empty!", Toast.LENGTH_SHORT).show();
            } else {
                resetPassword(newPassword); // Reset password logic
                redirectToAuthentication(); // Redirect user after password reset
                dialog.dismiss(); // Dismiss the dialog
            }
        });

        // Handle negative button click (cancel)
        btnNegative.setOnClickListener(v -> {
            dialog.dismiss(); // Simply dismiss the dialog
        });
    }

    // Reset Password and Enable Biometric Again
    private void resetPassword(String newPassword) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PASSWORD, newPassword);
        editor.putBoolean(KEY_BIOMETRIC_ENABLED, true); // Re-enable biometric
        editor.apply();

        Toast.makeText(this, "Password reset successfully!", Toast.LENGTH_SHORT).show();
    }

    private void redirectToAuthentication() {
        Intent intent = new Intent(this, AuthenticationActivity.class);
        startActivity(intent);
        finish();
    }
}