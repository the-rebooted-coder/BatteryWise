package com.onesilicondiode.batterywise;

import static com.onesilicondiode.batterywise.Constants.PREFS_NAME;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;

public class Second_Startup extends AppCompatActivity {
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FIRST_LAUNCH_KEY = "firstLaunch";
    MaterialButton finalLaunch;
    private TextView moreProductInfo, learnMore;
    private Vibrator vibrator;


    public static int getThemeColor(Context context, int colorResId) {
        TypedValue typedValue = new TypedValue();
        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, new int[]{colorResId});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Specify a custom return transition animation
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finishAfterTransition();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        DynamicColors.applyToActivityIfAvailable(this);
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        getWindow().setStatusBarColor(getThemeColor(this, android.R.attr.colorPrimaryDark));
        super.onCreate(savedInstanceState);
        getWindow().setEnterTransition(null);
        setContentView(R.layout.activity_second_startup);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        moreProductInfo = findViewById(R.id.moreProductInfo);
        finalLaunch = findViewById(R.id.finalLaunch);
        finalLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Second_Startup.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                vibrate();
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(FIRST_LAUNCH_KEY, false);
                editor.apply();
                finish();
            }
        });
        learnMore = findViewById(R.id.learnMore);
        learnMore.setOnClickListener(v -> {
            // Fade in the "moreProductInfo" TextView
            fadeInTextView(moreProductInfo);
        });
        ImageView sharedImageView = findViewById(R.id.sharedImageView);
        sharedImageView.setTransitionName("imageTransition");
        TextView wavingHandTextView = findViewById(R.id.wavingHandTextView);
        Animation waveAnimation = AnimationUtils.loadAnimation(this, R.anim.emoji_wave);
        wavingHandTextView.startAnimation(waveAnimation);
    }

    private void fadeInTextView(TextView textView) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(1000); // Set the duration of the fade-in animation in milliseconds
        textView.setVisibility(View.VISIBLE);
        learnMore.setVisibility(View.GONE);
        textView.startAnimation(fadeIn);
    }

    private void vibrate() {
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