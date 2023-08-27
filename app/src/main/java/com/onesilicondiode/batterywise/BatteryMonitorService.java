package com.onesilicondiode.batterywise;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.onesilicondiode.batterywise.R;

public class BatteryMonitorService extends Service {
    private MediaPlayer mediaPlayer;
    private BroadcastReceiver batteryReceiver;
    private boolean alertPlayed = false;

    private static final int FOREGROUND_SERVICE_ID = 101;
    private static final String CHANNEL_ID = "BatteryMonitorChannel";

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

        // Create a notification channel (required for Android 8.0 and above)
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Battery Monitor",
                NotificationManager.IMPORTANCE_HIGH);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        // Create a notification for the foreground service
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafeCharge")
                .setContentText("Battery levels are being monitored.")
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(this, R.color.green))
                .setColorized(true)
                .build();

        // Start the service as a foreground service
        startForeground(FOREGROUND_SERVICE_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Service should continue running and restart if killed
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