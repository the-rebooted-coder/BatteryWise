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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.transition.Transition;
import androidx.transition.TransitionInflater;
import androidx.transition.TransitionManager;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "MyPrefsFile";
    MaterialButton startSaving, stopSaving;
    private FirebaseAnalytics mFirebaseAnalytics;
    private Vibrator vibrator;
    TextView productInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startSaving = findViewById(R.id.saveBatteryBtn);
        stopSaving = findViewById(R.id.closeBatteryBtn);
        productInfo = findViewById(R.id.productInfo);
        String manufacturer = Build.MANUFACTURER;
        String productInfoText = getString(R.string.productInfo) + " " + manufacturer + " phone " + getString(R.string.productInfo_partTwo);

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
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        // Get the list of running app processes
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = manager.getRunningAppProcesses();
        if (runningProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                // Check if the service's component name is in the process's package list
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    String[] packageList = processInfo.pkgList;
                    for (String packageName : packageList) {
                        if (packageName.equals(getPackageName())) {
                            return true; // Your service is running in the foreground
                        }
                    }
                }
            }
        }

        return false; // Your service is not running
    }

    private void vibrate() {
        long[] customPattern = {0, 1000, 500, 500};
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
}