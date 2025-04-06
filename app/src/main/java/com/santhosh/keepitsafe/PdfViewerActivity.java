package com.santhosh.keepitsafe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;
import com.shockwave.pdfium.PdfPasswordException;

import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {

    private PDFView pdfView;
    private String pdfPassword = null;
    private int passwordAttemptCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        pdfView = findViewById(R.id.pdfView);

        // Retrieve the file path from the intent
        String filePath = getIntent().getStringExtra("pdfFilePath");

        if (filePath != null) {
            File pdfFile = new File(filePath);
            if (pdfFile.exists()) {
                tryOpeningPdf(pdfFile);
            } else {
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error loading PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void tryOpeningPdf(File pdfFile) {
        pdfView.fromFile(pdfFile)
                .password(null) // Try opening without a password first
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .onError(t -> {
                    if (t instanceof PdfPasswordException) {
                        // PDF requires a password, prompt the user
                        promptForPassword(pdfFile);
                    } else {
                        Toast.makeText(this, "Error opening PDF: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .load();
    }

    private void displayPdf(File pdfFile, String password) {
        pdfView.fromFile(pdfFile)
                .password(password)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .onError(t -> {
                    if (t instanceof PdfPasswordException) {
                        if (passwordAttemptCount < 2) { // Limit retries to 3 attempts
                            passwordAttemptCount++;
                            Toast.makeText(this, "Incorrect password. Try again.", Toast.LENGTH_SHORT).show();
                            promptForPassword(pdfFile);
                        } else {
                            Toast.makeText(this, "Too many failed attempts. Cannot open PDF.", Toast.LENGTH_LONG).show();
                            // Navigate back to the main activity
                            Intent intent = new Intent(PdfViewerActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
                            startActivity(intent);
                            finish(); // Close the current activity
                        }
                    } else {
                        Toast.makeText(this, "Error opening PDF: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .load();
    }


    private void promptForPassword(File pdfFile) {
        // Using CustomDialog to prompt for the password
        CustomDialog.showDialog(this,
                "Protected PDF",
                "OK",
                "Cancel",
                true,
                "Enter PDF Password",
                "",
                input -> {
                    if (input == null || input.isEmpty()) {
                        Toast.makeText(PdfViewerActivity.this, "Cannot open protected PDF without a password", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(PdfViewerActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish(); // Close the current activity
                    } else {
                        pdfPassword = input.trim();
                        displayPdf(pdfFile, pdfPassword);
                    }
                });
    }

}