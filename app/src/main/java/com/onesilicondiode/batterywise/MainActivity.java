package com.onesilicondiode.batterywise;

import static com.onesilicondiode.batterywise.Constants.PREFS_NAME;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;

import me.itangqi.waveloadingview.WaveLoadingView;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_APP_UPDATE = 123;
    private static final String PREF_SEEK_TOUCH = "seekTouch";
    private static final String USER_STARTED_KEY = "userStarted";
    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    // Permission granted, perform your action
                    Toast.makeText(this, "Permission denied, cannot post notification to keep SafeCharge working 😔", Toast.LENGTH_LONG).show();

                }
            });
    MaterialButton startSaving, stopSaving;
    TextView productInfo;
    int selectedBatteryLevel = 85;
    private WaveLoadingView waveLoadingView;
    private float scaleFactorStretched = 1.2f; // Adjust the scaling factor as needed
    private float scaleFactorOriginal = 1.0f;
    private FirebaseAnalytics mFirebaseAnalytics;
    private boolean seekTouch = false;
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
        SplashScreen.installSplashScreen(this);
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
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        seekTouch = prefs.getBoolean(PREF_SEEK_TOUCH, false);
        selectedBatteryLevel = prefs.getInt("selectedBatteryLevel", 85);
        String productInfoText = "Your " + manufacturer + " phone " + getString(R.string.productInfo_partTwo) + " " + selectedBatteryLevel + "%";
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
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putInt("selectedBatteryLevel", selectedBatteryLevel);
                editor.apply();
                // Update the TextView to display the selected battery level
                String productInfoText = "Your " + manufacturer + " phone " + getString(R.string.productInfo_partTwo) + " " + selectedBatteryLevel + "%";
                productInfo.setText(productInfoText);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                scaleSeekBar(seekBar, 1.2f);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!seekTouch) {
                    showSnackbar();
                    seekTouch = true;
                }
                vibrateTouch();
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

            // Get the ImageView inside the popup
            MaterialButton popupButton = popupView.findViewById(R.id.popupButton);

            // Add an OnClickListener to the ImageView
            popupButton.setOnClickListener(v -> {
                // Create an Intent to open the NewActivity
                Intent intent = new Intent(MainActivity.this, About.class);
                startActivity(intent);
                vibrate();
                // Dismiss the popup
                popupWindow.dismiss();
            });

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
            try {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(USER_STARTED_KEY, true);
                editor.apply();
                enableAutoStart();
            } catch (Exception e) {
                showIntentErrorDialog();
            }
        });
        stopSaving.setOnClickListener(view -> {
            Intent serviceIntent = new Intent(MainActivity.this, BatteryMonitorService.class);
            stopService(serviceIntent);
            stopSaving.setVisibility(View.GONE);
            startSaving.setVisibility(View.VISIBLE);
            vibrate();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                //DO NOT REMOVE THIS
            }
        }
    }

    private void showSnackbar() {
        CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinatorLayout);

        Snackbar snackbar = Snackbar.make(coordinatorLayout, "You can also use the volume buttons to control slider", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("Okay", view -> {
            seekTouch = true;
            saveSeekTouchState();
            snackbar.dismiss();
        });
        snackbar.show();
    }

    private void saveSeekTouchState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_SEEK_TOUCH, seekTouch);
        editor.apply();
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
        long[] pattern = {11, 0, 11, 0, 20, 2, 23, 6, 10, 9, 0, 12, 11, 14, 0, 16, 12, 17, 0, 16, 10, 15, 0, 13};
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(vibrationEffect);
        } else {
            // For versions lower than Oreo
            vibrator.vibrate(pattern, -1);
        }
    }

    private void vibrateKeys() {
        long[] customPattern = {14, 0, 10, 9, 10, 15, 0, 11, 9, 5, 12, 2, 12, 4, 10, 7, 0, 11, 11, 14, 14, 25, 11, 25, 9};
        // Create a VibrationEffect
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(customPattern, -1);
            vibrator.vibrate(vibrationEffect);
        } else {
            // For versions lower than Oreo
            vibrator.vibrate(customPattern, -1);
        }
    }

    private void vibrateTouch() {
        long[] pattern = {7, 0, 8, 1, 6, 6, 6, 11, 5, 15, 0, 17, 5};
        // Create a VibrationEffect
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(vibrationEffect);
        } else {
            // For versions lower than Oreo
            vibrator.vibrate(pattern, -1);
        }
    }

    private void checkNotificationPermission() {
        String permission = Manifest.permission.POST_NOTIFICATIONS;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
        } else if (shouldShowRequestPermissionRationale(permission)) {
            // Permission denied previously, show rationale dialog
            showPermissionRationaleDialog();
        } else {
            // Request notification permission
            showPermissionRationaleDialog();
        }
    }

    private void showPermissionRationaleDialog() {
        View customView = getLayoutInflater().inflate(R.layout.custom_alert_dialog, null);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notification_notify)
                .setMessage(R.string.notify_desc)
                .setView(customView)
                .setPositiveButton("Continue", (dialog, which) -> {
                    vibrate();
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                })
                .setNegativeButton("No Thanks", (dialog, which) -> {
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
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putInt("selectedBatteryLevel", selectedBatteryLevel);
            editor.apply();

            // Update the TextView to display the selected battery level
            String productInfoText = "Your " + manufacturer + " phone " + getString(R.string.productInfo_partTwo) + " " + selectedBatteryLevel + "%";
            productInfo.setText(productInfoText);
            vibrateKeys();
            return true; // Consume the volume key press event
        }

        return super.onKeyDown(keyCode, event);
    }

    private void enableAutoStart() {
        String brand = Build.BRAND;
        String manufacturer = Build.MANUFACTURER;
        if (brand.equalsIgnoreCase("xiaomi")) {
            View customView = getLayoutInflater().inflate(R.layout.request_autostart_dialog, null);
            startService();
            vibrateTouch();
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Enable AutoStart")
                    .setMessage("You're using a Xiaomi Phone, enable Autostart to use SafeCharge on your device.")
                    .setCancelable(false)
                    .setView(customView)
                    .setPositiveButton("Continue", (dialog, which) -> {
                        Intent[] AUTO_START_XIAOMI = {
                                new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())),
                        };
                        boolean intentLaunched = false;
                        for (Intent intent : AUTO_START_XIAOMI) {
                            if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                                try {
                                    startActivity(intent);
                                    intentLaunched = true;
                                    break;
                                } catch (Exception e) {
                                    Log.e("Xiaomi ki mkb", "Crash Bach Gaya");
                                }
                            }
                        }
                        if (!intentLaunched) {
                            // Show the error dialog here because none of the Intents were successfully launched
                            showIntentErrorDialog();
                        }
                    })
                    .show();
        } else if (brand.equalsIgnoreCase("Letv")) {
            showAlertDialog("Letv", "com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity");
        } else if (brand.equalsIgnoreCase("Honor")) {
            showHuaweiAlertDialog();
        } else if (manufacturer.equalsIgnoreCase("oppo")) {
            showOppoAlertDialog();
        } else if (manufacturer.contains("vivo")) {
            showVivoAlertDialog();
        } else if (manufacturer.contains("asus")) {
            showAlertDialog("Asus", "com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity");
        } else if (manufacturer.contains("samsung")) {
            showAlertDialog("Samsung", "com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity");
        } else {
            Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
            startService(serviceIntent);
            startSaving.setVisibility(View.GONE);
            vibrate();
            stopSaving.setVisibility(View.VISIBLE);
            finish();
        }
    }

    private void showAlertDialog(String brandName, String componentNamePackage, String componentNameClass) {
        View customView = getLayoutInflater().inflate(R.layout.request_autostart_dialog, null);
        startService();
        vibrateTouch();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Enable AutoStart")
                .setMessage("You're using a " + brandName + " Phone, AutoStart is required to enable SafeCharge on your device.")
                .setCancelable(false)
                .setView(customView)
                .setPositiveButton("Continue", (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(componentNamePackage, componentNameClass));
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showIntentErrorDialog();
                    }
                })
                .show();
    }

    private void showIntentErrorDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cannot auto-open Settings 😔")
                .setMessage("SafeCharge tried opening app settings, to help you enable AutoStart but failed to do so.\n\nPlease open App Info Manually and allow SafeCharge to AutoStart/Un-restrict Battery Usage.")
                .setPositiveButton("OK", (dialog, which) -> {
                    vibrateKeys();
                })
                .show();
    }

    private void showHuaweiAlertDialog() {
        View customView = getLayoutInflater().inflate(R.layout.request_autostart_dialog, null);
        startService();
        vibrateTouch();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Enable AutoStart")
                .setMessage("You're using a Huawei Phone, autostart is required to enable SafeCharge on your device.")
                .setCancelable(false)
                .setView(customView)
                .setPositiveButton("Continue", (dialog, which) -> {
                    Intent[] AUTO_START_HONOR = {
                            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
                            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                    };
                    boolean intentLaunched = false;
                    for (Intent intent : AUTO_START_HONOR) {
                        if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                            try {
                                startActivity(intent);
                                intentLaunched = true;
                                break;
                            } catch (Exception e) {
                                Log.e("Honor ki mkb", "Crash Bach Gaya");
                            }
                        }
                    }
                    if (!intentLaunched) {
                        // Show the error dialog here because none of the Intents were successfully launched
                        showIntentErrorDialog();
                    }
                })
                .show();
    }

    private void showOppoAlertDialog() {
        View customView = getLayoutInflater().inflate(R.layout.request_autostart_dialog, null);
        startService();
        vibrateTouch();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Enable AutoStart")
                .setMessage("You're using an Oppo Phone, autostart is required to enable SafeCharge on your device.")
                .setView(customView)
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, which) -> {
                    Intent[] AUTO_START_OPPO = {
                            new Intent().setComponent(new ComponentName("com.coloros.safe", "com.coloros.safe.permission.startup.StartupAppListActivity")),
                            new Intent().setComponent(new ComponentName("com.coloros.safe", "com.coloros.safe.permission.startupapp.StartupAppListActivity")),
                            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
                            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startup.StartupAppListActivity")),
                            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.privacypermissionsentry.PermissionTopActivity"))
                    };
                    boolean intentLaunched = false;
                    for (Intent intent : AUTO_START_OPPO) {
                        if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                            try {
                                startActivity(intent);
                                intentLaunched = true;
                                break;
                            } catch (Exception e) {
                                Log.e("Oppo ki mkb", "Crash Bach Gaya");
                            }
                        }
                    }
                    if (!intentLaunched) {
                        // Show the error dialog here because none of the Intents were successfully launched
                        showIntentErrorDialog();
                    }
                })
                .show();
    }

    private void showVivoAlertDialog() {
        View customView = getLayoutInflater().inflate(R.layout.request_autostart_dialog, null);
        startService();
        vibrateTouch();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Enable AutoStart")
                .setMessage("You're using a Vivo Phone, autostart is required to enable SafeCharge on your device.")
                .setCancelable(false)
                .setView(customView)
                .setPositiveButton("Continue", (dialog, which) -> {
                    Intent[] AUTO_START_VIVO = {
                            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                            new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
                            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
                    };
                    boolean intentLaunched = false;
                    for (Intent intent : AUTO_START_VIVO) {
                        if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                            try {
                                startActivity(intent);
                                intentLaunched = true;
                                break;
                            } catch (Exception e) {
                                Log.e("Vivo ki mkb", "Crash Bach Gaya");
                            }
                        }
                    }
                    if (!intentLaunched) {
                        showIntentErrorDialog();
                    }
                })
                .show();
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
        startService(serviceIntent);
        startSaving.setVisibility(View.GONE);
        vibrate();
        stopSaving.setVisibility(View.VISIBLE);
    }
}