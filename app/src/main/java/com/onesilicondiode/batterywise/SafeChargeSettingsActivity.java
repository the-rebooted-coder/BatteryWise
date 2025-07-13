package com.onesilicondiode.batterywise;

import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

public class SafeChargeSettingsActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "safecharge_prefs";
    public static final String ZEN_MODE_KEY = "zen_mode";
    private OnBackInvokedCallback predictiveBackCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use a proper AppCompat theme! Set in Manifest, not here ideally.
        setContentView(R.layout.activity_safecharge_settings);

        SwitchCompat zenSwitch = findViewById(R.id.zen_mode_switch);
        boolean zenEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(ZEN_MODE_KEY, false);
        zenSwitch.setChecked(zenEnabled);

        zenSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putBoolean(ZEN_MODE_KEY, isChecked)
                        .apply()
        );

        TextView summary = findViewById(R.id.zen_mode_summary);
        zenSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(ZEN_MODE_KEY, isChecked)
                    .apply();
            if (isChecked) {
                summary.setText("Zen Mode is ON. Your clock will be blue and serene.");
            } else {
                summary.setText("Zen Mode makes your clock blue for a more peaceful screensaver experience.");
            }
        });

        // Predictive Back Navigation (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 15+
            predictiveBackCallback = () -> finish(); // just finish activity on back
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    predictiveBackCallback
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister predictive back callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && predictiveBackCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(predictiveBackCallback);
        }
    }
}