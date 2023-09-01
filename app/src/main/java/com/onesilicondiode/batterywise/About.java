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
    private Vibrator vibrator;
    SwipeButton moreAbout;
    TextView versionInfo, privacyPolicy, openSource;

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
            String productInfoText = "SafeCharge Version:\n"+version+"\n1.0.6";
            versionInfo.setText(productInfoText);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        privacyPolicy.setOnClickListener(view -> {
            vibrate();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://the-rebooted-coder.github.io/BatteryWise/PrivacyPolicy.txt"));
            startActivity(browserIntent);
        });
        openSource.setOnClickListener(view -> {
            vibrate();
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
}