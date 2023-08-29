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
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
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
    private PendingIntent pendingIntent; // Declare pendingIntent as a member variable

    private static final int FOREGROUND_SERVICE_ID = 101;
    private static final String NOTIF_CHANNEL_ID = "SafeCharge";


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
                // Retrieve the selected battery level from SharedPreferences
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                int selectedBatteryLevel = prefs.getInt("selectedBatteryLevel", 85);

                if (batteryPercent > selectedBatteryLevel && !alertPlayed) {
                    Toast.makeText(context, "Battery Levels More Than "+selectedBatteryLevel+"%", Toast.LENGTH_SHORT).show();
                    // Increase volume to 85 before playing the alert tone
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
        pendingIntent = PendingIntent.getActivity(
                this,
                0,
                mainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create a notification channel (required for Android 8.0 and above)
        NotificationChannel channel = new NotificationChannel(
                NOTIF_CHANNEL_ID,
                "SafeCharge",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.subtle), null); // Set custom sound

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        // Create a notification for the foreground service with the PendingIntent
        Notification notification = createNotification();

        // Start the service as a foreground service
        startForeground(FOREGROUND_SERVICE_ID, notification);
    }

    // Create a custom notification
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setContentTitle("Monitoring Charge Levels")
                .setContentText("Alerting when battery goes >85%")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setColor(ContextCompat.getColor(this, R.color.green))
                .setColorized(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Set custom sound for the notification
        Uri customSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.subtle);
        builder.setSound(customSoundUri);
        return builder.build();
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