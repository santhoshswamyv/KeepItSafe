package com.santhosh.keepitsafe;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private FilesAdapter filesAdapter;
    private final ArrayList<File> uploadedFiles = new ArrayList<>();

    // Launch file selector for file picking
    ActivityResultLauncher<Intent> fileSelectLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        promptForFileName(fileUri);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "No files selected", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppCompatImageButton btnUpload = findViewById(R.id.btnUpload);
        ImageView btnSettings = findViewById(R.id.btnSettings);
        RecyclerView recyclerViewFiles = findViewById(R.id.recyclerViewFiles);

        // Create the adapter for displaying files in the RecyclerView
        filesAdapter = new FilesAdapter(MainActivity.this, uploadedFiles);

        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFiles.setAdapter(filesAdapter);

        loadFilesFromPrivateStorage();

        // Handle the button click to open the file selector
        btnUpload.setOnClickListener(v -> openFileSelector());
        btnSettings.setOnClickListener(v -> openSettings());

    }

    // Load files from private storage
    private void loadFilesFromPrivateStorage() {
        File appDirectory = new File(getFilesDir(), "private_files");
        if (appDirectory.exists() && appDirectory.isDirectory()) {
            File[] files = appDirectory.listFiles();
            if (files != null) {
                uploadedFiles.addAll(Arrays.asList(files));
                filesAdapter.notifyDataSetChanged();
            }
        }
    }

    // Open file selector dialog to select files
    private void openFileSelector() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "image/*"});
        fileSelectLauncher.launch(intent);
    }

    private void openSettings(){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        finish();
    }

    // Prompt for a file name before saving it
    private void promptForFileName(Uri fileUri) {
        String originalFileName = getFileNameFromUri(fileUri);

        // Using the custom dialog to prompt for the file name
        CustomDialog.showDialog(this,
                "Enter a File Name",
                "Save",
                "Cancel",
                true,
                "",
                originalFileName,
                input -> {
            if (input != null && !input.isEmpty()) {
                // Clean the file name to remove any extension entered by the user
                String fileName = cleanFileName(input);

                // Get the file extension based on the MIME type of the file
                String extension = getFileExtension(fileUri);

                // Save the file with the correct name and extension
                saveFileToPrivateStorage(fileUri, fileName, extension);

            }

            if (input != null && input.isEmpty()) {
                Toast.makeText(MainActivity.this, "File name cannot be empty", Toast.LENGTH_SHORT).show();
            }

        });
    }

    public String getFileNameFromUri(Uri uri) {
        String fileName = null;

        // Query the content provider for the file details
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (columnIndex != -1) {
                    fileName = cursor.getString(columnIndex);
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex != -1) {
                        fileName = fileName.substring(0, dotIndex); // Remove the extension part
                    }
                }
            }
            cursor.close();
        }

        return fileName;
    }

    private void saveFileToPrivateStorage(Uri fileUri, String fileName, String extension) {
        File appDirectory = new File(getFilesDir(), "private_files");
        if (!appDirectory.exists()) {
            appDirectory.mkdirs();  // Ensures all parent directories are created
        }

        // Combine the user-entered file name with the extension
        String finalFileName = fileName + extension;
        File outputFile = new File(appDirectory, finalFileName);

        if (outputFile.exists()) {
            CustomDialog.showDialog(
                    this,
                    "File already exists",
                    "Yes",
                    "No",
                    false,
                    "Do you want to replace the existing file?",
                    "",
                    input -> {
                        try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
                            if (inputStream != null) {
                                overwriteFile(inputStream, outputFile);
                                uploadedFiles.remove(outputFile);
                                uploadedFiles.add(outputFile);
                                filesAdapter.notifyDataSetChanged();
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Error reopening file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        } else {
            try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
                if (inputStream != null) {
                    overwriteFile(inputStream, outputFile);
                    uploadedFiles.add(outputFile);
                    filesAdapter.notifyDataSetChanged();
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Clean the file name to ensure there are no invalid characters or extension
    private String cleanFileName(String fileName) {
        if (fileName.contains(".")) {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        return fileName;
    }

    // Get the file extension based on the MIME type
    private String getFileExtension(Uri fileUri) {
        String extension = null;
        String mimeType = this.getContentResolver().getType(fileUri); // Use context.getContentResolver()

        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

                // Ensure the extension has a dot
                if (extension == null) {
                    extension = ".jpg"; // Default for images
                } else {
                    extension = "." + extension;
                }
            } else if (mimeType.equals("application/pdf")) {
                extension = ".pdf"; // Ensure dot is present
            }
        }

        return extension;
    }


    // Overwrite the file in private storage
    private void overwriteFile(InputStream inputStream, File outputFile) {
        try (OutputStream outputStream = Files.newOutputStream(outputFile.toPath())) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            Toast.makeText(this, "File saved: " + outputFile.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
