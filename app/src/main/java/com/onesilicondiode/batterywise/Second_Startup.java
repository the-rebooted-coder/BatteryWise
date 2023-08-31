package com.onesilicondiode.batterywise;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
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

import com.google.android.material.color.DynamicColors;

public class Second_Startup extends AppCompatActivity {
    private TextView moreProductInfo, learnMore;


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
        moreProductInfo = findViewById(R.id.moreProductInfo);
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
}