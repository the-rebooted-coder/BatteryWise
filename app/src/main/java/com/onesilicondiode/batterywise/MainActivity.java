package com.onesilicondiode.batterywise;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.Manifest;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {
    MaterialButton startSaving, stopSaving;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission();
        }
        // Initialize the MediaPlayer
        startSaving = findViewById(R.id.saveBatteryBtn);
        stopSaving = findViewById(R.id.closeBatteryBtn);

        // Check if the service is running and update button visibility
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isServiceRunning = prefs.getBoolean("isServiceRunning", false);

        if (isServiceRunning) {
            startSaving.setVisibility(View.INVISIBLE);
            stopSaving.setVisibility(View.VISIBLE);
        } else {
            startSaving.setVisibility(View.VISIBLE);
            stopSaving.setVisibility(View.INVISIBLE);
        }

        startSaving.setOnClickListener(view -> {
            Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
            startService(serviceIntent);
            startSaving.setVisibility(View.INVISIBLE);
            stopSaving.setVisibility(View.VISIBLE);
            Toast.makeText(MainActivity.this, "Service Enabled", Toast.LENGTH_SHORT).show();

            // Update shared preferences
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean("isServiceRunning", true);
            editor.apply();
            finish();
        });

        stopSaving.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent serviceIntent = new Intent(MainActivity.this, BatteryMonitorService.class);
                stopService(serviceIntent);
                Toast.makeText(MainActivity.this, "Service Stopped", Toast.LENGTH_SHORT).show();
                stopSaving.setVisibility(View.INVISIBLE);
                startSaving.setVisibility(View.VISIBLE);

                // Update shared preferences
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean("isServiceRunning", false);
                editor.apply();
            }
        });
    }
    private void checkNotificationPermission() {
        String permission = Manifest.permission.POST_NOTIFICATIONS;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, proceed with your action here
            // You can call the method for your action
        } else if (shouldShowRequestPermissionRationale(permission)) {
            // Permission denied previously, show rationale dialog
            showPermissionRationaleDialog();
        } else {
            // Request notification permission
            requestNotificationPermission.launch(permission);
        }
    }
    private void showPermissionRationaleDialog() {
        // For example, you can use a AlertDialog:
        new AlertDialog.Builder(this)
                .setTitle("Notification Permission is Required")
                .setMessage("Permissions are required to show notification in-order to keep the battery monitoring service active.")
                .setPositiveButton("GRANT", (dialog, which) -> {
                    // Request permission after explaining
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Handle if the user cancels the request
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, perform your action
                    Toast.makeText(this, "Continue to Enable the Service", Toast.LENGTH_SHORT).show();

                } else {
                    // Permission denied
                    Toast.makeText(this, "Permission denied, cannot post notification to keep app alive 😔", Toast.LENGTH_LONG).show();
                }
            });

}