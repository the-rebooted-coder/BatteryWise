package com.onesilicondiode.batterywise;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.splashscreen.SplashScreen;

import com.example.swipebutton_library.SwipeButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.textview.MaterialTextView;

import me.itangqi.waveloadingview.WaveLoadingView;

public class StopAlert extends AppCompatActivity {
    private static final int STOP_ACTION_NOTIFICATION_ID = 198;
    private static final String TAG = "StopAlert";
    private SwipeButton stopAlert;
    private MaterialTextView battPercentage, appName, belowAppName;
    private MediaPlayer mediaPlayer;
    private int previousVolume;
    private SharedPreferences sharedPreferences, sharedMyPrefs;
    private boolean shouldAutoStop;
    private Vibrator vibrator;
    private int currentValue;
    private WaveLoadingView stopLoadingView;
    private CountDownTimer countDownTimer;
    private BroadcastReceiver unplugReceiver;

    public static int getThemeColor(Context context, int colorResId) {
        TypedValue typedValue = new TypedValue();
        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, new int[]{colorResId});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Starting StopAlert activity");
        SplashScreen.installSplashScreen(this);
        DynamicColors.applyToActivityIfAvailable(this);
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        getWindow().setStatusBarColor(getThemeColor(this, android.R.attr.colorPrimaryDark));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        currentValue = sharedPreferences.getInt("counter", 0);
        sharedMyPrefs = getSharedPreferences("MyPrefsFile", MODE_PRIVATE);
        shouldAutoStop = sharedMyPrefs.getBoolean("switchState", true);
        int selectedTime = sharedMyPrefs.getInt("selected_time", 1);

        super.onCreate(savedInstanceState);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        setContentView(R.layout.activity_stop_alert);

        // Initialize views
        appName = findViewById(R.id.appNamed);
        belowAppName = findViewById(R.id.appNamedBelow);
        battPercentage = findViewById(R.id.appNamedBelow);
        stopAlert = findViewById(R.id.stopMusic);
        stopLoadingView = findViewById(R.id.stopLoadingView);

        // Initialize media player
        mediaPlayer = MediaPlayer.create(this, R.raw.notification);
        if (mediaPlayer == null) {
            Log.e(TAG, "onCreate: Failed to create MediaPlayer");
            finish();
            return;
        }

        // Update battery level display
        updateBatteryLevel();

        // Setup components
        setupMusic();
        setupButton();

        // Handle auto-stop
        if (shouldAutoStop && selectedTime != -1) {
            startAutoStopTimer(selectedTime);
        } else {
            appName.setTextColor(Color.parseColor("#FFFFFF"));
            belowAppName.setTextColor(Color.parseColor("#FFFFFF"));
            stopLoadingView.setVisibility(View.GONE);
        }

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Register receiver for unplug event (auto-dismiss if unplugged)
        unplugReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                boolean isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                        plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                        plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
                if (!isPlugged) {
                    Log.d(TAG, "Charger disconnected, auto-dismissing StopAlert");
                    stopAlertAndCleanup();
                }
            }
        };
        registerReceiver(unplugReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void updateBatteryLevel() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        if (battPercentage != null) {
            battPercentage.setText("Battery is at " + batLevel + "%\nUnplug the charger");
        } else {
            Log.e(TAG, "updateBatteryLevel: battPercentage TextView is null");
        }
    }

    private void setupButton() {
        if (stopAlert != null) {
            stopAlert.setOnActiveListener(() -> {
                Log.d(TAG, "SwipeButton activated");
                vibrate();
                stopAlertAndCleanup();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        } else {
            Log.e(TAG, "setupButton: stopAlert SwipeButton is null");
        }
    }

    private void setupMusic() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int seventyVolume = (int) (maxVolume * 0.8f);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, seventyVolume, 0);
        mediaPlayer.setLooping(true);
        try {
            mediaPlayer.start();
            Log.d(TAG, "setupMusic: MediaPlayer started");
        } catch (Exception e) {
            Log.e(TAG, "setupMusic: Error starting MediaPlayer", e);
        }
        mediaPlayer.setOnCompletionListener(mp -> {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);
            Log.d(TAG, "setupMusic: MediaPlayer completed, volume restored");
        });
    }

    private void startAutoStopTimer(int selectedTime) {
        stopLoadingView.setVisibility(View.VISIBLE);
        long duration;
        long textColorChangeDelay;
        switch (selectedTime) {
            case 1:
                duration = 60000; // 60 seconds
                textColorChangeDelay = 26000; // 26 seconds
                break;
            case 2:
                duration = 120000; // 120 seconds
                textColorChangeDelay = 56000; // 56 seconds
                break;
            case 3:
                duration = 180000; // 180 seconds
                textColorChangeDelay = 89600; // 89.6 seconds
                break;
            case 4:
                duration = 30000; // 30 seconds
                textColorChangeDelay = 13000; // 13 seconds
                break;
            default:
                duration = 60000; // Default 60 seconds
                textColorChangeDelay = 26000; // 26 seconds
                break;
        }

        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) (millisUntilFinished * 100 / duration);
                stopLoadingView.setProgressValue(progress);
                new Handler().postDelayed(() -> {
                    if (appName != null && belowAppName != null) {
                        appName.setTextColor(Color.parseColor("#FFFFFF"));
                        belowAppName.setTextColor(Color.parseColor("#FFFFFF"));
                    }
                }, textColorChangeDelay);
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Auto-stop timer finished");
                stopAlertAndCleanup();
            }
        }.start();
        Log.d(TAG, "startAutoStopTimer: Started timer for " + duration + "ms");
    }

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {5, 0, 5, 0, 5, 1, 5, 1, 5, 2, 5, 2, 5, 3};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
                vibrator.vibrate(vibrationEffect);
            } else {
                vibrator.vibrate(pattern, -1);
            }
            Log.d(TAG, "vibrate: Vibration triggered");
        } else {
            Log.w(TAG, "vibrate: Vibrator not available");
        }
    }

    private void stopAlertAndCleanup() {
        Log.d(TAG, "stopAlertAndCleanup: Cleaning up resources");
        NotificationManagerCompat.from(this).cancel(STOP_ACTION_NOTIFICATION_ID);
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("counter", currentValue + 1);
        editor.apply();
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            Log.d(TAG, "onWindowFocusChanged: Lost focus, stopping alert");
            stopAlertAndCleanup();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity destroyed");
        stopAlertAndCleanup();
        if (unplugReceiver != null) {
            unregisterReceiver(unplugReceiver);
            unplugReceiver = null;
        }
    }
}