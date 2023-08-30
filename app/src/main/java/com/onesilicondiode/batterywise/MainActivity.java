package com.onesilicondiode.batterywise;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, perform your action
                    Toast.makeText(this, "Continue to Enable the Service", Toast.LENGTH_SHORT).show();

                } else {
                    // Permission denied
                    Toast.makeText(this, "Permission denied, cannot post notification to keep app alive 😔", Toast.LENGTH_LONG).show();
                }
            });
    MaterialButton startSaving, stopSaving;
    TextView productInfo;
    int selectedBatteryLevel = 85;
    boolean seekTouch = false;
    private FirebaseAnalytics mFirebaseAnalytics;
    private Vibrator vibrator;
    private String manufacturer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startSaving = findViewById(R.id.saveBatteryBtn);
        stopSaving = findViewById(R.id.closeBatteryBtn);
        productInfo = findViewById(R.id.productInfo);
        manufacturer = Build.MANUFACTURER;
        SeekBar batteryLevelSeekBar = findViewById(R.id.batteryLevelSeekBar);
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        selectedBatteryLevel = prefs.getInt("selectedBatteryLevel", 85);
        String productInfoText = getString(R.string.productInfo) + " " + manufacturer + " phone " + getString(R.string.productInfo_partTwo) + " " + selectedBatteryLevel + "%";
        productInfo.setText(productInfoText);
        int seekBarProgress = selectedBatteryLevel - 80;
        batteryLevelSeekBar.setProgress(seekBarProgress);
        batteryLevelSeekBar.setMax(10);
        batteryLevelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Calculate the selected battery level based on the progress
                selectedBatteryLevel = 80 + progress;

                // Update a shared preference or perform any action with the selectedBatteryLevel
                SharedPreferences.Editor editor = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit();
                editor.putInt("selectedBatteryLevel", selectedBatteryLevel);
                editor.apply();

                // Update the TextView to display the selected battery level
                String productInfoText = getString(R.string.productInfo) + " " + manufacturer + " phone " + getString(R.string.productInfo_partTwo) + " " + selectedBatteryLevel + "%";
                productInfo.setText(productInfoText);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                vibrateTouch();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!seekTouch) {
                    Toast.makeText(MainActivity.this, "You can set more precisely using volume buttons", Toast.LENGTH_LONG).show();
                    seekTouch = true;
                }
            }
        });
        productInfo.setText(productInfoText);
        boolean isServiceRunning = isServiceRunning(BatteryMonitorService.class);
        if (isServiceRunning) {
            startSaving.setVisibility(View.GONE);
            stopSaving.setVisibility(View.VISIBLE);
        } else {
            startSaving.setVisibility(View.VISIBLE);
            stopSaving.setVisibility(View.GONE);
        }
        TextView batterySaveText = findViewById(R.id.batterySaveText);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        batterySaveText.setOnClickListener(view -> {
            // Create a LayoutInflater
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.popup_info, null);

            // Create a PopupWindow
            int width = LayoutParams.MATCH_PARENT;
            int height = LayoutParams.WRAP_CONTENT;
            boolean focusable = true;
            PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

            // Set a custom background for the popup
            popupWindow.setBackgroundDrawable(getDrawable(R.drawable.popup_background));

            // Show the popup with custom animations
            popupWindow.setAnimationStyle(0); // Disable the default animation
            Animation enterAnimation = AnimationUtils.loadAnimation(this, R.anim.popup_enter_animation);
            popupView.startAnimation(enterAnimation);

            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

            // Dismiss the popup with exit animation when needed
            popupView.setOnClickListener(v -> {
                Animation exitAnimation = AnimationUtils.loadAnimation(this, R.anim.popup_exit_animation);
                popupView.startAnimation(exitAnimation);
                popupWindow.dismiss();
            });
        });

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission();
        }
        startSaving.setOnClickListener(view -> {
            Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
            startService(serviceIntent);
            startSaving.setVisibility(View.GONE);
            vibrate();
            stopSaving.setVisibility(View.VISIBLE);
            Toast.makeText(MainActivity.this, "Service Enabled", Toast.LENGTH_SHORT).show();
            finish();
        });

        stopSaving.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent serviceIntent = new Intent(MainActivity.this, BatteryMonitorService.class);
                stopService(serviceIntent);
                Toast.makeText(MainActivity.this, "Service Stopped", Toast.LENGTH_SHORT).show();
                stopSaving.setVisibility(View.GONE);
                startSaving.setVisibility(View.VISIBLE);
                vibrate();
            }
        });
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void vibrate() {
        long[] customPattern = {0, 1000, 500, 500};
        // Create a VibrationEffect
        VibrationEffect vibrationEffect = VibrationEffect.createWaveform(customPattern, -1);
        // Vibrate with the custom pattern
        vibrator.vibrate(vibrationEffect);
    }

    private void vibrateKeys() {
        long[] customPattern = {0, 200, 500, 400};
        // Create a VibrationEffect
        VibrationEffect vibrationEffect = VibrationEffect.createWaveform(customPattern, -1);
        // Vibrate with the custom pattern
        vibrator.vibrate(vibrationEffect);
    }

    private void vibrateTouch() {
        long[] customPattern = {0, 290};
        // Create a VibrationEffect
        VibrationEffect vibrationEffect = VibrationEffect.createWaveform(customPattern, -1);
        // Vibrate with the custom pattern
        vibrator.vibrate(vibrationEffect);
    }

    private void checkNotificationPermission() {
        String permission = Manifest.permission.POST_NOTIFICATIONS;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, proceed with your action here
            // You can call the method for your action
        } else if (shouldShowRequestPermissionRationale(permission)) {
            // Permission denied previously, show rationale dialog
            showPermissionRationaleDialog();
        } else {
            // Request notification permission
            requestNotificationPermission.launch(permission);
        }
    }

    private void showPermissionRationaleDialog() {
        // For example, you can use a AlertDialog:
        new AlertDialog.Builder(this)
                .setTitle("Notification Permission is Required")
                .setMessage("Permissions are required to show notification in-order to keep the battery monitoring service active.")
                .setPositiveButton("GRANT", (dialog, which) -> {
                    // Request permission after explaining
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Handle if the user cancels the request
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            SeekBar batteryLevelSeekBar = findViewById(R.id.batteryLevelSeekBar);

            // Calculate the selected battery level based on the current seek bar progress
            int seekBarProgress = batteryLevelSeekBar.getProgress();
            int newSeekBarProgress;

            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                // Increase the progress (increase battery level) if within bounds
                newSeekBarProgress = seekBarProgress + 1;
                newSeekBarProgress = Math.min(10, newSeekBarProgress); // Limit to 10
            } else {
                // Decrease the progress (decrease battery level) if within bounds
                newSeekBarProgress = seekBarProgress - 1;
                newSeekBarProgress = Math.max(0, newSeekBarProgress); // Limit to 0
            }

            // Update the seek bar progress
            batteryLevelSeekBar.setProgress(newSeekBarProgress);

            // Update the selectedBatteryLevel and save it to SharedPreferences
            selectedBatteryLevel = 80 + newSeekBarProgress;
            SharedPreferences.Editor editor = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit();
            editor.putInt("selectedBatteryLevel", selectedBatteryLevel);
            editor.apply();

            // Update the TextView to display the selected battery level
            String productInfoText = getString(R.string.productInfo) + " " + manufacturer + " phone " + getString(R.string.productInfo_partTwo) + " " + selectedBatteryLevel + "%";
            productInfo.setText(productInfoText);
            vibrateKeys();
            return true; // Consume the volume key press event
        }

        return super.onKeyDown(keyCode, event);
    }
}