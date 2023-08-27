package com.onesilicondiode.batterywise;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {
    MaterialButton startSaving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the MediaPlayer
        startSaving = findViewById(R.id.saveBatteryBtn);
        startSaving.setOnClickListener(view -> {
            Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
            startService(serviceIntent);
            startSaving.setText(R.string.enabled);
            startSaving.setEnabled(false);
            Toast.makeText(MainActivity.this, "Service Enabled", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    public void stopMonitoring(View view) {
        Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
        stopService(serviceIntent);
    }
}