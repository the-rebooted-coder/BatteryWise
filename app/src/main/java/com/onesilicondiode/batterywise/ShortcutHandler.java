package com.onesilicondiode.batterywise;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class ShortcutHandler extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, BatteryMonitorService.class);
        startService(intent);
        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
        finish();
    }
}