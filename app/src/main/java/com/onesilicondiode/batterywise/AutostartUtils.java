package com.onesilicondiode.batterywise;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.view.LayoutInflater;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AutostartUtils {

    public interface AutostartCallback {
        void onServiceStarted();
        void onIntentError();
    }

    public static void enableAutoStart(Context context, LayoutInflater inflater, AutostartCallback callback) {
        String brand = Build.BRAND;
        String manufacturer = Build.MANUFACTURER;

        if (brand.equalsIgnoreCase("xiaomi") || brand.equalsIgnoreCase("redmi")) {
            showSingleStepDialog(context, inflater, callback, "Xiaomi", "com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
        } else if (brand.equalsIgnoreCase("Letv")) {
            showSingleStepDialog(context, inflater, callback, "Letv", "com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity");
        } else if (brand.equalsIgnoreCase("Honor")) {
            showHonorDialog(context, inflater, callback);
        } else if (manufacturer.equalsIgnoreCase("oppo")) {
            showOppoDialog(context, inflater, callback);
        } else if (manufacturer.contains("vivo")) {
            showVivoDialog(context, inflater, callback);
        } else if (manufacturer.contains("asus")) {
            showSingleStepDialog(context, inflater, callback, "Asus", "com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity");
        } else if (manufacturer.contains("samsung")) {
            showSamsungDialog(context, inflater, callback);
        } else {
            callback.onServiceStarted();
        }
    }

    private static void showSingleStepDialog(Context context, LayoutInflater inflater, AutostartCallback callback, String brandName, String pkg, String cls) {
        View customView = inflater.inflate(R.layout.request_autostart_dialog, null);
        callback.onServiceStarted();
        new MaterialAlertDialogBuilder(context)
                .setTitle("Enable AutoStart")
                .setMessage("You're using a " + brandName + " Phone, AutoStart is required to enable SafeCharge on your device.")
                .setCancelable(false)
                .setView(customView)
                .setPositiveButton("Continue", (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(pkg, cls));
                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {
                        callback.onIntentError();
                    }
                })
                .show();
    }

    private static void showHonorDialog(Context context, LayoutInflater inflater, AutostartCallback callback) {
        View customView = inflater.inflate(R.layout.request_autostart_dialog, null);
        callback.onServiceStarted();
        new MaterialAlertDialogBuilder(context)
                .setTitle("Enable AutoStart")
                .setMessage("You're using a Huawei Phone, autostart is required to enable SafeCharge on your device.")
                .setCancelable(false)
                .setView(customView)
                .setPositiveButton("Continue", (dialog, which) -> {
                    Intent[] intents = {
                            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
                            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"))
                    };
                    launchAnyIntent(context, intents, callback);
                })
                .show();
    }

    private static void showOppoDialog(Context context, LayoutInflater inflater, AutostartCallback callback) {
        View customView = inflater.inflate(R.layout.request_autostart_dialog, null);
        callback.onServiceStarted();
        new MaterialAlertDialogBuilder(context)
                .setTitle("Allow SafeCharge to run in background")
                .setMessage("To ensure SafeCharge works correctly on your Oppo device, please:\n\n1. Enable autostart for SafeCharge\n2. Allow background activity/unrestricted battery usage\n\nTap Start to be guided to the correct setting screens.")
                .setCancelable(false)
                .setView(customView)
                .setPositiveButton("Start", (dialog, which) -> showOppoStep1(context, callback))
                .show();
    }

    private static void showOppoStep1(Context context, AutostartCallback callback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Step 1: Enable Autostart")
                .setMessage("Tap 'Open Settings'. In the next screen:\n• Locate SafeCharge in the list\n• Enable autostart for SafeCharge\n\nAfter finishing, return for step 2.")
                .setCancelable(false)
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent[] intents = {
                            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
                            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startup.StartupAppListActivity")),
                            new Intent().setComponent(new ComponentName("com.coloros.safe", "com.coloros.safe.permission.startup.StartupAppListActivity")),
                            new Intent().setComponent(new ComponentName("com.coloros.safe", "com.coloros.safe.permission.startupapp.StartupAppListActivity"))
                    };
                    launchAnyIntent(context, intents, callback);
                    showStep2(context, callback);
                })
                .setNegativeButton("Skip", (dialog, which) -> showStep2(context, callback))
                .show();
    }

    private static void showVivoDialog(Context context, LayoutInflater inflater, AutostartCallback callback) {
        callback.onServiceStarted();
        new MaterialAlertDialogBuilder(context)
                .setTitle("Step 1: Add to Whitelist/Autostart")
                .setMessage("Tap 'Open Settings'. In the next screen:\n• Find SafeCharge in the list\n• Enable autostart/whitelist for SafeCharge\n\nAfter finishing, return for step 2.")
                .setCancelable(false)
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent[] intents = {
                            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                            new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
                            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"))
                    };
                    launchAnyIntent(context, intents, callback);
                    showStep2(context, callback);
                })
                .setNegativeButton("Skip", (dialog, which) -> showStep2(context, callback))
                .show();
    }

    private static void showSamsungDialog(Context context, LayoutInflater inflater, AutostartCallback callback) {
        View customView = inflater.inflate(R.layout.request_autostart_dialog, null);
        callback.onServiceStarted();
        new MaterialAlertDialogBuilder(context)
                .setTitle("Allow SafeCharge to run in background")
                .setMessage("To ensure SafeCharge works correctly on your Samsung device, please:\n\n1. Disable battery optimization for SafeCharge\n2. Allow background activity\n\nYou will be guided to the right settings screens. Please follow the steps!")
                .setCancelable(false)
                .setView(customView)
                .setPositiveButton("Start", (dialog, which) -> showSamsungStep1(context, callback))
                .show();
    }

    private static void showSamsungStep1(Context context, AutostartCallback callback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Step 1: Disable Battery Optimization")
                .setMessage("• Find SafeCharge in the list\n• Select 'Unrestricted' or 'Allow'\n\nAfter finishing, return to the app for step 2.")
                .setCancelable(false)
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        context.startActivity(intent);
                    } catch (Exception e) {
                        callback.onIntentError();
                    }
                    showSamsungStep2(context, callback);
                })
                .show();
    }

    private static void showSamsungStep2(Context context, AutostartCallback callback) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View customSamsungView = inflater.inflate(R.layout.custom_samsung_view, null);
        new MaterialAlertDialogBuilder(context)
                .setTitle("Step 2: Allow Background Activity")
                .setMessage("• Find 'Background usage limits'\n• Click 'Never sleeping apps'\n• Add 'SafeCharge' to the list\n• Ignore if 'SafeCharge' is not present in the list\n")
                .setCancelable(false)
                .setView(customSamsungView)
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"));
                        context.startActivity(intent);
                    } catch (Exception e) {
                        launchAppDetails(context, callback);
                    }
                })
                .setNegativeButton("Done", null)
                .show();
    }

    private static void showStep2(Context context, AutostartCallback callback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Step 2: Allow Background Activity")
                .setMessage("Tap 'Open Settings'. In the next screen:\n• Find SafeCharge\n• Allow 'Background activity', 'Run in background', or set battery usage to 'Unrestricted'\n\nThis will help SafeCharge work reliably even when your screen is off.")
                .setCancelable(false)
                .setPositiveButton("Open Settings", (dialog, which) -> launchAppDetails(context, callback))
                .setNegativeButton("Done", null)
                .show();
    }

    private static void launchAppDetails(Context context, AutostartCallback callback) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception e) {
            callback.onIntentError();
        }
    }

    private static void launchAnyIntent(Context context, Intent[] intents, AutostartCallback callback) {
        boolean launched = false;
        for (Intent intent : intents) {
            if (context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                try {
                    context.startActivity(intent);
                    launched = true;
                    break;
                } catch (Exception ignored) {}
            }
        }
        if (!launched) {
            callback.onIntentError();
        }
    }
}
