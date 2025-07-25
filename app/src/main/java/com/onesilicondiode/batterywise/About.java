package com.onesilicondiode.batterywise;

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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.swipebutton_library.SwipeButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.shape.CornerFamily;

public class About extends AppCompatActivity {
    SwipeButton moreAbout;
    TextView versionInfo, privacyPolicy, openSource;
    private Vibrator vibrator;
    ImageView osdLogo;

    // Predictive back callback for Android 13+ (U+)
    private OnBackInvokedCallback predictiveBackCallback;

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
        osdLogo = findViewById(R.id.osdLogo);
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            String version = pInfo.versionName;
            String productInfoText = version + "\n35.0";
            versionInfo.setText(productInfoText);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        osdLogo.setOnClickListener(view -> {
            vibrateOSD();
            Toast.makeText(About.this, "App developed by OneSiliconDiode (;", Toast.LENGTH_SHORT).show();
        });
        privacyPolicy.setOnClickListener(view -> {
            vibrateOtherButton();
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://the-rebooted-coder.github.io/BatteryWise/PrivacyPolicy.txt"));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Cannot Open Browser", Toast.LENGTH_LONG).show();
            }
        });
        openSource.setOnClickListener(view -> {
            vibrateOtherButton();
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/the-rebooted-coder/BatteryWise/blob/main/LICENSE"));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Cannot Open Browser", Toast.LENGTH_LONG).show();
            }
        });
        moreAbout.setOnActiveListener(() -> {
            vibrate();
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://the-rebooted-coder.github.io/Digital-TeesShirt/"));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Google me up ;)", Toast.LENGTH_LONG).show();
            }
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

        // Predictive Back Navigation (Android 15+/U+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 15+
            predictiveBackCallback = () -> finish();
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

    private void vibrate() {
        long[] pattern = {5, 0, 5, 0, 5, 1, 5, 1, 5, 2, 5, 2, 5, 3, 5, 4, 5, 4, 5};
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(vibrationEffect);
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    private void vibrateOSD() {
        long[] pattern = {17, 4, 14, 17, 0, 22, 21, 8, 22, 0, 18, 0, 16};
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(vibrationEffect);
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    private void vibrateOtherButton() {
        long[] pattern = {10, 0, 11, 1, 16, 2, 11, 3};
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(vibrationEffect);
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }
}