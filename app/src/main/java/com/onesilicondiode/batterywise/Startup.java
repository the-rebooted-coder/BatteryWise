package com.onesilicondiode.batterywise;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.swipebutton_library.SwipeButton;
import com.gauravbhola.ripplepulsebackground.RipplePulseLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;

public class Startup extends AppCompatActivity {
    TextView appName;
    SwipeButton startApp;
    RipplePulseLayout mRipplePulseLayout;

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
    }

    private void startPulse() {
        mRipplePulseLayout.startRippleAnimation();
    }
}