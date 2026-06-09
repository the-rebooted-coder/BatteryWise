package com.onesilicondiode.batterywise;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

/**
 * Quick Settings Tile for SafeCharge — "One-Tap Guardian"
 *
 * Allows users to toggle battery monitoring on/off directly from the
 * notification shade without opening the app.
 *
 * Supports all API levels from 24+ (minSdk is 25).
 *
 * Tile states:
 *   ACTIVE   → SafeCharge is monitoring, tile shows battery % and "Monitoring"
 *   INACTIVE → SafeCharge is off, tile shows "Tap to start"
 */
public class SafeChargeTileService extends TileService {

    private static final String TAG = "SafeChargeTile";

    /**
     * Called when the tile becomes visible in the Quick Settings panel.
     * Update the tile to reflect current service state.
     */
    @Override
    public void onStartListening() {
        super.onStartListening();
        setTileState(isServiceRunning());
    }

    /**
     * Called when the user taps the tile — toggle SafeCharge on/off.
     *
     * Key design: we optimistically set the tile to the NEW intended state
     * immediately, before the service has started/stopped. This avoids the
     * race condition where the service hasn't set isRunning yet when we
     * query it. The service's own requestListeningState() call in
     * onCreate/onDestroy provides eventual consistency.
     */
    @Override
    public void onClick() {
        super.onClick();

        boolean wasRunning = isServiceRunning();

        if (wasRunning) {
            // ── Stop monitoring ──
            try {
                Intent stopIntent = new Intent(this, BatteryMonitorService.class);
                stopService(stopIntent);
                // Eagerly clear the persisted flag so the tile doesn't read stale state
                getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                        .edit().putBoolean("serviceRunning", false).apply();
                Log.d(TAG, "SafeCharge stopped via QS tile");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping service", e);
            }
            // Optimistically show INACTIVE
            setTileState(false);
        } else {
            // ── Start monitoring ──
            boolean started = false;
            try {
                Intent startIntent = new Intent(this, BatteryMonitorService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(startIntent);
                } else {
                    startService(startIntent);
                }
                started = true;
                Log.d(TAG, "SafeCharge started via QS tile");
            } catch (Exception e) {
                Log.e(TAG, "Error starting service", e);
                // If we can't start the foreground service (e.g. background restrictions),
                // open the app so the user can start it manually
                try {
                    Intent launchIntent = new Intent(this, MainActivity.class);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityAndCollapse(launchIntent);
                } catch (Exception ex) {
                    Log.e(TAG, "Error launching activity", ex);
                }
            }
            // Optimistically show ACTIVE (only if start call succeeded)
            setTileState(started);
        }
    }

    /**
     * Sets the tile's visual state deterministically.
     * Called from onClick with the intended new state (optimistic),
     * and from onStartListening with the actual queried state.
     */
    private void setTileState(boolean active) {
        Tile tile = getQsTile();
        if (tile == null) return;

        if (active) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel("SafeCharge");

            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            int alertLevel = prefs.getInt("selectedBatteryLevel", 85);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle("Alert at " + alertLevel + "%");
            } else {
                tile.setLabel("SafeCharge · " + alertLevel + "%");
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                tile.setStateDescription("Active");
            }
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("SafeCharge");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle("Tap to start");
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                tile.setStateDescription("Inactive");
            }
        }

        tile.updateTile();
    }

    /**
     * Checks if BatteryMonitorService is currently running.
     * Uses both the static flag and persisted preference for robustness.
     */
    private boolean isServiceRunning() {
        // Primary check: static flag (most reliable when service is alive)
        if (BatteryMonitorService.isRunning) {
            return true;
        }
        // Fallback: persisted preference (survives process death)
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean("serviceRunning", false);
    }
}
