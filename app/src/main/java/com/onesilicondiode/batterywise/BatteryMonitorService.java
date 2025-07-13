package com.onesilicondiode.batterywise;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class BatteryMonitorService extends Service {
    private static final int FOREGROUND_SERVICE_ID = 101;
    private static final String NOTIF_CHANNEL_ID = "SafeCharge";
    private static final int STOP_ACTION_NOTIFICATION_ID = 198;
    private static final String STOP_ACTION_CHANNEL_ID = "StopAlert";
    private static final String USER_STARTED_KEY = "userStarted";
    private static final String ALERT_PLAYED_KEY = "alertPlayed";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    private static final boolean DEFAULT_USER_STARTED = false;
    private static final String TAG = "BatteryMonitorService";
    private SharedPreferences prefs;
    private BroadcastReceiver batteryReceiver;
    private PendingIntent pendingIntent;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: SafeCharge Service started");

        // Initialize SharedPreferences
        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(USER_STARTED_KEY, false);
        editor.putBoolean(ALERT_PLAYED_KEY, false);
        editor.apply();

        // Create notification channel for StopAlert
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel stopActionChannel = new NotificationChannel(
                    STOP_ACTION_CHANNEL_ID, "Stop Alerts", NotificationManager.IMPORTANCE_HIGH);
            stopActionChannel.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.silence), null);
            stopActionChannel.setBypassDnd(true);
            stopActionChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(stopActionChannel);
        }

        // Register BroadcastReceiver for battery changes
        batteryReceiver = new BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            @Override
            public void onReceive(Context context, Intent intent) {
                BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                int batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                boolean isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                        plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                        plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;

                int selectedBatteryLevel = prefs.getInt("selectedBatteryLevel", 85);
                boolean userStarted = prefs.getBoolean(USER_STARTED_KEY, DEFAULT_USER_STARTED);
                boolean alertPlayed = prefs.getBoolean(ALERT_PLAYED_KEY, false);

                Log.d(TAG, "onReceive: batteryPercent=" + batteryPercent + ", isPlugged=" + isPlugged +
                        ", selectedBatteryLevel=" + selectedBatteryLevel + ", userStarted=" + userStarted +
                        ", alertPlayed=" + alertPlayed);

                if (isPlugged && batteryPercent >= selectedBatteryLevel && !alertPlayed && !userStarted) {
                    // Check notification permission first
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                    != PackageManager.PERMISSION_GRANTED) {

                        Log.w(TAG, "Missing notification permission");
                        return;
                    }
                    Intent fullScreenIntent = new Intent(context, StopAlert.class);
                    fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                            context,
                            STOP_ACTION_NOTIFICATION_ID,
                            fullScreenIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, STOP_ACTION_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle("Battery Charged")
                            .setContentText("Disconnect the charger")
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setCategory(NotificationCompat.CATEGORY_ALARM)
                            .setAutoCancel(true)
                            .setFullScreenIntent(fullScreenPendingIntent, true)
                            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                    Log.d(TAG, "onReceive: Posting full-screen notification");
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    try {
                        notificationManager.notify(STOP_ACTION_NOTIFICATION_ID, builder.build());
                        Log.d(TAG, "Notification posted successfully");
                    } catch (SecurityException e) {
                        Log.e(TAG, "Failed to post notification", e);
                    }

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(ALERT_PLAYED_KEY, true);
                    editor.putBoolean(USER_STARTED_KEY, false);
                    editor.apply();
                } else if (!isPlugged) {
                    Log.d(TAG, "onReceive: Charger disconnected, resetting alertPlayed");
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(ALERT_PLAYED_KEY, false);
                    editor.putBoolean(USER_STARTED_KEY, false);
                    editor.apply();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, intentFilter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(batteryReceiver, intentFilter);
        }

        // Setup foreground service notification
        Intent mainActivityIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mainActivityIntent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, NOTIF_CHANNEL_ID);
        } else {
            mainActivityIntent = new Intent(this, MainActivity.class);
        }
        pendingIntent = PendingIntent.getActivity(this, 0, mainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIF_CHANNEL_ID, "SafeCharge", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.subtle), null);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = createNotification();
        startForeground(FOREGROUND_SERVICE_ID, notification);
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setContentTitle("Monitoring Charge Levels")
                .setContentText("You may tap on this notification and then disable it")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        Uri customSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.subtle);
        builder.setSound(customSoundUri);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
            batteryReceiver = null;
            Log.d(TAG, "onDestroy: Unregistered batteryReceiver");
        }
        stopForeground(true);
        Log.d(TAG, "onDestroy: Service stopped");
    }
}