package com.onesilicondiode.batterywise;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.swipebutton_library.OnActiveListener;
import com.example.swipebutton_library.SwipeButton;
import com.gauravbhola.ripplepulsebackground.RipplePulseLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;

public class Startup extends AppCompatActivity {
    TextView appName;
    SwipeButton startApp;
    RipplePulseLayout mRipplePulseLayout;
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
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        DynamicColors.applyToActivityIfAvailable(this);
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        getWindow().setStatusBarColor(getThemeColor(this, android.R.attr.colorPrimaryDark));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        ImageView sharedImageView = findViewById(R.id.sharedImageView);
        appName = findViewById(R.id.appName);
        startApp = findViewById(R.id.startButton);
        mRipplePulseLayout = findViewById(R.id.layout_ripplepulse);
        startPulse();
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                appName.startAnimation(fadeIn);
                fadeIn.setDuration(1200);
                appName.setVisibility(View.VISIBLE);
            }
        }, 2000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                startApp.startAnimation(fadeIn);
                fadeIn.setDuration(1200);
                startApp.setVisibility(View.VISIBLE);
            }
        }, 2500);

        startApp.setOnActiveListener(new OnActiveListener() {
            @Override
            public void onActive() {
               vibrate();
                getWindow().setSharedElementsUseOverlay(true);
                Intent intent = new Intent(Startup.this, Second_Startup.class);
                ActivityOptions options = ActivityOptions
                        .makeSceneTransitionAnimation(Startup.this, sharedImageView, "imageTransition");
                startActivity(intent, options.toBundle());
                finish();
            }
        });

    }

    private void startPulse() {
        mRipplePulseLayout.startRippleAnimation();
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