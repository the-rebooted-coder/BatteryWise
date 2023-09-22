package com.onesilicondiode.batterywise;

import android.annotation.SuppressLint;
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
import android.os.Build;
import android.os.IBinder;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class BatteryMonitorService extends Service {
    private static final int FOREGROUND_SERVICE_ID = 101;
    private static final String NOTIF_CHANNEL_ID = "SafeCharge";
    private static final int STOP_ACTION_NOTIFICATION_ID = 103;
    private static final String STOP_ACTION_CHANNEL_ID = "StopAction";
    private static final String USER_STARTED_KEY = "userStarted";
    private static final boolean DEFAULT_USER_STARTED = true;
    SharedPreferences prefs;
    private MediaPlayer mediaPlayer;
    private BroadcastReceiver batteryReceiver;
    private boolean alertPlayed = false;
    private int previousVolume;
    private PendingIntent pendingIntent;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(getApplicationContext(), "Service Enabled", Toast.LENGTH_SHORT).show();
        mediaPlayer = MediaPlayer.create(this, R.raw.notification);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel stopActionChannel = new NotificationChannel(STOP_ACTION_CHANNEL_ID, "Stop Action", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(stopActionChannel);
        }
        BroadcastReceiver stopActionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Stop the media player when the stop action notification is tapped
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer = MediaPlayer.create(context, R.raw.notification);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.cancel(STOP_ACTION_NOTIFICATION_ID);
            }
        };
        IntentFilter stopActionIntentFilter = new IntentFilter("com.onesilicondiode.batterywise.STOP_ACTION");
        registerReceiver(stopActionReceiver, stopActionIntentFilter);
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                int batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                // Retrieve the selected battery level from SharedPreferences
                prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                int selectedBatteryLevel = prefs.getInt("selectedBatteryLevel", 85);
                boolean userStarted = prefs.getBoolean(USER_STARTED_KEY, DEFAULT_USER_STARTED);
                if (batteryPercent > selectedBatteryLevel && !alertPlayed) {
                    if (!userStarted) {
                        /*
                        //TODO Create Alarm
                        Toast.makeText(getApplicationContext(), "Restart", Toast.LENGTH_SHORT).show();
                        Intent setTimerIntent = new Intent(AlarmClock.ACTION_SET_TIMER);
                        setTimerIntent.putExtra(AlarmClock.EXTRA_LENGTH, 10);
                        setTimerIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "SafeCharge");
                        setTimerIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                        setTimerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(setTimerIntent);
                         */
                        Notification stopActionNotification = createStopActionNotification();
                        NotificationManager notificationManager = getSystemService(NotificationManager.class);
                        notificationManager.notify(STOP_ACTION_NOTIFICATION_ID, stopActionNotification);
                        Toast.makeText(context, "Battery Levels More Than " + selectedBatteryLevel + "%", Toast.LENGTH_SHORT).show();
                        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        float percent = 0.8f;
                        int seventyVolume = (int) (maxVolume * percent);
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, seventyVolume, 0);
                        mediaPlayer.start();
                        mediaPlayer.setLooping(true);
                        mediaPlayer.setOnCompletionListener(mp -> {
                            // Revert the volume to the previous level
                            AudioManager audioManager1 = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                            audioManager1.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);
                        });
                    }
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(USER_STARTED_KEY, false);
                    editor.apply();
                    alertPlayed = true;
                } else if (batteryPercent <= selectedBatteryLevel) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(USER_STARTED_KEY, false);
                    editor.apply();
                    alertPlayed = false;
                }
            }
        };

        // Register the BroadcastReceiver with the intent filter
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, intentFilter);

        Intent mainActivityIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mainActivityIntent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, this.getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, NOTIF_CHANNEL_ID);
        } else {
            mainActivityIntent = new Intent(this, MainActivity.class);
        }
        pendingIntent = PendingIntent.getActivity(this, 0, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create a notification channel (required for Android 8.0 and above)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID, "SafeCharge", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.subtle), null); // Set custom sound

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            // Create a notification for the foreground service with the PendingIntent
            Notification notification = createNotification();

            // Start the service as a foreground service
            startForeground(FOREGROUND_SERVICE_ID, notification);
        } else {
            Notification notification = createNotification();
            startForeground(FOREGROUND_SERVICE_ID, notification);
        }
    }

    // Create a custom notification
    private Notification createNotification() {
        NotificationCompat.Builder builder;

        // Check Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android Oreo (API 26) and above
            builder = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                    .setContentText("Optimising Battery Use")
                    .setContentText("You may tap on this notification to hide it")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
        } else {
            Uri notifSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.subtle);
            // For Android versions prior to Oreo
            builder = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                    .setContentText("Optimising Battery Use")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setSound(notifSound)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
        }

        // Set custom sound for the notification
        Uri customSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.subtle);
        builder.setSound(customSoundUri);

        return builder.build();
    }

    private Notification createStopActionNotification() {
        Intent stopIntent = new Intent("com.onesilicondiode.batterywise.STOP_ACTION");
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // Create a notification action for the stop button
        NotificationCompat.Action stopAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.baseline_stop_24,
                        "Stop Playing",
                        stopPendingIntent
                ).build();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, STOP_ACTION_CHANNEL_ID)
                .setContentTitle("Alert is Playing...")
                .setContentText("Disconnect the charger")
                .setSmallIcon(R.drawable.ringing)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .addAction(stopAction);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        unregisterReceiver(batteryReceiver);
        Toast.makeText(getApplicationContext(), "Service Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return null since this service is not designed to be bound
        return null;
    }
}