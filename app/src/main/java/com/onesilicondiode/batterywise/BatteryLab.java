package com.onesilicondiode.batterywise;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BatteryLab extends AppCompatActivity {

    private static final String TAG = "BatteryLab";
    private TextView txtWattage, txtTemp, txtHealth, txtVoltage, txtSourceType, txtChargeStatus, txtSafeCharged;
    private TextView txtPowerDirection;
    private View cardPowerFlow, cardTemp, cardHealth, cardVoltage, cardSource, cardSafeCharged;
    private View livePulseDot;
    private View gridRow1, gridRow2;

    private BatteryManager batteryManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<View> animatableCards = new ArrayList<>();
    private boolean hasAnimated = false;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateLiveMetrics();
            handler.postDelayed(this, 2000);
        }
    };

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateStickyMetrics(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_lab);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);

        initViews();
        setupTooltips();
        startLivePulse();

        // Back navigation
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            HapticUtils.vibrateTouch(this);
            getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void initViews() {
        txtWattage = findViewById(R.id.txt_wattage);
        txtTemp = findViewById(R.id.txt_temp);
        txtHealth = findViewById(R.id.txt_health);
        txtVoltage = findViewById(R.id.txt_voltage);
        txtSourceType = findViewById(R.id.txt_source_type);
        txtChargeStatus = findViewById(R.id.txt_charge_status);
        txtSafeCharged = findViewById(R.id.txt_safecharged);
        txtPowerDirection = findViewById(R.id.txt_power_direction);

        cardPowerFlow = findViewById(R.id.card_power_flow);
        cardTemp = findViewById(R.id.card_temp);
        cardHealth = findViewById(R.id.card_health);
        cardVoltage = findViewById(R.id.card_voltage);
        cardSource = findViewById(R.id.card_source);
        cardSafeCharged = findViewById(R.id.card_safecharged);
        livePulseDot = findViewById(R.id.live_pulse_dot);
        gridRow1 = findViewById(R.id.grid_row_1);
        gridRow2 = findViewById(R.id.grid_row_2);
    }

    private void setupTooltips() {
        findViewById(R.id.info_power_flow).setOnClickListener(v -> {
            HapticUtils.vibrateTouch(this);
            showTooltip("Power Flow", "The real-time energy entering or leaving your battery. Measured in Watts.");
        });
        findViewById(R.id.info_temp).setOnClickListener(v -> {
            HapticUtils.vibrateTouch(this);
            showTooltip("Temperature", "Internal heat level. Batteries hate heat; staying below 40°C is ideal for longevity.");
        });
        findViewById(R.id.info_health).setOnClickListener(v -> {
            HapticUtils.vibrateTouch(this);
            showTooltip("Battery Health", "Hardware-level health assessment reported by the system.");
        });
        findViewById(R.id.info_voltage).setOnClickListener(v -> {
            HapticUtils.vibrateTouch(this);
            showTooltip("Voltage", "The electrical pressure provided by the battery. Stable voltage indicates a healthy power system.");
        });
        findViewById(R.id.info_safecharged).setOnClickListener(v -> {
            HapticUtils.vibrateTouch(this);
            showTooltip("SafeCharged", "The total number of times SafeCharge successfully monitored your battery and prevented potential overcharging.");
        });
    }

    private void showTooltip(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got it", null)
                .show();
    }

    /**
     * Pulsing "LIVE" dot animation — infinite scale + alpha cycle.
     */
    private void startLivePulse() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(livePulseDot, "scaleX", 1f, 1.6f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(livePulseDot, "scaleY", 1f, 1.6f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(livePulseDot, "alpha", 1f, 0.4f, 1f);

        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(scaleX, scaleY, alpha);
        pulseSet.setDuration(1500);
        pulseSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                pulseSet.start(); // loop forever
            }
        });
        pulseSet.start();
    }

    /**
     * Staggered bouncy entry animation for all visible cards.
     */
    private void runStaggeredEntryAnimation() {
        if (hasAnimated) return;
        hasAnimated = true;

        animatableCards.clear();
        animatableCards.add(findViewById(R.id.lab_header));
        if (cardPowerFlow.getVisibility() == View.VISIBLE) animatableCards.add(cardPowerFlow);
        if (cardSource.getVisibility() == View.VISIBLE) animatableCards.add(cardSource);
        if (cardTemp.getVisibility() == View.VISIBLE) animatableCards.add(cardTemp);
        if (cardVoltage.getVisibility() == View.VISIBLE) animatableCards.add(cardVoltage);
        if (cardHealth.getVisibility() == View.VISIBLE) animatableCards.add(cardHealth);
        if (cardSafeCharged.getVisibility() == View.VISIBLE) animatableCards.add(cardSafeCharged);

        for (int i = 0; i < animatableCards.size(); i++) {
            View card = animatableCards.get(i);
            card.setAlpha(0f);
            card.setTranslationY(50f);
            card.setScaleX(0.95f);
            card.setScaleY(0.95f);

            ObjectAnimator a = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
            ObjectAnimator ty = ObjectAnimator.ofFloat(card, "translationY", 50f, 0f);
            ObjectAnimator sx = ObjectAnimator.ofFloat(card, "scaleX", 0.95f, 1f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(card, "scaleY", 0.95f, 1f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(a, ty, sx, sy);
            set.setDuration(450);
            set.setStartDelay(i * 70L);
            set.setInterpolator(new OvershootInterpolator(0.8f));
            set.start();

            // Haptic tick for first few cards
            if (i < 3) {
                handler.postDelayed(() -> HapticUtils.playCustomVibration(this,
                        new long[]{3, 0, 3, 1, 2}), i * 70L + 80L);
            }
        }
    }

    /**
     * Bouncy scale animation for value text when it updates.
     */
    private void animateValueBounce(View view) {
        view.setScaleX(0.6f);
        view.setScaleY(0.6f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator(2.5f))
                .start();
    }

    private void updateLiveMetrics() {
        try {
            long currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW); // microAmperes

            Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (intent != null) {
                int voltageMm = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1); // milliVolts
                if (voltageMm != -1) {
                    double currentA = currentNow / 1000000.0;
                    double voltageV = voltageMm / 1000.0;
                    double wattage = voltageV * currentA;

                    txtWattage.setText(String.format(Locale.getDefault(), "%.1f W", Math.abs(wattage)));
                    cardPowerFlow.setVisibility(View.VISIBLE);

                    // Show power direction
                    if (wattage > 0.01) {
                        txtPowerDirection.setText("⚡ Energy flowing into battery");
                    } else if (wattage < -0.01) {
                        txtPowerDirection.setText("↗ Battery powering device");
                    } else {
                        txtPowerDirection.setText("Battery idle");
                    }

                    txtVoltage.setText(String.format(Locale.getDefault(), "%.2f V", voltageV));
                    cardVoltage.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating current/wattage", e);
            cardPowerFlow.setVisibility(View.GONE);
            cardVoltage.setVisibility(View.GONE);
        }
    }

    private void updateStickyMetrics(Intent intent) {
        // 1. Temperature
        int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        if (temp != -1) {
            double tempC = temp / 10.0;
            txtTemp.setText(String.format(Locale.getDefault(), "%.1f°", tempC));
            cardTemp.setVisibility(View.VISIBLE);

            if (tempC > 40) {
                txtTemp.setTextColor(ContextCompat.getColor(this, R.color.red));
            } else {
                txtTemp.setTextColor(ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorOnSurface));
            }
        }

        // 2. Health
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        if (health != -1) {
            String healthStr = getHealthString(health);
            txtHealth.setText(healthStr);
            cardHealth.setVisibility(View.VISIBLE);
        }

        // 3. Charging Source
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        if (plugged != -1 && plugged != 0) {
            cardSource.setVisibility(View.VISIBLE);
            String source;

            if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
                source = "Wall Charger (AC)";
            } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                source = "USB Port";
            } else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                source = "Wireless Dock";
            } else {
                source = "Charging";
            }
            txtSourceType.setText(source);

            try {
                long currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                if (currentNow > 1500000) {
                    txtChargeStatus.setText("⚡ Rapidly Charging");
                } else {
                    txtChargeStatus.setText("Standard Charging");
                }
            } catch (Exception e) {
                txtChargeStatus.setText("Charging");
            }
        } else {
            cardSource.setVisibility(View.GONE);
        }

        // After first data load, run the entry animation
        if (!hasAnimated) {
            handler.postDelayed(this::runStaggeredEntryAnimation, 100);
        }

        // Adapt grid based on available info
        adaptGridLayout();
    }

    private String getHealthString(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Overheated";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Over Voltage";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "Failure";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Too Cold";
            default: return "Unknown";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        handler.post(updateRunnable);
        updateSafeChargedCounter();
        adaptGridLayout();
    }

    /**
     * Hides entire grid rows when both cards are gone,
     * so the layout stays clean on devices/API levels
     * where some metrics aren't available.
     */
    private void adaptGridLayout() {
        // Row 1: temp + voltage
        boolean tempVisible = cardTemp.getVisibility() == View.VISIBLE;
        boolean voltageVisible = cardVoltage.getVisibility() == View.VISIBLE;
        gridRow1.setVisibility((tempVisible || voltageVisible) ? View.VISIBLE : View.GONE);

        // Row 2: health + safecharged
        boolean healthVisible = cardHealth.getVisibility() == View.VISIBLE;
        boolean safechargedVisible = cardSafeCharged.getVisibility() == View.VISIBLE;
        gridRow2.setVisibility((healthVisible || safechargedVisible) ? View.VISIBLE : View.GONE);
    }

    private void updateSafeChargedCounter() {
        android.content.SharedPreferences sp = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        int counter = sp.getInt("counter", 0);
        txtSafeCharged.setText(String.valueOf(counter));
        if (counter > 0) {
            cardSafeCharged.setVisibility(View.VISIBLE);
            animateValueBounce(txtSafeCharged);
        } else {
            cardSafeCharged.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryReceiver);
        handler.removeCallbacks(updateRunnable);
    }
}
