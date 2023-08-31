package com.onesilicondiode.batterywise;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
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

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;

import me.itangqi.waveloadingview.WaveLoadingView;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_APP_UPDATE = 123;
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
    private WaveLoadingView waveLoadingView;
    boolean seekTouch = false;
    private float scaleFactorStretched = 1.2f; // Adjust the scaling factor as needed
    private float scaleFactorOriginal = 1.0f;
    private FirebaseAnalytics mFirebaseAnalytics;
    private Vibrator vibrator;
    private String manufacturer;
    private AppUpdateManager appUpdateManager;

    public static int getThemeColor(Context context, int colorResId) {
        TypedValue typedValue = new TypedValue();
        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, new int[]{colorResId});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      //  SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        DynamicColors.applyToActivityIfAvailable(this);
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        getWindow().setStatusBarColor(getThemeColor(this, android.R.attr.colorPrimaryDark));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        waveLoadingView = findViewById(R.id.waveLoadingView); // Initialize the WaveLoadingView
        appUpdateManager = AppUpdateManagerFactory.create(this);
        InstallStateUpdatedListener installStateUpdatedListener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                // The update has been downloaded, trigger the installation
                appUpdateManager.completeUpdate();
            }
        };
        appUpdateManager.registerListener(installStateUpdatedListener);

        // Check for app updates
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                // An update is available, and it can be installed immediately
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            this,
                            REQUEST_CODE_APP_UPDATE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        startSaving = findViewById(R.id.saveBatteryBtn);
        stopSaving = findViewById(R.id.closeBatteryBtn);
        productInfo = findViewById(R.id.productInfo);
        Animation showAnimation = AnimationUtils.loadAnimation(this, R.anim.popup_show);
        Animation hideAnimation = AnimationUtils.loadAnimation(this, R.anim.popup_hide);
        TextView seekBarValueOverlay = findViewById(R.id.seekBarValueOverlay);
        manufacturer = Build.MANUFACTURER;
        BatteryManager batteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);
        int batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        waveLoadingView.setProgressValue(batteryPercent);
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
                String progressText = selectedBatteryLevel + "%";
                seekBarValueOverlay.setText(progressText);
                vibrateTouch();
                // Display the overlay
                seekBarValueOverlay.startAnimation(showAnimation);
                seekBarValueOverlay.setVisibility(View.VISIBLE);
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
                scaleSeekBar(seekBar, 1.2f);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!seekTouch) {
                    Toast.makeText(MainActivity.this, "You can set more precisely using volume buttons", Toast.LENGTH_LONG).show();
                    seekTouch = true;
                }
                scaleSeekBar(seekBar, 1.0f);
                seekBarValueOverlay.startAnimation(hideAnimation);
                seekBarValueOverlay.setVisibility(View.GONE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                // If the update is not completed, you can handle it here
            }
        }
    }

    private void scaleSeekBar(SeekBar seekBar, float scaleFactor) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(seekBar, "scaleX", scaleFactor);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(seekBar, "scaleY", scaleFactor);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(200); // Adjust the duration as needed

        animatorSet.start();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            vibrator.vibrate(
                    VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.3f)
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.3f)
                            .compose());
        } else {
            long[] pattern = {0, 100, 100}; // Vibrate for 100 milliseconds, pause for 100 milliseconds, and repeat
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(vibrationEffect);
        }
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