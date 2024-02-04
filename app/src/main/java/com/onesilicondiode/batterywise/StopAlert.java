package com.onesilicondiode.batterywise;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.splashscreen.SplashScreen;

import com.example.swipebutton_library.OnActiveListener;
import com.example.swipebutton_library.SwipeButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.textview.MaterialTextView;

public class StopAlert extends AppCompatActivity {
    private static final int STOP_ACTION_NOTIFICATION_ID = 198;
    SwipeButton stopAlert;
    MaterialTextView battPercentage;
    private MediaPlayer mediaPlayer;
    private int previousVolume;
    private SharedPreferences sharedPreferences, sharedMyPrefs;
    private boolean shouldAutoStop;
    private Vibrator vibrator;
    int currentValue;

    public static int getThemeColor(Context context, int colorResId) {
        TypedValue typedValue = new TypedValue();
        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, new int[]{colorResId});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        sharedMyPrefs = getSharedPreferences("MyPrefsFile",MODE_PRIVATE);
        shouldAutoStop = sharedMyPrefs.getBoolean("switchState", true);
        int selectedTime = sharedMyPrefs.getInt("selected_time", -1);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_alert);
        mediaPlayer = MediaPlayer.create(this, R.raw.notification);
        setupMusic();
        setupButton();
        if (shouldAutoStop) {
            if (selectedTime != -1) {
                switch (selectedTime) {
                    case 1:
                        // Start a handler to stop the media player after 60 seconds
                        new Handler().postDelayed(() -> {
                            NotificationManagerCompat.from(this).cancel(STOP_ACTION_NOTIFICATION_ID);
                            if (mediaPlayer.isPlaying()) {
                                mediaPlayer.stop();
                            }
                            mediaPlayer.reset();
                            mediaPlayer = MediaPlayer.create(StopAlert.this, R.raw.notification);
                            int newValue = currentValue + 1;
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt("counter", newValue);
                            editor.apply();
                            finish();
                        }, 60000); // Stop after 60 seconds (60000 milliseconds)
                        break;
                    case 2:
                        new Handler().postDelayed(() -> {
                            NotificationManagerCompat.from(this).cancel(STOP_ACTION_NOTIFICATION_ID);
                            if (mediaPlayer.isPlaying()) {
                                mediaPlayer.stop();
                            }
                            mediaPlayer.reset();
                            mediaPlayer = MediaPlayer.create(StopAlert.this, R.raw.notification);
                            int newValue = currentValue + 1;
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt("counter", newValue);
                            editor.apply();
                            finish();
                        }, 120000); // Stop after 120 seconds (60000 milliseconds)
                        break;
                    case 3:
                        new Handler().postDelayed(() -> {
                            NotificationManagerCompat.from(this).cancel(STOP_ACTION_NOTIFICATION_ID);
                            if (mediaPlayer.isPlaying()) {
                                mediaPlayer.stop();
                            }
                            mediaPlayer.reset();
                            mediaPlayer = MediaPlayer.create(StopAlert.this, R.raw.notification);
                            int newValue = currentValue + 1;
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt("counter", newValue);
                            editor.apply();
                            finish();
                        }, 180000); // Stop after 180 seconds (60000 milliseconds)
                        break;
                    default:
                        new Handler().postDelayed(() -> {
                            NotificationManagerCompat.from(this).cancel(STOP_ACTION_NOTIFICATION_ID);
                            if (mediaPlayer.isPlaying()) {
                                mediaPlayer.stop();
                            }
                            mediaPlayer.reset();
                            mediaPlayer = MediaPlayer.create(StopAlert.this, R.raw.notification);
                            int newValue = currentValue + 1;
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt("counter", newValue);
                            editor.apply();
                            finish();
                        }, 60000); // Stop after 60 seconds (60000 milliseconds)
                        break;
                }
            }
        }
        BatteryManager bm = (BatteryManager) this.getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        battPercentage.setText("Battery is at " + batLevel + "%\nUnplug the charger");
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    private void setupButton() {
        battPercentage = findViewById(R.id.appNamedBelow);
        stopAlert = findViewById(R.id.stopMusic);
        stopAlert.setOnActiveListener(() -> {
            NotificationManagerCompat.from(getApplicationContext()).cancel(STOP_ACTION_NOTIFICATION_ID);
            vibrate();
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer = MediaPlayer.create(StopAlert.this, R.raw.notification);
            int newValue = currentValue + 1;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("counter", newValue);
            editor.apply();
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            NotificationManagerCompat.from(this).cancel(STOP_ACTION_NOTIFICATION_ID);
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer = MediaPlayer.create(StopAlert.this, R.raw.notification);
            int newValue = currentValue + 1;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("counter", newValue);
            editor.apply();
            finish();
        }
    }

    private void setupMusic() {
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

    private void vibrate() {
        long[] pattern = {5, 0, 5, 0, 5, 1, 5, 1, 5, 2, 5, 2, 5, 3};
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(vibrationEffect);
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationManagerCompat.from(this).cancel(STOP_ACTION_NOTIFICATION_ID);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        mediaPlayer = MediaPlayer.create(StopAlert.this, R.raw.notification);
        int newValue = currentValue + 1;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("counter", newValue);
        editor.apply();
    }
}