package com.onesilicondiode.batterywise;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.IBinder;

import com.onesilicondiode.batterywise.R;

public class BatteryMonitorService extends Service {
    private MediaPlayer mediaPlayer;
    private BroadcastReceiver batteryReceiver;
    private boolean alertPlayed = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.notification);

        // Register BroadcastReceiver to monitor battery level changes
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                int batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

                if (batteryPercent > 80 && !alertPlayed) {
                    // Play the alert tone only if it hasn't been played yet
                    mediaPlayer.start();
                    alertPlayed = true;
                } else if (batteryPercent <= 80) {
                    // Reset the flag when battery drops below 80%
                    alertPlayed = false;
                }
            }
        };

        // Register the BroadcastReceiver with the intent filter
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Service should continue running until explicitly stopped
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Release MediaPlayer and unregister BroadcastReceiver
        mediaPlayer.release();
        unregisterReceiver(batteryReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return null since this service is not designed to be bound
        return null;
    }
}