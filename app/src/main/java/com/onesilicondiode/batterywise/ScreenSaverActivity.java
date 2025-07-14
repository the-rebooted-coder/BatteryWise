package com.onesilicondiode.batterywise;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class ScreenSaverActivity extends AppCompatActivity {

    private TextView clockHourView, clockColonView, clockMinuteView, safeChargeLabel, clockDateView;
    private LinearLayout clockContainer;
    private static final int COLOR_DEFAULT = 0xFF818589;
    private static final int COLOR_ZEN = 0xFF2196F3;
    private Handler handler = new Handler();
    private final SimpleDateFormat hourFormat = new SimpleDateFormat("HH", Locale.getDefault());
    private final SimpleDateFormat minuteFormat = new SimpleDateFormat("mm", Locale.getDefault());
    private final Random random = new Random();

    private final int MOVE_INTERVAL_MS = 120000; // 2 mins for clock
    private final int SAFECHARGE_MOVE_INTERVAL_MS = 120000; // 2 mins for SafeCharge
    private final int FADE_OUT_DURATION = 1200; // ms
    private final int FADE_IN_DURATION = 1500; // ms
    private final int SAFECHARGE_FADE_OUT = 700; // ms
    private final int SAFECHARGE_FADE_IN = 900;  // ms

    private FrameLayout rootLayout;
    private int safeChargePrevOffset = 0;
    private final int SAFECHARGE_OFFSET_PX = 10; // max +/-10px movement
    private LinearLayout safeChargeLabelContainer;

    private boolean zenModeEnabled;
    private TextView batteryPercentLabel;

    private final Runnable updateClockRunnable = new Runnable() {
        @Override
        public void run() {
            if (clockHourView != null && clockMinuteView != null && clockColonView != null) {
                Date now = new Date();
                String hour = hourFormat.format(now);
                String minute = minuteFormat.format(now);
                clockHourView.setText(hour);
                clockMinuteView.setText(minute);
                clockColonView.setText(":");

                // Day and date
                if (clockDateView != null) {
                    int dayOfMonth = Integer.parseInt(new SimpleDateFormat("d", Locale.getDefault()).format(now));
                    String suffix = getDaySuffix(dayOfMonth);
                    String formattedDate = new SimpleDateFormat("EEEE, d'" + suffix + "' MMMM", Locale.getDefault()).format(now);
                    clockDateView.setText(formattedDate);
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable moveClockRunnable = new Runnable() {
        @Override
        public void run() {
            animateClockMove();
            handler.postDelayed(this, MOVE_INTERVAL_MS);
        }
    };

    private final Runnable moveSafeChargeRunnable = new Runnable() {
        @Override
        public void run() {
            animateSafeChargeMove();
            handler.postDelayed(this, SAFECHARGE_MOVE_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dream_clock);

        // Optional: Hide status/navigation bars for immersive experience
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        rootLayout = findViewById(R.id.dreamRoot);
        batteryPercentLabel = findViewById(R.id.batteryPercentLabel);
        clockContainer = findViewById(R.id.clockContainer);
        clockHourView = findViewById(R.id.clockHourView);
        clockColonView = findViewById(R.id.clockColonView);
        clockMinuteView = findViewById(R.id.clockMinuteView);
        clockDateView = findViewById(R.id.clockDateView);
        safeChargeLabelContainer = findViewById(R.id.safeChargeLabelContainer);


        int batteryPercent = getBatteryPercentage();
        batteryPercentLabel.setText(batteryPercent + "%");

        // Read Zen Mode from SafeCharge Settings
        zenModeEnabled = getSharedPreferences(
                SafeChargeSettingsActivity.PREFS_NAME,
                Context.MODE_PRIVATE
        ).getBoolean(SafeChargeSettingsActivity.ZEN_MODE_KEY, false);

        int color = zenModeEnabled ? COLOR_ZEN : COLOR_DEFAULT;
        clockHourView.setTextColor(color);
        clockColonView.setTextColor(color);
        clockMinuteView.setTextColor(color);
        if (clockDateView != null) {
            clockDateView.setTextColor(color);
        }

        updateClockRunnable.run();
        moveClockRunnable.run();
        moveSafeChargeRunnable.run();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateClockRunnable);
        handler.removeCallbacks(moveClockRunnable);
        handler.removeCallbacks(moveSafeChargeRunnable);
    }

    private String getDaySuffix(int day) {
        if (day >= 11 && day <= 13) return "th";
        switch (day % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    private void animateClockMove() {
        if (rootLayout == null || clockContainer == null) return;

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(clockContainer, "alpha", 1f, 0f);
        fadeOut.setDuration(FADE_OUT_DURATION);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                moveClock();
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(clockContainer, "alpha", 0f, 1f);
                fadeIn.setDuration(FADE_IN_DURATION);
                fadeIn.start();
            }
        });
        fadeOut.start();
    }

    private void moveClock() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Point size = new Point();
        if (wm != null) {
            wm.getDefaultDisplay().getSize(size);
        } else {
            size.x = rootLayout.getWidth();
            size.y = rootLayout.getHeight();
        }

        int clockWidth = clockContainer.getWidth();
        int clockHeight = clockContainer.getHeight();

        int maxX = Math.max(0, size.x - clockWidth - 32);
        int maxY = Math.max(0, size.y - clockHeight - 160); // avoid bottom SafeCharge label
        int minY = 32; // avoid top bar

        int x = random.nextInt(Math.max(1, maxX));
        int y = minY + random.nextInt(Math.max(1, maxY - minY));

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) clockContainer.getLayoutParams();
        params.leftMargin = x;
        params.topMargin = y;
        params.gravity = 0; // disable gravity so margins work
        clockContainer.setLayoutParams(params);
    }

    private void animateSafeChargeMove() {
        if (safeChargeLabelContainer == null || rootLayout == null) return;

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(safeChargeLabelContainer, "alpha", 0.9f, 0f);
        fadeOut.setDuration(SAFECHARGE_FADE_OUT);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) safeChargeLabelContainer.getLayoutParams();
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                params.leftMargin = safeChargePrevOffset;
                safeChargeLabelContainer.setLayoutParams(params);

                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(safeChargeLabelContainer, "alpha", 0f, 0.9f);
                fadeIn.setDuration(SAFECHARGE_FADE_IN);
                fadeIn.start();
            }
        });
        fadeOut.start();
    }

    private int getBatteryPercentage() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        if (bm != null) {
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return -1; // fallback
    }
}