package com.onesilicondiode.batterywise;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;

public class About extends AppCompatActivity {
    private TextView versionInfo;
    private View privacyPolicy;
    private View openSource;
    private View devRow;
    private ImageView osdLogo;
    private ImageView aboutAppIcon;
    private MaterialCardView missionCard;
    private MaterialCardView devCard;

    // Predictive back callback
    private OnBackInvokedCallback predictiveBackCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        DynamicColors.applyToActivityIfAvailable(this);
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Bind Views
        privacyPolicy = findViewById(R.id.privacyP);
        openSource = findViewById(R.id.openSourceLicense);
        versionInfo = findViewById(R.id.versionName);
        osdLogo = findViewById(R.id.osdLogo);
        devRow = findViewById(R.id.dev_row);
        aboutAppIcon = findViewById(R.id.about_app_icon);
        missionCard = findViewById(R.id.mission_card);
        devCard = findViewById(R.id.dev_card);

        setupLogic();
        startEntranceAnimations();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            predictiveBackCallback = () -> finish();
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    predictiveBackCallback
            );
        }
    }

    private void setupLogic() {
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            versionInfo.setText("Version " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        osdLogo.setOnClickListener(view -> {
            vibrateOSD();
            Toast.makeText(this, "SafeCharge: Built for Eternity (;", Toast.LENGTH_SHORT).show();
        });

        devRow.setOnClickListener(v -> openUrl("https://the-rebooted-coder.github.io/Digital-TeesShirt/"));
        privacyPolicy.setOnClickListener(v -> openUrl("https://the-rebooted-coder.github.io/BatteryWise/PrivacyPolicy.txt"));
        openSource.setOnClickListener(v -> openUrl("https://github.com/the-rebooted-coder/BatteryWise/blob/main/LICENSE"));
    }

    private void openUrl(String url) {
        vibrateOtherButton();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void startEntranceAnimations() {
        // Initial state
        aboutAppIcon.setAlpha(0f);
        aboutAppIcon.setTranslationY(100f);
        aboutAppIcon.setScaleX(0.8f);
        aboutAppIcon.setScaleY(0.8f);

        missionCard.setAlpha(0f);
        missionCard.setTranslationY(80f);
        
        devCard.setAlpha(0f);
        devCard.setTranslationY(80f);

        // Core Entrance
        aboutAppIcon.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new AnticipateOvershootInterpolator(1.0f))
                .setStartDelay(200)
                .start();

        animateStaggered(missionCard, 400);
        animateStaggered(devCard, 550);
    }

    private void animateStaggered(View v, long delay) {
        v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(new DecelerateInterpolator())
                .setStartDelay(delay)
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && predictiveBackCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(predictiveBackCallback);
        }
    }

    private void vibrateOSD() {
        HapticUtils.playCustomVibration(this, new long[]{17, 4, 14, 17, 0, 22, 21, 8, 22, 0, 18, 0, 16});
    }

    private void vibrateOtherButton() {
        HapticUtils.playCustomVibration(this, new long[]{10, 0, 11, 1, 16, 2, 11, 3});
    }
}
