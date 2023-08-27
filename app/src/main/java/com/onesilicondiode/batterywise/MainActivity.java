package com.onesilicondiode.batterywise;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {
    MaterialButton startSaving, stopSaving;
    private static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                finish();
            }
        });
    }
}