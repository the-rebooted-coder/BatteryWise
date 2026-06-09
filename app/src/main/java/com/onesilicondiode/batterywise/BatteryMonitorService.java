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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.onesilicondiode.batterywise.db.BatterySnapshot;
import com.onesilicondiode.batterywise.db.ChargeSession;
import com.onesilicondiode.batterywise.db.ChronoDatabase;
import com.onesilicondiode.batterywise.db.ChronoDao;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ComponentName;
import android.service.quicksettings.TileService;

public class BatteryMonitorService extends Service {
    private static final int FOREGROUND_SERVICE_ID = 101;
    private static final String NOTIF_CHANNEL_ID = "SafeCharge";
    private static final int STOP_ACTION_NOTIFICATION_ID = 198;
    private static final String STOP_ACTION_CHANNEL_ID = "StopAlert";
    private static final String USER_STARTED_KEY = "userStarted";
    private static final String ALERT_PLAYED_KEY = "alertPlayed";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    /**
     * Static flag for MainActivity to check service state without using the
     * deprecated ActivityManager.getRunningServices() API (unreliable on API 26+).
     */
    public static boolean isRunning = false;

    private static final boolean DEFAULT_USER_STARTED = false;
    private static final String TAG = "BatteryMonitorService";
    private SharedPreferences prefs;
    private BroadcastReceiver batteryReceiver;
    private PendingIntent pendingIntent;

    // ── ChronoCell data logging ──
    private ChronoDao chronoDao;
    private ExecutorService dbExecutor;
    private Handler snapshotHandler;
    private static final long SNAPSHOT_INTERVAL_MS = 30 * 60 * 1000; // 30 minutes
    private static final long DATA_RETENTION_MS = 180L * 24 * 60 * 60 * 1000; // 6 months

    // Charge session tracking
    private boolean wasCharging = false;
    private long sessionStartMs = 0;
    private int sessionStartPercent = 0;
    private int sessionChargerType = 0;
    private final List<Float> sessionTemps = new ArrayList<>();
    private final List<Float> sessionWattages = new ArrayList<>();
    private boolean sessionSafeChargeTriggered = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: SafeCharge Service started");

        // Fix 1: Service killed by system - register receiver in onStartCommand and re-register if needed

        // Fix 2: App updated - make service STICKY so it restarts after update
        // This is handled in onStartCommand below

        // Mark service as running (for MainActivity state check and BootReceiver guard)
        isRunning = true;

        // Notify QS tile to update its state
        try {
            TileService.requestListeningState(this,
                    new ComponentName(this, SafeChargeTileService.class));
        } catch (Exception ignored) {}

        // Initialize SharedPreferences
        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(USER_STARTED_KEY, false);
        editor.putBoolean(ALERT_PLAYED_KEY, false);
        editor.putBoolean("serviceRunning", true); // Persisted for BootReceiver on reboot/update
        editor.apply();

        // ── ChronoCell: Initialize database and snapshot scheduler ──
        chronoDao = ChronoDatabase.getInstance(this).chronoDao();
        dbExecutor = Executors.newSingleThreadExecutor();
        snapshotHandler = new Handler(Looper.getMainLooper());

        // Prune old data (older than 6 months)
        dbExecutor.execute(() -> {
            long cutoff = System.currentTimeMillis() - DATA_RETENTION_MS;
            chronoDao.deleteOldSessions(cutoff);
            chronoDao.deleteOldSnapshots(cutoff);
            Log.d(TAG, "ChronoCell: Pruned data older than 6 months");
        });

        // Start periodic snapshots
        snapshotHandler.post(snapshotRunnable);

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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Fix 1: Service killed by system - re-register receiver if null
        if (batteryReceiver == null) {
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

                    // ── ChronoCell: Track charge session lifecycle ──
                    trackChargeSession(intent, batteryManager, batteryPercent, isPlugged, plugged);

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

                        // ── ChronoCell: Mark SafeCharge triggered for this session ──
                        sessionSafeChargeTriggered = true;

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(ALERT_PLAYED_KEY, true);
                        editor.putBoolean(USER_STARTED_KEY, false);
                        editor.apply();
                    } else if (!isPlugged) {
                        Log.d(TAG, "onReceive: Charger disconnected, resetting alertPlayed");
                        NotificationManagerCompat.from(context).cancel(STOP_ACTION_NOTIFICATION_ID); // Ensure notification is cancelled
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
        }

        // Fix 2: App update or service killed - restart service automatically
        return START_STICKY;
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
        // Mark service as stopped
        isRunning = false;

        // Notify QS tile to update its state
        try {
            TileService.requestListeningState(this,
                    new ComponentName(this, SafeChargeTileService.class));
        } catch (Exception ignored) {}
        if (prefs != null) {
            prefs.edit().putBoolean("serviceRunning", false).apply();
        }
        if (batteryReceiver != null) {
            try {
                unregisterReceiver(batteryReceiver);
            } catch (IllegalArgumentException ignored) {
                // Receiver was already unregistered
            }
            batteryReceiver = null;
            Log.d(TAG, "onDestroy: Unregistered batteryReceiver");
        }

        // ── ChronoCell: Finalize any in-progress charge session ──
        if (wasCharging) {
            finalizeChargeSession(((BatteryManager) getSystemService(BATTERY_SERVICE))
                    .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
        }
        if (snapshotHandler != null) {
            snapshotHandler.removeCallbacks(snapshotRunnable);
        }
        if (dbExecutor != null) {
            dbExecutor.shutdown();
        }

        stopForeground(true);
        Log.d(TAG, "onDestroy: Service stopped");
    }

    // ══════════════════════════════════════════════════════════════
    // ChronoCell: Charge session tracking & periodic snapshots
    // ══════════════════════════════════════════════════════════════

    private final Runnable snapshotRunnable = new Runnable() {
        @Override
        public void run() {
            logBatterySnapshot();
            snapshotHandler.postDelayed(this, SNAPSHOT_INTERVAL_MS);
        }
    };

    private void logBatterySnapshot() {
        if (dbExecutor == null || dbExecutor.isShutdown()) return;
        dbExecutor.execute(() -> {
            try {
                BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
                if (bm == null) return;

                Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

                BatterySnapshot snapshot = new BatterySnapshot();
                snapshot.timestampMs = System.currentTimeMillis();
                snapshot.batteryPercent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

                if (batteryIntent != null) {
                    int temp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                    snapshot.temperature = (temp != -1) ? temp / 10f : 0f;

                    int voltageMv = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                    snapshot.voltage = (voltageMv != -1) ? voltageMv / 1000f : 0f;

                    int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                    snapshot.isCharging = plugged != 0;

                    int health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                    snapshot.healthStatus = health;
                }

                chronoDao.insertSnapshot(snapshot);
                Log.d(TAG, "ChronoCell: Snapshot logged - " + snapshot.batteryPercent + "%, "
                        + snapshot.temperature + "°C");
            } catch (Exception e) {
                Log.e(TAG, "ChronoCell: Failed to log snapshot", e);
            }
        });
    }

    private void trackChargeSession(Intent intent, BatteryManager bm,
                                     int batteryPercent, boolean isPlugged, int pluggedType) {
        if (isPlugged && !wasCharging) {
            // ── Charger just connected → start a new session ──
            wasCharging = true;
            sessionStartMs = System.currentTimeMillis();
            sessionStartPercent = batteryPercent;
            sessionChargerType = pluggedType;
            sessionSafeChargeTriggered = false;
            sessionTemps.clear();
            sessionWattages.clear();
            Log.d(TAG, "ChronoCell: Charge session started at " + batteryPercent + "%");

            // Log an immediate snapshot at session start
            logBatterySnapshot();
        }

        if (isPlugged && wasCharging) {
            // ── Accumulate temperature & wattage readings during the session ──
            if (intent != null) {
                int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                if (temp > 0) sessionTemps.add(temp / 10f);

                try {
                    long currentNow = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                    int voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                    if (voltageMv > 0) {
                        double wattage = Math.abs((voltageMv / 1000.0) * (currentNow / 1000000.0));
                        sessionWattages.add((float) wattage);
                    }
                } catch (Exception ignored) {}
            }
        }

        if (!isPlugged && wasCharging) {
            // ── Charger disconnected → finalize the session ──
            finalizeChargeSession(batteryPercent);
        }
    }

    private void finalizeChargeSession(int endPercent) {
        wasCharging = false;
        if (dbExecutor == null || dbExecutor.isShutdown()) return;

        ChargeSession session = new ChargeSession();
        session.startTimeMs = sessionStartMs;
        session.endTimeMs = System.currentTimeMillis();
        session.startPercent = sessionStartPercent;
        session.endPercent = endPercent;
        session.chargerType = sessionChargerType;
        session.safeChargeTriggered = sessionSafeChargeTriggered;

        // Calculate averages
        if (!sessionTemps.isEmpty()) {
            float sum = 0, max = 0;
            for (float t : sessionTemps) {
                sum += t;
                if (t > max) max = t;
            }
            session.avgTemperature = sum / sessionTemps.size();
            session.maxTemperature = max;
        }
        if (!sessionWattages.isEmpty()) {
            float sum = 0;
            for (float w : sessionWattages) sum += w;
            session.avgWattage = sum / sessionWattages.size();
        }

        dbExecutor.execute(() -> {
            try {
                chronoDao.insertSession(session);
                Log.d(TAG, "ChronoCell: Session saved - " + session.startPercent
                        + "% → " + session.endPercent + "%, "
                        + String.format("%.1f", session.avgTemperature) + "°C avg");
            } catch (Exception e) {
                Log.e(TAG, "ChronoCell: Failed to save session", e);
            }
        });

        // Log a snapshot at session end too
        logBatterySnapshot();
    }
}