package com.onesilicondiode.batterywise;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if the device has finished booting
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, BatteryMonitorService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Starting foreground service is available starting from Android Oreo (API level 26)
                context.startForegroundService(serviceIntent);
            } else {
                // For Android Nougat (API level 24) and lower, use startService
                context.startService(serviceIntent);
            }
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, BatteryMonitorService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Starting foreground service is available starting from Android Oreo (API level 26)
                context.startForegroundService(serviceIntent);
            } else {
                // For Android Nougat (API level 24) and lower, use startService
                context.startService(serviceIntent);
            }
        }
    }
}