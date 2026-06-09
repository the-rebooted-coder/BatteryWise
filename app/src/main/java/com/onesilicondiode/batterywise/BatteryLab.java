package com.onesilicondiode.batterywise;

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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class BatteryLab extends AppCompatActivity {

    private static final String TAG = "BatteryLab";
    private TextView txtWattage, txtTemp, txtHealth, txtVoltage, txtSourceType, txtChargeStatus, txtSafeCharged;
    private View cardPowerFlow, cardTemp, cardHealth, cardVoltage, cardSource, cardSafeCharged;
    
    private BatteryManager batteryManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
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
    }

    private void initViews() {
        txtWattage = findViewById(R.id.txt_wattage);
        txtTemp = findViewById(R.id.txt_temp);
        txtHealth = findViewById(R.id.txt_health);
        txtVoltage = findViewById(R.id.txt_voltage);
        txtSourceType = findViewById(R.id.txt_source_type);
        txtChargeStatus = findViewById(R.id.txt_charge_status);
        txtSafeCharged = findViewById(R.id.txt_safecharged);

        cardPowerFlow = findViewById(R.id.card_power_flow);
        cardTemp = findViewById(R.id.card_temp);
        cardHealth = findViewById(R.id.card_health);
        cardVoltage = findViewById(R.id.card_voltage);
        cardSource = findViewById(R.id.card_source);
        cardSafeCharged = findViewById(R.id.card_safecharged);
    }

    private void setupTooltips() {
        findViewById(R.id.info_power_flow).setOnClickListener(v -> showTooltip("Power Flow", "The real-time energy entering or leaving your battery. Measured in Watts."));
        findViewById(R.id.info_temp).setOnClickListener(v -> showTooltip("Temperature", "Internal heat level. Batteries hate heat; staying below 40°C is ideal for longevity."));
        findViewById(R.id.info_health).setOnClickListener(v -> showTooltip("Battery Health", "Hardware-level health assessment reported by the system."));
        findViewById(R.id.info_voltage).setOnClickListener(v -> showTooltip("Voltage", "The electrical pressure provided by the battery. Stable voltage indicates a healthy power system."));
        findViewById(R.id.info_safecharged).setOnClickListener(v -> showTooltip("SafeCharged", "The total number of times SafeCharge successfully monitored your battery and prevented potential overcharging."));
    }

    private void showTooltip(String title, String message) {
        HapticUtils.vibrateTouch(this);
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got it", null)
                .show();
    }

    private void updateLiveMetrics() {
        try {
            long currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW); // microAmperes
            
            // On Emulators, currentNow is often 0 or invalid. For testing/demo, we ensure it shows something if available.
            Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (intent != null) {
                int voltageMm = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1); // milliVolts
                if (voltageMm != -1) {
                    // Wattage = (V * A). If current is 0 (emulator), wattage will be 0.
                    double currentA = currentNow / 1000000.0;
                    double voltageV = voltageMm / 1000.0;
                    double wattage = voltageV * currentA;
                    
                    txtWattage.setText(String.format(Locale.getDefault(), "%.1f W", Math.abs(wattage)));
                    cardPowerFlow.setVisibility(View.VISIBLE);
                    
                    txtVoltage.setText(String.format(Locale.getDefault(), "%.1f V", voltageV));
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
            txtTemp.setText(String.format(Locale.getDefault(), "%.1f °C", tempC));
            cardTemp.setVisibility(View.VISIBLE);
            
            if (tempC > 40) txtTemp.setTextColor(ContextCompat.getColor(this, R.color.red));
            else txtTemp.setTextColor(ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorOnSurface));
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
                    txtChargeStatus.setText("Rapidly Charging");
                } else {
                    txtChargeStatus.setText("Standard Charging");
                }
            } catch (Exception e) {
                txtChargeStatus.setText("Charging");
            }
        } else {
            cardSource.setVisibility(View.GONE);
        }
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
    }

    private void updateSafeChargedCounter() {
        android.content.SharedPreferences sp = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        int counter = sp.getInt("counter", 0);
        txtSafeCharged.setText(String.valueOf(counter));
        if (counter > 0) {
            cardSafeCharged.setVisibility(View.VISIBLE);
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
