package com.santhosh.keepitsafe;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class CustomDialog {

    public static void showDialog(Context context, String title, String positiveText, String negativeText,
                                  boolean hasInput, String extraMessage, String defaultInput, DialogCallback callback) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        builder.setCancelable(false);

        // Get references to the dialog components
        TextView dialogTitle = dialogView.findViewById(R.id.txtDialogTitle);
        Button btnPositive = dialogView.findViewById(R.id.btnDialogConfirm);
        Button btnNegative = dialogView.findViewById(R.id.btnDialogCancel);
        EditText editText = dialogView.findViewById(R.id.edtDialogInput);
        TextView dialogMessage = dialogView.findViewById(R.id.txtDialogMessage);  // Extra message TextView

        dialogTitle.setText(title);
        btnPositive.setText(positiveText);
        btnNegative.setText(negativeText);

        // Show or hide the input field dynamically
        editText.setVisibility(hasInput ? View.VISIBLE : View.GONE);

        if (hasInput || !defaultInput.trim().isEmpty()) {
            editText.setVisibility(View.VISIBLE);
            editText.setText(defaultInput);
            editText.setSelection(defaultInput.length()); // Move cursor to end
        } else {
            editText.setVisibility(View.GONE);
        }

        // Handle extra message if provided
        if (extraMessage != null && !extraMessage.trim().isEmpty()) {
            dialogMessage.setText(extraMessage);
            dialogMessage.setVisibility(View.VISIBLE);
        } else {
            dialogMessage.setVisibility(View.GONE);
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnPositive.setOnClickListener(v -> {
            if (hasInput) {
                String inputText = editText.getText().toString().trim();
                callback.onConfirm(inputText);  // Pass the input to the callback
            } else {
                if (title.contains("Delete")){
                    callback.onConfirm("");
                }else{
                    callback.onConfirm(null);  // No input case
                }
            }
            dialog.dismiss();
        });

        btnNegative.setOnClickListener(v -> {
            callback.onConfirm(null); // This ensures the cancel action is handled
            dialog.dismiss();
        });

        dialog.show();
    }

    // Callback interface to handle dialog confirmation
    public interface DialogCallback {
        void onConfirm(String input);
    }
}
