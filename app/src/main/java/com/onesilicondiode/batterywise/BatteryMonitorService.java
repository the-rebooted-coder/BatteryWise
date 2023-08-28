package com.onesilicondiode.batterywise;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class BatteryMonitorService extends Service {
    private MediaPlayer mediaPlayer;
    private BroadcastReceiver batteryReceiver;
    private boolean alertPlayed = false;
    private int previousVolume; // Store the previous volume level

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
                    // Increase volume to 80 before playing the alert tone
                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 70, 0);

                    // Play the alert tone only if it hasn't been played yet
                    mediaPlayer.start();

                    // Set an OnCompletionListener to revert the volume when audio has finished playing
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            // Revert the volume to the previous level
                            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);
                        }
                    });

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

        // Create an intent to open the MainActivity when the notification is tapped
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                mainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Create a notification channel (required for Android 8.0 and above)
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Battery Monitor",
                NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        // Create a notification for the foreground service with the PendingIntent
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafeCharge")
                .setContentText("Alert when battery goes >80%")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setColor(ContextCompat.getColor(this, R.color.green))
                .setColorized(true)
                .setContentIntent(pendingIntent) // Set the PendingIntent to open MainActivity
                .setAutoCancel(true) // Auto-cancel the notification when tapped
                .build();

        // Start the service as a foreground service
        startForeground(FOREGROUND_SERVICE_ID, notification);
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