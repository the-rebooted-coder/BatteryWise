package com.onesilicondiode.batterywise;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.firebase.analytics.FirebaseAnalytics;

import me.itangqi.waveloadingview.WaveLoadingView;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_APP_UPDATE = 123;
    private static final String PREF_SEEK_TOUCH = "seekTouch";
    private static final String USER_STARTED_KEY = "userStarted";
    private static final String SELECTED_TIME_KEY = "selected_time";
    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    // Permission granted, perform your action
                    Toast.makeText(this, "SafeCharge will not work, restart app to grant permission", Toast.LENGTH_LONG).show();

                }
            });
    MaterialButton startSaving, stopSaving, oneMin, twoMin, threeMin, thirtySec;
    TextView productInfo;
    int selectedBatteryLevel = 85;
    private WaveLoadingView waveLoadingView;
    private final float scaleFactorStretched = 1.2f; // Adjust the scaling factor as needed
    private final float scaleFactorOriginal = 1.0f;
    private FirebaseAnalytics mFirebaseAnalytics;
    private boolean seekTouch = false;
    private Vibrator vibrator;
    private String manufacturer;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String SWITCH_STATE = "switchState";
    private AppUpdateManager appUpdateManager;
    private ReviewManager reviewManager;
    private MaterialSwitch switchToggle;
    private SharedPreferences sharedPreferences;
    private LinearLayout buttonToggleGroup;
    private MaterialButton currentSelectedButton;


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
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        reviewManager = ReviewManagerFactory.create(this);
        waveLoadingView = findViewById(R.id.waveLoadingView);
        buttonToggleGroup = findViewById(R.id.buttonToggleGroup);
        appUpdateManager = AppUpdateManagerFactory.create(this);
        oneMin = findViewById(R.id.button_1m);
        twoMin = findViewById(R.id.button_2m);
        threeMin = findViewById(R.id.button_3m);
        thirtySec = findViewById(R.id.button_45s);
        InstallStateUpdatedListener installStateUpdatedListener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                // The update has been downloaded, trigger the installation
                appUpdateManager.completeUpdate();
            }
        };
        appUpdateManager.registerListener(installStateUpdatedListener);
        switchToggle = findViewById(R.id.switchToggle);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // Load the previous state of the switch toggle from SharedPreferences
        switchToggle.setChecked(sharedPreferences.getBoolean(SWITCH_STATE, false));
        boolean switchState = sharedPreferences.getBoolean(SWITCH_STATE, true);
        int selectedTime = sharedPreferences.getInt(SELECTED_TIME_KEY, 2);
        switch (selectedTime) {
            case 1:
                handleButtonSelection(oneMin, 1);
                break;
            case 2:
                handleButtonSelection(twoMin, 2);
                break;
            case 3:
                handleButtonSelection(threeMin, 3);
                break;
            case 4:
                handleButtonSelection(thirtySec, 4);
                break;
        }
        updateSegmentedButtonVisibility(switchState);
        thirtySec.setOnClickListener(v -> handleButtonSelection(thirtySec, 4));
        oneMin.setOnClickListener(v -> handleButtonSelection(oneMin, 1));
        twoMin.setOnClickListener(v -> handleButtonSelection(twoMin, 2));
        threeMin.setOnClickListener(v -> handleButtonSelection(threeMin, 3));
        // Set a listener to save the state of the switch toggle in SharedPreferences when changed
        switchToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Switch is ON, show the segmented buttons
                buttonToggleGroup.setVisibility(View.VISIBLE);
            } else {
                // Switch is OFF, hide the segmented buttons
                buttonToggleGroup.setVisibility(View.GONE);
            }
            // Save the state of the switch toggle in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SWITCH_STATE, isChecked);
            editor.apply();
            vibrateTouch();
        });

        //Bottom Sheet What's New
        if (sharedPreferences.getBoolean("isConfirmed", true)) {
            showBottomSheet();
        }
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
        int counter = getCounterFromSharedPreferences();
        startSaving = findViewById(R.id.saveBatteryBtn);
        stopSaving = findViewById(R.id.closeBatteryBtn);
        productInfo = findViewById(R.id.productInfo);
        Animation showAnimation = AnimationUtils.loadAnimation(this, R.anim.popup_show);
        Animation hideAnimation = AnimationUtils.loadAnimation(this, R.anim.popup_hide);
        TextView seekBarValueOverlay = findViewById(R.id.seekBarValueOverlay);
        manufacturer = Build.MANUFACTURER;
        BatteryManager batteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);
        int batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        if (batteryPercent > 55) {
            waveLoadingView.setWaveColor(Color.parseColor("#006C49"));
            waveLoadingView.setProgressValue(batteryPercent);
        } else if (batteryPercent > 40 && batteryPercent < 55) {
            waveLoadingView.setWaveColor(Color.parseColor("#F6D3A1"));
            waveLoadingView.setProgressValue(batteryPercent);
        } else if (batteryPercent > 20 && batteryPercent < 40) {
            waveLoadingView.setWaveColor(Color.parseColor("#E4B284"));
            waveLoadingView.setProgressValue(batteryPercent);
        } else if (batteryPercent > 1 && batteryPercent < 20) {
            waveLoadingView.setWaveColor(Color.parseColor("#FFB4AB"));
            waveLoadingView.setProgressValue(batteryPercent);
        }
        Slider batteryLevelSlider = findViewById(R.id.batteryLevelSlider);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        seekTouch = prefs.getBoolean(PREF_SEEK_TOUCH, false);
        selectedBatteryLevel = prefs.getInt("selectedBatteryLevel", 85);
        String productInfoText;
        if (selectedBatteryLevel > 98) {
            productInfoText = "Your " + manufacturer + " " + getString(R.string.productInfo_partThree);
        } else {
            productInfoText = "Your " + manufacturer + " " + getString(R.string.productInfo_partTwo) + " " + selectedBatteryLevel + "%";
        }
        productInfo.setText(productInfoText);

        batteryLevelSlider.setValue(selectedBatteryLevel);
        batteryLevelSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                selectedBatteryLevel = (int) value;
                waveLoadingView.setProgressValue(selectedBatteryLevel + 1);
                String progressText = selectedBatteryLevel + "%";
                seekBarValueOverlay.setText(progressText);
                vibrateTouch();
                seekBarValueOverlay.startAnimation(showAnimation);
                seekBarValueOverlay.setVisibility(View.VISIBLE);
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putInt("selectedBatteryLevel", selectedBatteryLevel);
                editor.apply();

                String productInfoText;
                if (selectedBatteryLevel > 98) {
                    productInfoText = "Your " + manufacturer + " phone " + getString(R.string.productInfo_partThree);
                } else {
                    productInfoText = "Your " + manufacturer + " phone " + getString(R.string.productInfo_partTwo) + " " + selectedBatteryLevel + "%";
                }
                productInfo.setText(productInfoText);
            }
        });

        batteryLevelSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) {
                scaleSeekBar(slider, 1.2f);
            }

            @Override
            public void onStopTrackingTouch(Slider slider) {
                if (!seekTouch) {
                    seekTouch = true;
                }
                waveLoadingView.setProgressValue(batteryPercent);
                vibrateTouch();
                scaleSeekBar(slider, 1.0f);
                seekBarValueOverlay.startAnimation(hideAnimation);
                seekBarValueOverlay.setVisibility(View.GONE);
            }
        });
        final String PREF_SHARE_DIALOG_SHOWN = "share_dialog_shown";
        if (counter > 9) {
            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            boolean isShareDialogShown = sharedPreferences.getBoolean(PREF_SHARE_DIALOG_SHOWN, false);

            if (!isShareDialogShown) {
                showShareDialog();

                // Mark the dialog as shown in SharedPreferences to avoid showing it again
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(PREF_SHARE_DIALOG_SHOWN, true);
                editor.apply();
            }
        }
        productInfo.setText(productInfoText);
        boolean isServiceRunning = isServiceRunning(BatteryMonitorService.class);
        if (isServiceRunning) {
            startSaving.setVisibility(View.GONE);
            stopSaving.setVisibility(View.VISIBLE);
            animateSwitchToggle();
        } else {
            startSaving.setVisibility(View.VISIBLE);
            stopSaving.setVisibility(View.GONE);
            hideSwitchToggle();
        }
        Chip batterySaveText = findViewById(R.id.batterySaveText);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        batterySaveText.setOnClickListener(view -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Important information about your battery")
                    .setMessage(R.string.disclaimer)
                    .setPositiveButton("About & Licenses", (dialog, which) -> {
                        vibrate();
                        Intent intent = new Intent(MainActivity.this, About.class);
                        startActivity(intent);
                    })
                    .show();
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
                boolean switchedState = sharedPreferences.getBoolean(SWITCH_STATE, false);
                if (switchedState) {
                    buttonToggleGroup.setVisibility(View.VISIBLE);
                    if (selectedTime != 1) {
                        switch (selectedTime) {
                            case 1:
                                // Button for 1 minute
                                oneMin.setChecked(true);
                                break;
                            case 2:
                                // Button for 2 minutes
                                twoMin.setChecked(true);
                                break;
                            case 3:
                                // Button for 3 minutes
                                threeMin.setChecked(true);
                                break;
                        }
                    }
                } else {
                    buttonToggleGroup.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                showIntentErrorDialog();
            }
        });
        stopSaving.setOnClickListener(view -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Disable SafeCharge?")
                    .setMessage("Are you sure you want to turn off SafeCharge service?\n\nBattery monitoring and alerts will be turned off and your device might be prone to overcharging.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Intent serviceIntent = new Intent(MainActivity.this, BatteryMonitorService.class);
                        stopService(serviceIntent);
                        stopSaving.setVisibility(View.GONE);
                        startSaving.setVisibility(View.VISIBLE);
                        hideSwitchToggle();
                        vibrate();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        if (counter > 0) {
            TextView usedTime;
            usedTime = findViewById(R.id.usedTime);
            usedTime.setVisibility(View.VISIBLE);
            if (counter == 1) {
                usedTime.setText("SafeCharged " + counter + " time");
            } else {
                usedTime.setText("SafeCharged " + counter + " times");
            }
        }
    }

    private void handleButtonSelection(MaterialButton selectedButton, int timeValue) {
        resetAllButtons();
        selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_light_primary));
        selectedButton.setTextColor(ContextCompat.getColor(this, R.color.white));
        selectedButton.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.md_theme_light_primary)));

        currentSelectedButton = selectedButton;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SELECTED_TIME_KEY, timeValue);
        editor.apply();

        vibrateTouch();

        // Only show message if auto-dismiss is enabled
        if (switchToggle.isChecked()) {
            String message = getDismissMessage(timeValue);
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private String getDismissMessage(int timeValue) {
        switch (timeValue) {
            case 1:
                return "Alert will dismiss after 1 minute";
            case 2:
                return "Alert will dismiss after 2 minutes";
            case 3:
                return "Alert will dismiss after 3 minutes";
            case 4:
                return "Alert will dismiss after 30 seconds";
            default:
                return "Time selection updated";
        }
    }

    private void resetAllButtons() {
        int defaultTextColor = ContextCompat.getColor(this, R.color.md_theme_light_onSurface);
        int strokeColor = ContextCompat.getColor(this, R.color.md_theme_light_primary);

        thirtySec.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        thirtySec.setTextColor(defaultTextColor);
        thirtySec.setStrokeColor(ColorStateList.valueOf(strokeColor));

        oneMin.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        oneMin.setTextColor(defaultTextColor);
        oneMin.setStrokeColor(ColorStateList.valueOf(strokeColor));

        twoMin.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        twoMin.setTextColor(defaultTextColor);
        twoMin.setStrokeColor(ColorStateList.valueOf(strokeColor));

        threeMin.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        threeMin.setTextColor(defaultTextColor);
        threeMin.setStrokeColor(ColorStateList.valueOf(strokeColor));
    }

    private void showBottomSheet() {
        // Inflate the layout for the Bottom Sheet Dialog
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_new, null);

        // Create BottomSheetDialog
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);
        // Get views from the Bottom Sheet layout
        final TextView textWhatsNew = bottomSheetView.findViewById(R.id.textWhatsNew);
        final MaterialButton btnConfirm = bottomSheetView.findViewById(R.id.btnConfirm);

        // Set button click listener
        btnConfirm.setOnClickListener(v -> {
            // Save user confirmation
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isConfirmed", false);
            editor.apply();

            // Dismiss the Bottom Sheet Dialog
            bottomSheetDialog.dismiss();
        });
        //On Dismiss
        bottomSheetDialog.setOnDismissListener(dialog -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isConfirmed", false);
            editor.apply();
        });
        // Show the Bottom Sheet Dialog
        bottomSheetDialog.show();
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

    private int getCounterFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("counter", 0);
    }

    private void scaleSeekBar(Slider seekBar, float scaleFactor) {
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
        long[] pattern = {11, 0, 11, 0, 20, 2, 23};
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(vibrationEffect);
        } else {
            // For versions lower than Oreo
            vibrator.vibrate(pattern, -1);
        }
    }

    private void vibrateKeys() {
        long[] customPattern = {14, 0, 10, 9, 10, 15, 0, 11};
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
        long[] pattern = {7, 0, 8, 1, 6, 6, 6, 11, 5};
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
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notification_notify)
                .setMessage(R.string.notify_desc)
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, which) -> {
                    vibrate();
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                })
                .show();
    }

    private void enableAutoStart() {
        String brand = Build.BRAND;
        String manufacturer = Build.MANUFACTURER;
        if (brand.equalsIgnoreCase("xiaomi") || brand.equalsIgnoreCase("redmi")) {
            View customView = getLayoutInflater().inflate(R.layout.request_autostart_dialog, null);
            startService();
            vibrateTouch();
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Enable AutoStart")
                    .setMessage("You're using a Xiaomi Phone, enable Autostart to use SafeCharge on your device.")
                    .setCancelable(false)
                    .setView(customView)
                    .setPositiveButton("Continue", (dialog, which) -> {
                        try {
                            Intent intent1 = new Intent();
                            intent1.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                            startActivity(intent1);
                        } catch (Exception e) {
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
            showSamsungAlertDialog();
        } else {
            Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
            startService(serviceIntent);
            startSaving.setVisibility(View.GONE);
            vibrate();
            stopSaving.setVisibility(View.VISIBLE);
            animateSwitchToggle();
        }
    }

    private void showSamsungAlertDialog() {
        View customView = getLayoutInflater().inflate(R.layout.request_autostart_dialog, null);
        startService();
        vibrateTouch();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Enable Background Running")
                .setMessage("You're using a Samsung Phone which stops SafeCharge from running in the background.\n\nFollow the steps next to make SafeCharge run efficiently")
                .setCancelable(false)
                .setView(customView)
                .setPositiveButton("Next", (dialog, which) -> {
                    View customSamsungView = getLayoutInflater().inflate(R.layout.custom_samsung_view, null);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Here are the steps")
                            .setMessage("On the next page\n\nTap on Background usage Limits ->\nNever sleeping apps ->\nTap on +\nAdd SafeCharge to the list")
                            .setCancelable(false)
                            .setView(customSamsungView)
                            .setPositiveButton("Enable", (dialog2, which2) -> {
                                boolean intentLaunched = false;
                                Intent[] AUTO_START_HONOR = {
                                        new Intent().setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"))
                                };
                                for (Intent intent : AUTO_START_HONOR) {
                                    if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                                        try {
                                            startActivity(intent);
                                            break;
                                        } catch (Exception e) {
                                            Log.e("Samsung ki mkb", "Crash Bach Gaya");
                                        }
                                        if (!intentLaunched) {
                                            // Show the error dialog here because none of the Intents were successfully launched
                                            showIntentErrorDialog();
                                        }
                                    }
                                }
                            }).show();
                })
                .show();
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

    private void showShareDialog() {
        View customView = getLayoutInflater().inflate(R.layout.share_app_dial, null);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hey 👋")
                .setMessage("Feel SafeCharge has contributed to a safer and more reliable charging experience for you?\n\nConsider sharing it with friends and family and make them SafeCharge too!")
                .setCancelable(false)
                .setView(customView)
                .setPositiveButton("Share", (dialog, which) -> {
                    String shareMessage = "Hey there! I've been using SafeCharge to ensure safe charging for my phone. It's been a game-changer!\n\nCheck it out: https://play.google.com/store/apps/details?id=com.onesilicondiode.batterywise";
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

                    startActivity(Intent.createChooser(shareIntent, "Share SafeCharge via"));
                })
                .setNegativeButton("No thanks", null)
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
        animateSwitchToggle();
    }

    private void animateSwitchToggle() {
        switchToggle.setVisibility(View.VISIBLE);
        float startY = -100f; // Adjust this value based on your desired starting position

        // Define the animation - falling from a lower position
        ObjectAnimator animator = ObjectAnimator.ofFloat(switchToggle, "translationY", startY, 0);
        animator.setDuration(200); // Duration in milliseconds
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    private void hideSwitchToggle() {
        float endY = -100f; // Adjust this value based on your desired ending position
        // Define the animation - moving upwards before disappearing
        ObjectAnimator animator = ObjectAnimator.ofFloat(switchToggle, "translationY", 0, endY);
        animator.setDuration(110); // Duration in milliseconds
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                switchToggle.setVisibility(View.GONE);
                buttonToggleGroup.setVisibility(View.GONE);
            }
        });
        animator.start();
    }

    private void updateSegmentedButtonVisibility(boolean switchState) {
        if (switchState) {
            // Switch is ON, show the segmented buttons
            buttonToggleGroup.setVisibility(View.VISIBLE);
        } else {
            // Switch is OFF, hide the segmented buttons
            buttonToggleGroup.setVisibility(View.GONE);
        }
    }
}