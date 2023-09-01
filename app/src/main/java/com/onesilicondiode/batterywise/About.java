package com.onesilicondiode.batterywise;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.example.swipebutton_library.SwipeButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.shape.CornerFamily;

public class About extends AppCompatActivity {
    SwipeButton moreAbout;
    TextView versionInfo, privacyPolicy, openSource;
    private Vibrator vibrator;

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
        setContentView(R.layout.activity_about);
        moreAbout = findViewById(R.id.moreAboutSS);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        privacyPolicy = findViewById(R.id.privacyP);
        openSource = findViewById(R.id.openSourceLicense);
        versionInfo = findViewById(R.id.versionName);
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            String version = pInfo.versionName;
            String productInfoText = "SafeCharge Version:\n" + version + "\n1.0.7";
            versionInfo.setText(productInfoText);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        privacyPolicy.setOnClickListener(view -> {
            vibrateOtherButton();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://the-rebooted-coder.github.io/BatteryWise/PrivacyPolicy.txt"));
            startActivity(browserIntent);
        });
        openSource.setOnClickListener(view -> {
            vibrateOtherButton();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/the-rebooted-coder/BatteryWise/blob/main/LICENSE"));
            startActivity(browserIntent);
        });
        moreAbout.setOnActiveListener(() -> {
            vibrate();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/spandn/"));
            startActivity(browserIntent);
        });

        MaterialCardView cardView = findViewById(R.id.initialCard);
        float radius = 30;
        cardView.setShapeAppearanceModel(
                cardView.getShapeAppearanceModel()
                        .toBuilder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                        .setTopRightCorner(CornerFamily.ROUNDED, radius)
                        .setBottomRightCornerSize(0)
                        .setBottomLeftCornerSize(0)
                        .build());

        MaterialCardView cardViews = findViewById(R.id.secondaryCard);
        cardViews.setShapeAppearanceModel(
                cardViews.getShapeAppearanceModel()
                        .toBuilder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                        .setTopRightCorner(CornerFamily.ROUNDED, radius)
                        .setBottomRightCornerSize(0)
                        .setBottomLeftCornerSize(0)
                        .build());
    }

    private void vibrate() {
        long[] pattern = {5, 0, 5, 0, 5, 1, 5, 1, 5, 2, 5, 2, 5, 3, 5, 4, 5, 4, 5, 5, 5, 6, 5, 6, 5, 7, 5, 8, 5, 8, 5, 9, 5, 10, 5, 10, 5, 11, 5, 11, 5, 12, 5, 13, 5, 13, 5, 14, 5, 14, 5, 15, 5, 15, 5, 16, 5, 16, 5, 17, 5, 17, 5, 17, 5, 18, 5, 18, 5, 19, 5, 19, 5, 19, 5, 20, 5, 20, 5, 20, 5, 21, 5, 21, 5, 21, 5, 22, 5, 22, 5, 22, 5, 22, 5, 23, 5, 23, 5, 23, 5, 23, 5, 23, 5, 24, 5, 24, 5, 24, 5, 24, 5, 24, 5, 24, 5, 24, 5, 24, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5};
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(vibrationEffect);
        } else {
            // For versions lower than Oreo
            vibrator.vibrate(pattern, -1);
        }
    }
    private void vibrateOtherButton() {
        long[] pattern = {10, 0, 11, 1, 16, 2, 11, 3, 10, 5, 0, 6, 0, 7, 11, 9, 14, 10, 13, 10, 11, 11, 0, 11, 11, 11, 11, 11, 13, 11, 14, 10, 10, 10, 0, 10, 11, 10, 14, 9, 10, 9, 11, 10, 0, 10, 10, 11, 13, 11, 11, 13, 12, 14, 11, 15, 0, 17, 10, 18, 11, 20, 13, 21, 13, 23, 11, 24, 10, 25, 16};

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(vibrationEffect);
        } else {
            // For versions lower than Oreo
            vibrator.vibrate(pattern, -1);
        }
    }
}