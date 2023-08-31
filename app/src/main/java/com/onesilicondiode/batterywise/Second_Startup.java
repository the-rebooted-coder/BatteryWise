package com.onesilicondiode.batterywise;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.color.DynamicColors;

public class Second_Startup extends AppCompatActivity {

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
        ImageView sharedImageView = findViewById(R.id.sharedImageView);
        sharedImageView.setTransitionName("imageTransition");
    }
}