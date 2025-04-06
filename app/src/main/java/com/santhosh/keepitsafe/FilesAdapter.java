package com.santhosh.keepitsafe;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {
    private final ArrayList<File> uploadedFiles;
    private final Context context;

    public FilesAdapter(Context context, ArrayList<File> uploadedFiles) {
        this.context = context;
        this.uploadedFiles = uploadedFiles;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        if (position < 0 || position >= uploadedFiles.size()) return; // Prevent index out of bounds

        File file = uploadedFiles.get(position);
        String fileName = file.getName();

        // Format last modified date
        String lastModified = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                .format(new Date(file.lastModified()));

        holder.fileName.setText(fileName);
        holder.fileDateTime.setText("Last Modified On: " + lastModified);

        String fileExtension = getFileExtension(file);
        if (fileExtension != null) {
            if (fileExtension.equals(".pdf")) {
                holder.fileIcon.setImageResource(R.drawable.pdf);  // Set PDF icon
            } else if (fileExtension.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                holder.fileIcon.setImageResource(R.drawable.jpg); // Set Image icon
            } else {
                holder.fileIcon.setImageResource(R.drawable.file_question); // Set a default file icon
            }
        }

        // Set listeners for delete and rename actions
        holder.btnDelete.setOnClickListener(v -> deleteFile(file, position));
        holder.btnRename.setOnClickListener(v -> renameFile(holder, file, position));

        // Open file action
        holder.itemView.setOnClickListener(v -> {
            if (!file.exists()) return;
            String fName = file.getName().toLowerCase();

            if (fName.endsWith(".pdf")) {
                // Open PDF in your custom PDF viewer
                Intent intent = new Intent(context, PdfViewerActivity.class);
                intent.putExtra("pdfFilePath", file.getAbsolutePath());
                context.startActivity(intent);
            } else if (fName.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Required for API 24+

                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Unsupported file format", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteFile(File file, int position) {
        if (position < 0 || position >= uploadedFiles.size()) return;

        CustomDialog.showDialog(context,
                "Delete " + file.getName() + " ?",
                "Yes",
                "Cancel",
                false,
                "Are you sure you want to delete this file?",
                "",
                input -> {
                    if (input != null && file.exists() && file.delete()) {
                        uploadedFiles.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, uploadedFiles.size());
                    }
                });
    }

    private void renameFile(FileViewHolder holder, File file, int position) {
        if (position < 0 || position >= uploadedFiles.size()) return;

        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf(".");
        String originalFileName = (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;

        CustomDialog.showDialog(context,
                "Rename File",
                "Rename",
                "Cancel",
                true,
                "Enter New File Name",
                originalFileName,
                input -> {
                    if (input == null || input.trim().isEmpty()) return;

                    String newFileName = input.trim();
                    String fileExtension = getFileExtension(file);

                    if (!newFileName.endsWith(fileExtension)) {
                        newFileName += fileExtension;
                    }

                    File newFile = new File(file.getParent(), newFileName);

                    if (newFile.exists() || file.renameTo(newFile)) {
                        newFile.setLastModified(System.currentTimeMillis());
                        holder.fileDateTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.getDefault())
                                .format(new Date(newFile.lastModified())));

                        uploadedFiles.set(position, newFile);
                        notifyItemChanged(position);
                    }
                });
    }

    private String getFileExtension(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex > 0) ? fileName.substring(dotIndex) : "";
    }

    @Override
    public int getItemCount() {
        return uploadedFiles.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, fileDateTime;
        ImageView btnDelete, btnRename, fileIcon;

        public FileViewHolder(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.txtFileName);
            fileDateTime = itemView.findViewById(R.id.txtFileDateTime);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnRename = itemView.findViewById(R.id.btnRename);
        }
    }
}
