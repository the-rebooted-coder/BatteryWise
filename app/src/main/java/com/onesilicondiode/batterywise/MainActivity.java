package com.onesilicondiode.batterywise;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {
    MaterialButton toggleService;
    private static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize the MediaPlayer
        toggleService = findViewById(R.id.saveBatteryBtn);
        // Check if the service is running and set button text accordingly
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isServiceRunning = prefs.getBoolean("isServiceRunning", false);

        if (isServiceRunning) {
            toggleService.setText(R.string.enable);
        } else {
            toggleService.setText(R.string.disable);
        }

        toggleService.setOnClickListener(view -> {
            if (isServiceRunning) {
                // Stop the service
                Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
                stopService(serviceIntent);
                toggleService.setText(R.string.enable);
                Toast.makeText(MainActivity.this, "Service Stopped", Toast.LENGTH_SHORT).show();
            } else {
                // Start the service
                Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
                startService(serviceIntent);
                toggleService.setText(R.string.disable);
                Toast.makeText(MainActivity.this, "Service Enabled", Toast.LENGTH_SHORT).show();
            }

            // Toggle the service running state in shared preferences
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean("isServiceRunning", !isServiceRunning);
            editor.apply();
        });
    }
}