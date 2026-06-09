package com.onesilicondiode.batterywise;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
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
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.example.swipebutton_library.SwipeButton;

import me.itangqi.waveloadingview.WaveLoadingView;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_APP_UPDATE = 123;
    private static final String PREF_SEEK_TOUCH = "seekTouch";
    private static final String USER_STARTED_KEY = "userStarted";
    private static final String SELECTED_TIME_KEY = "selected_time";
    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "SafeCharge will not work, restart app to grant permission", Toast.LENGTH_LONG).show();
                }
            });

    // Liquid Canvas UI references
    private MaterialButton startSaving;
    private com.google.android.material.textview.MaterialTextView heroBatteryNumber;
    private com.google.android.material.textview.MaterialTextView heroSubLabel;
    private com.google.android.material.textview.MaterialTextView settingsGhostLink;
    private com.google.android.material.textview.MaterialTextView topBarTitle;
    private com.google.android.material.card.MaterialCardView statusPill;
    private com.google.android.material.textview.MaterialTextView statusPillText;

    // Settings bottom sheet references
    private MaterialButton sheetBtn30s, sheetBtn1m, sheetBtn2m, sheetBtn3m;
    private MaterialButton currentSelectedButton;
    private com.google.android.material.materialswitch.MaterialSwitch sheetSwitchToggle;
    private android.widget.HorizontalScrollView sheetTimeChipsContainer;

    int selectedBatteryLevel = 85;
    private WaveLoadingView waveLoadingView;
    private FirebaseAnalytics mFirebaseAnalytics;
    private boolean seekTouch = false;
    private Vibrator vibrator;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String SWITCH_STATE = "switchState";
    private AppUpdateManager appUpdateManager;
    private ReviewManager reviewManager;
    private SharedPreferences sharedPreferences;

    private static final String WAVE_COLOR_GREY = "#B0B0B0";
    private static final String WAVE_COLOR_GREEN = "#006C49";
    private static final String WAVE_COLOR_YELLOW = "#F6D3A1";
    private static final String WAVE_COLOR_ORANGE = "#E4B284";
    private static final String WAVE_COLOR_RED = "#FFB4AB";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        DynamicColors.applyToActivityIfAvailable(this);
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        reviewManager = ReviewManagerFactory.create(this);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // ── Wire Liquid Canvas views ──
        waveLoadingView = findViewById(R.id.waveLoadingView);
        heroBatteryNumber = findViewById(R.id.heroBatteryNumber);
        heroSubLabel = findViewById(R.id.heroSubLabel);
        statusPill = findViewById(R.id.statusPill);
        statusPillText = findViewById(R.id.statusPillText);
        settingsGhostLink = findViewById(R.id.settingsGhostLink);
        topBarTitle = findViewById(R.id.topBarTitle);
        startSaving = findViewById(R.id.saveBatteryBtn);

        // ── Dynamic wave border colour ──
        int primaryColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorPrimary);
        waveLoadingView.setBorderColor(primaryColor);

        // ── Battery info ──
        BatteryManager batteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);
        int batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        // Hero battery number
        heroBatteryNumber.setText(batteryPercent + "%");
        waveLoadingView.setProgressValue(batteryPercent);
        updateUIColors(batteryPercent);

        // Threshold sub-label
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        seekTouch = prefs.getBoolean(PREF_SEEK_TOUCH, false);
        selectedBatteryLevel = prefs.getInt("selectedBatteryLevel", 85);

        // ── Service state ──
        appUpdateManager = AppUpdateManagerFactory.create(this);
        boolean isServiceRunning = isServiceRunning(BatteryMonitorService.class);
        setWaveColor(isServiceRunning, batteryPercent);

        if (isServiceRunning) {
            Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            showActiveState();
        } else {
            showIdleState();
        }

        // ── Settings ghost link → opens settings bottom sheet ──
        settingsGhostLink.setOnClickListener(v -> {
            vibrateTouch();
            showSettingsBottomSheet();
        });

        // ── Battery Lab ──
        MaterialButton btnLab = findViewById(R.id.btn_lab);
        btnLab.setOnClickListener(v -> {
            vibrateTouch();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("lab_visited", true);
            editor.apply();
            startActivity(new Intent(this, BatteryLab.class));
        });
        if (!sharedPreferences.getBoolean("lab_visited", false)) {
            startLabButtonGlow(btnLab);
        }

        // ── ChronoCell (Battery Time Machine) ──
        MaterialButton btnChrono = findViewById(R.id.btn_chronocell);
        btnChrono.setOnClickListener(v -> {
            vibrateTouch();
            startActivity(new Intent(this, ChronoCellActivity.class));
        });

        // ── Status pill → also opens settings ──
        statusPill.setOnClickListener(v -> {
            vibrateTouch();
            showSettingsBottomSheet();
        });

        // ── Enable button ──
        startSaving.setOnClickListener(view -> {
            try {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(USER_STARTED_KEY, true);
                editor.apply();
                enableAutoStart();
                int currentBatteryPercent = getCurrentBatteryPercent();
                setWaveColor(true, currentBatteryPercent);
                waveLoadingView.setProgressValue(currentBatteryPercent);
                updateUIColors(currentBatteryPercent);
            } catch (Exception e) {
                showIntentErrorDialog();
            }
        });



        // ── App update check ──
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.FLEXIBLE, this, REQUEST_CODE_APP_UPDATE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // ── "What's New" bottom sheet (ChronoCell Update) ──
        if (!sharedPreferences.getBoolean("isChronoCellUpdateConfirmed", false)) {
            showBottomSheet();
        }

        // ── Share dialog (after 10 uses) ──
        int counter = getCounterFromSharedPreferences();
        if (counter > 9) {
            SharedPreferences sp = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            if (!sp.getBoolean("share_dialog_shown", false)) {
                showShareDialog();
                sp.edit().putBoolean("share_dialog_shown", true).apply();
            }
        }

        // ── Firebase ──
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int batteryPercent = getCurrentBatteryPercent();
        heroBatteryNumber.setText(batteryPercent + "%");
        waveLoadingView.setProgressValue(batteryPercent);
        boolean isServiceRunning = isServiceRunning(BatteryMonitorService.class);
        setWaveColor(isServiceRunning, batteryPercent);
        updateUIColors(batteryPercent);
    }

    // ─────────────────────────────────────────────────────────────────
    // LIQUID CANVAS STATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    private void showActiveState() {
        startSaving.setVisibility(View.GONE);
        // Animate status pill in
        statusPill.setVisibility(View.VISIBLE);
        statusPill.setAlpha(0f);
        statusPill.setTranslationY(20f);
        statusPill.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        updateStatusPillText();
        // Animate settings ghost in
        settingsGhostLink.setAlpha(0f);
        settingsGhostLink.animate().alpha(1f).setDuration(400).setStartDelay(200).start();
    }

    private void showIdleState() {
        startSaving.setVisibility(View.VISIBLE);
        statusPill.setVisibility(View.INVISIBLE);
        statusPill.setAlpha(0f);
        settingsGhostLink.setAlpha(0.6f);
    }

    private void hideActiveState() {
        startSaving.setVisibility(View.VISIBLE);
        statusPill.animate()
                .alpha(0f)
                .translationY(20f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> statusPill.setVisibility(View.INVISIBLE))
                .start();
    }

    private void updateStatusPillText() {
        statusPillText.setText("● Monitoring  ·  Alert at " + selectedBatteryLevel + "%");
    }



    // ─────────────────────────────────────────────────────────────────
    // SETTINGS BOTTOM SHEET
    // ─────────────────────────────────────────────────────────────────

    private void showSettingsBottomSheet() {
        android.view.View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_settings, null);
        com.google.android.material.bottomsheet.BottomSheetDialog sheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        sheetDialog.setContentView(sheetView);

        // Threshold section
        com.google.android.material.textview.MaterialTextView sheetThresholdValue =
                sheetView.findViewById(R.id.sheet_threshold_value);
        com.google.android.material.slider.Slider sheetSlider =
                sheetView.findViewById(R.id.sheet_battery_slider);
        sheetSlider.setValue(selectedBatteryLevel);
        sheetThresholdValue.setText(selectedBatteryLevel + "%");

        sheetSlider.addOnChangeListener((slider, value, fromUser) -> {
            selectedBatteryLevel = (int) value;
            sheetThresholdValue.setText(selectedBatteryLevel + "%");
            vibrateTouch();
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putInt("selectedBatteryLevel", selectedBatteryLevel);
            editor.apply();
            updateStatusPillText();
            if (isServiceRunning(BatteryMonitorService.class)) {
                setWaveColor(true, getCurrentBatteryPercent());
            }
        });

        // Auto-dismiss toggle
        sheetSwitchToggle = sheetView.findViewById(R.id.sheet_switch_toggle);
        sheetTimeChipsContainer = sheetView.findViewById(R.id.sheet_time_chips_container);
        sheetBtn30s = sheetView.findViewById(R.id.sheet_btn_30s);
        sheetBtn1m = sheetView.findViewById(R.id.sheet_btn_1m);
        sheetBtn2m = sheetView.findViewById(R.id.sheet_btn_2m);
        sheetBtn3m = sheetView.findViewById(R.id.sheet_btn_3m);

        boolean switchState = sharedPreferences.getBoolean(SWITCH_STATE, false);
        sheetSwitchToggle.setChecked(switchState);
        sheetTimeChipsContainer.setVisibility(switchState ? View.VISIBLE : View.GONE);

        int selectedTime = sharedPreferences.getInt(SELECTED_TIME_KEY, 2);
        switch (selectedTime) {
            case 1: sheetHandleButtonSelection(sheetBtn1m, 1); break;
            case 2: sheetHandleButtonSelection(sheetBtn2m, 2); break;
            case 3: sheetHandleButtonSelection(sheetBtn3m, 3); break;
            case 4: sheetHandleButtonSelection(sheetBtn30s, 4); break;
        }

        sheetSwitchToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sheetTimeChipsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SWITCH_STATE, isChecked);
            editor.apply();
            vibrateTouch();
        });

        sheetBtn30s.setOnClickListener(v -> sheetHandleButtonSelection(sheetBtn30s, 4));
        sheetBtn1m.setOnClickListener(v -> sheetHandleButtonSelection(sheetBtn1m, 1));
        sheetBtn2m.setOnClickListener(v -> sheetHandleButtonSelection(sheetBtn2m, 2));
        sheetBtn3m.setOnClickListener(v -> sheetHandleButtonSelection(sheetBtn3m, 3));

        // Utility rows
        View daydreamRow = sheetView.findViewById(R.id.sheet_daydream_row);
        daydreamRow.setOnClickListener(v -> {
            vibrateTouch();
            sheetDialog.dismiss();
            showDayDreamBottomSheet();
        });

        View infoRow = sheetView.findViewById(R.id.sheet_info_row);
        infoRow.setOnClickListener(v -> {
            vibrateTouch();
            sheetDialog.dismiss();
            showInfoBottomSheet();
        });

        // Stop Monitoring Button
        MaterialButton sheetStopMonitoringBtn = sheetView.findViewById(R.id.sheet_stop_monitoring_btn);
        if (isServiceRunning(BatteryMonitorService.class)) {
            sheetStopMonitoringBtn.setVisibility(View.VISIBLE);
            sheetStopMonitoringBtn.setOnClickListener(v -> {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Disable SafeCharge?")
                        .setMessage("Battery monitoring and alerts will be turned off.")
                        .setPositiveButton("Yes, Disable", (dialog, which) -> {
                            Intent serviceIntent = new Intent(MainActivity.this, BatteryMonitorService.class);
                            stopService(serviceIntent);
                            vibrate();
                            waveLoadingView.setWaveColor(Color.parseColor(WAVE_COLOR_GREY));
                            hideActiveState();
                            sheetDialog.dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else {
            sheetStopMonitoringBtn.setVisibility(View.GONE);
        }

        sheetDialog.show();
    }

    private void showDayDreamBottomSheet() {
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_daydream, null);
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        sheetDialog.setContentView(sheetView);

        // Header animation
        View icon = sheetView.findViewById(R.id.daydream_icon);
        icon.setAlpha(0f);
        icon.setTranslationY(40f);
        icon.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .setStartDelay(200)
                .start();

        // Zen Mode setup
        MaterialSwitch zenSwitch = sheetView.findViewById(R.id.daydream_zen_switch);
        SharedPreferences ddPrefs = getSharedPreferences(SafeChargeSettingsActivity.PREFS_NAME, MODE_PRIVATE);
        boolean zenEnabled = ddPrefs.getBoolean(SafeChargeSettingsActivity.ZEN_MODE_KEY, false);
        zenSwitch.setChecked(zenEnabled);

        zenSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vibrateTouch();
            ddPrefs.edit().putBoolean(SafeChargeSettingsActivity.ZEN_MODE_KEY, isChecked).apply();
        });

        // Preview button
        MaterialButton btnPreview = sheetView.findViewById(R.id.btn_preview_daydream);
        btnPreview.setOnClickListener(v -> {
            vibrateTouch();
            sheetDialog.dismiss();
            try {
                startActivity(new Intent(this, ScreenSaverActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Could not open DayDream.", Toast.LENGTH_SHORT).show();
            }
        });

        // System Settings button
        MaterialButton btnSystem = sheetView.findViewById(R.id.btn_system_screensaver);
        btnSystem.setOnClickListener(v -> {
            vibrateTouch();
            sheetDialog.dismiss();
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_DREAM_SETTINGS));
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open screensaver settings.", Toast.LENGTH_LONG).show();
            }
        });

        sheetDialog.show();
    }

    private void showInfoBottomSheet() {
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_info, null);
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        sheetDialog.setContentView(sheetView);

        // Header animation
        View icon = sheetView.findViewById(R.id.info_icon);
        icon.setAlpha(0f);
        icon.setScaleX(0.4f);
        icon.setScaleY(0.4f);
        icon.animate()
                .alpha(1f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    icon.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .setInterpolator(new android.view.animation.AccelerateInterpolator())
                            .start();
                })
                .setStartDelay(100)
                .start();

        // About Developer button
        MaterialButton btnAbout = sheetView.findViewById(R.id.btn_about_dev);
        btnAbout.setOnClickListener(v -> {
            vibrateTouch();
            sheetDialog.dismiss();
            startActivity(new Intent(MainActivity.this, About.class));
        });

        // Close button
        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close_info);
        btnClose.setOnClickListener(v -> {
            vibrateTouch();
            sheetDialog.dismiss();
        });

        sheetDialog.show();
    }

    private void sheetHandleButtonSelection(MaterialButton selectedButton, int timeValue) {
        sheetResetAllButtons();
        int primaryColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorPrimary);
        int onPrimaryColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorOnPrimary);
        selectedButton.setBackgroundColor(primaryColor);
        selectedButton.setTextColor(onPrimaryColor);
        selectedButton.setStrokeColor(android.content.res.ColorStateList.valueOf(primaryColor));
        currentSelectedButton = selectedButton;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SELECTED_TIME_KEY, timeValue);
        editor.apply();
        vibrateTouch();
    }

    private void sheetResetAllButtons() {
        if (sheetBtn30s == null) return;
        int defaultTextColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorOnSurface);
        int strokeColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorOutline);
        for (MaterialButton btn : new MaterialButton[]{sheetBtn30s, sheetBtn1m, sheetBtn2m, sheetBtn3m}) {
            btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            btn.setTextColor(defaultTextColor);
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(strokeColor));
        }
    }

    private int getCurrentBatteryPercent() {
        BatteryManager batteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }



    private boolean isServiceRunning(Class<?> serviceClass) {
        android.content.SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean("serviceRunning", false);
    }

    private void showBottomSheet() {
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_new, null);
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);
        final TextView textWhatsNew = bottomSheetView.findViewById(R.id.textWhatsNew);
        final MaterialButton btnConfirm = bottomSheetView.findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isChronoCellUpdateConfirmed", true);
            editor.apply();
            bottomSheetDialog.dismiss();
        });
        bottomSheetDialog.setOnDismissListener(dialog -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isChronoCellUpdateConfirmed", true);
            editor.apply();
        });
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

    private void startLabButtonGlow(View btn) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(btn, "scaleX", 1f, 1.15f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(btn, "scaleY", 1f, 1.15f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(btn, "alpha", 1f, 0.7f);

        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        alpha.setDuration(1000);

        scaleX.setRepeatCount(3); // Pulse twice (Forward-Reverse = 1, so 4 states total)
        scaleY.setRepeatCount(3);
        alpha.setRepeatCount(3);

        scaleX.setRepeatMode(ObjectAnimator.REVERSE);
        scaleY.setRepeatMode(ObjectAnimator.REVERSE);
        alpha.setRepeatMode(ObjectAnimator.REVERSE);

        AnimatorSet breathing = new AnimatorSet();
        breathing.playTogether(scaleX, scaleY, alpha);
        breathing.start();
    }

    private void updateUIColors(int batteryPercent) {
        int onSurfaceColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorOnSurface);
        int onSurfaceVariantColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
        MaterialButton btnLab = findViewById(R.id.btn_lab);
        MaterialButton btnChrono = findViewById(R.id.btn_chronocell);

        // App Name (Top Bar) - threshold around 92% as it's at the very top
        if (batteryPercent >= 92) {
            topBarTitle.setTextColor(Color.WHITE);
            btnLab.setIconTint(ColorStateList.valueOf(Color.WHITE));
            btnChrono.setIconTint(ColorStateList.valueOf(Color.WHITE));
        } else {
            topBarTitle.setTextColor(onSurfaceColor);
            btnLab.setIconTint(ColorStateList.valueOf(onSurfaceVariantColor));
            btnChrono.setIconTint(ColorStateList.valueOf(onSurfaceVariantColor));
        }

        // Hero Battery Number - threshold around 52% (center is 50, wave crests higher)
        if (batteryPercent >= 52) {
            heroBatteryNumber.setTextColor(Color.WHITE);
            heroSubLabel.setTextColor(Color.parseColor("#B3FFFFFF"));
        } else {
            heroBatteryNumber.setTextColor(onSurfaceColor);
            heroSubLabel.setTextColor(onSurfaceVariantColor);
        }

        // Ghost settings link - at the bottom (~10-15% height)
        if (batteryPercent >= 15) {
            settingsGhostLink.setTextColor(Color.parseColor("#66FFFFFF"));
        } else {
            settingsGhostLink.setTextColor(onSurfaceVariantColor);
        }
    }

    private void setWaveColor(boolean serviceEnabled, int batteryPercent) {
        if (batteryPercent > 55) {
            waveLoadingView.setWaveColor(Color.parseColor(WAVE_COLOR_GREEN));
        } else if (batteryPercent > 40 && batteryPercent <= 55) {
            waveLoadingView.setWaveColor(Color.parseColor(WAVE_COLOR_YELLOW));
        } else if (batteryPercent > 20 && batteryPercent <= 40) {
            waveLoadingView.setWaveColor(Color.parseColor(WAVE_COLOR_ORANGE));
        } else if (batteryPercent > 1 && batteryPercent <= 20) {
            waveLoadingView.setWaveColor(Color.parseColor(WAVE_COLOR_RED));
        } else {
            waveLoadingView.setWaveColor(Color.parseColor(WAVE_COLOR_GREY));
        }
    }

    private int getCounterFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("counter", 0);
    }


    private void vibrate() {
        HapticUtils.playCustomVibration(this, new long[]{11, 0, 11, 0, 20, 2, 23});
    }

    private void vibrateKeys() {
        HapticUtils.playCustomVibration(this, new long[]{14, 0, 10, 9, 10, 15, 0, 11});
    }

    private void vibrateTouch() {
        HapticUtils.playCustomVibration(this, new long[]{7, 0, 8, 1, 6, 6, 6, 11, 5});
    }

    private void checkNotificationPermission() {
        String permission = Manifest.permission.POST_NOTIFICATIONS;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
        } else if (shouldShowRequestPermissionRationale(permission)) {
            showPermissionRationaleDialog();
        } else {
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
        AutostartUtils.enableAutoStart(this, getLayoutInflater(), new AutostartUtils.AutostartCallback() {
            @Override
            public void onServiceStarted() {
                startService();
                vibrateTouch();
            }

            @Override
            public void onIntentError() {
                showIntentErrorDialog();
            }
        });
    }

    private void showShareDialog() {
        View customView = getLayoutInflater().inflate(R.layout.share_app_dial, null);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Cannot auto-open Settings 😔")
                .setMessage("SafeCharge tried opening app settings, to help you enable AutoStart but failed to do so.\n\nPlease open App Info Manually and allow SafeCharge to AutoStart/Un-restrict Battery Usage.")
                .setPositiveButton("OK", (dialog, which) -> {
                    vibrateKeys();
                })
                .show();
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
        startService(serviceIntent);
        vibrate();
        showActiveState();
    }



}