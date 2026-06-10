package com.onesilicondiode.batterywise;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import com.onesilicondiode.batterywise.db.BatterySnapshot;
import com.onesilicondiode.batterywise.db.ChargeSession;
import com.onesilicondiode.batterywise.db.ChronoDatabase;
import com.onesilicondiode.batterywise.db.ChronoDao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChronoCellActivity extends AppCompatActivity {

    // ── Time ranges in milliseconds ──
    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;
    private static final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000;

    // ── Views ──
    private ChronoGraphView graphTemperature;
    private ChronoGraphView graphBatteryLevel;
    private ChronoGraphView graphSessions;
    private MaterialTextView txtStatCycles, txtStatTemp, txtStatHours, txtStatSafeCharged;
    private MaterialTextView txtStatCyclesLabel, txtStatHoursLabel;
    private MaterialTextView txtPrediction, txtHealthPercent;
    private CircularProgressIndicator progressHealth;
    private ChipGroup timeChips;

    // ── Data ──
    private ChronoDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long currentRangeMs = SEVEN_DAYS_MS;

    // ── Cards for stagger animation ──
    private final List<View> animatableCards = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chrono_cell);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        dao = ChronoDatabase.getInstance(this).chronoDao();

        initViews();
        setupChips();
        setupGraphColors();
        collectAnimatableCards();
        runStaggeredEntryAnimation();
        loadData();
    }

    private void initViews() {
        graphTemperature = findViewById(R.id.graph_temperature);
        graphBatteryLevel = findViewById(R.id.graph_battery_level);
        graphSessions = findViewById(R.id.graph_sessions);

        txtStatCycles = findViewById(R.id.txt_stat_cycles);
        txtStatTemp = findViewById(R.id.txt_stat_temp);
        txtStatHours = findViewById(R.id.txt_stat_hours);
        txtStatSafeCharged = findViewById(R.id.txt_stat_safecharged);
        txtStatCyclesLabel = findViewById(R.id.txt_stat_cycles_label);
        txtStatHoursLabel = findViewById(R.id.txt_stat_hours_label);

        txtPrediction = findViewById(R.id.txt_prediction);
        txtHealthPercent = findViewById(R.id.txt_health_percent);
        progressHealth = findViewById(R.id.progress_health);

        timeChips = findViewById(R.id.chrono_time_chips);

        graphTemperature.setUnitLabel("°C");
        graphTemperature.setEmptyMessage("No temperature data yet");
        graphBatteryLevel.setUnitLabel("%");
        graphBatteryLevel.setEmptyMessage("No battery snapshots yet");
        graphSessions.setUnitLabel("%");
        graphSessions.setEmptyMessage("No charge sessions yet");
    }

    private void setupGraphColors() {
        int primaryColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorPrimary);
        int tertiaryColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorTertiary);
        int secondaryColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorSecondary);

        graphTemperature.setLineColor(tertiaryColor);
        graphBatteryLevel.setLineColor(primaryColor);
        graphSessions.setLineColor(secondaryColor);
    }

    private void setupChips() {
        Chip chip7d = findViewById(R.id.chip_7d);
        Chip chip30d = findViewById(R.id.chip_30d);
        Chip chipAll = findViewById(R.id.chip_all);

        timeChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            HapticUtils.vibrateTouch(this);
            if (checkedIds.contains(R.id.chip_7d)) {
                currentRangeMs = SEVEN_DAYS_MS;
            } else if (checkedIds.contains(R.id.chip_30d)) {
                currentRangeMs = THIRTY_DAYS_MS;
            } else if (checkedIds.contains(R.id.chip_all)) {
                currentRangeMs = 0; // 0 = all time
            }
            loadData();
        });
    }

    private void collectAnimatableCards() {
        animatableCards.clear();
        animatableCards.add(findViewById(R.id.chrono_header));
        animatableCards.add(findViewById(R.id.card_temp_graph));
        animatableCards.add(findViewById(R.id.card_level_graph));
        animatableCards.add(findViewById(R.id.card_sessions_graph));
        animatableCards.add(findViewById(R.id.card_stat_cycles));
        animatableCards.add(findViewById(R.id.card_stat_temp));
        animatableCards.add(findViewById(R.id.card_stat_hours));
        animatableCards.add(findViewById(R.id.card_stat_safecharged));
        animatableCards.add(findViewById(R.id.card_prediction));
        animatableCards.add(findViewById(R.id.card_data_notice));
    }

    private void runStaggeredEntryAnimation() {
        for (int i = 0; i < animatableCards.size(); i++) {
            View card = animatableCards.get(i);
            card.setAlpha(0f);
            card.setTranslationY(60f);
            card.setScaleX(0.95f);
            card.setScaleY(0.95f);

            ObjectAnimator alpha = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
            ObjectAnimator transY = ObjectAnimator.ofFloat(card, "translationY", 60f, 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(card, "scaleX", 0.95f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(card, "scaleY", 0.95f, 1f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(alpha, transY, scaleX, scaleY);
            set.setDuration(500);
            set.setStartDelay(i * 60L);
            set.setInterpolator(new OvershootInterpolator(0.8f));
            set.start();

            // Haptic tick for first few cards
            if (i < 4) {
                mainHandler.postDelayed(() -> HapticUtils.playCustomVibration(this,
                        new long[]{3, 0, 3, 1, 2}), i * 60L + 100L);
            }
        }
    }

    private void loadData() {
        executor.execute(() -> {
            long sinceMs = currentRangeMs > 0
                    ? System.currentTimeMillis() - currentRangeMs
                    : 0;

            // Fetch snapshots
            List<BatterySnapshot> snapshots = (sinceMs > 0)
                    ? dao.getSnapshotsSince(sinceMs)
                    : dao.getAllSnapshots();

            // Fetch sessions
            List<ChargeSession> sessions = (sinceMs > 0)
                    ? dao.getSessionsSince(sinceMs)
                    : dao.getAllSessions();

            // Lifetime stats (always all-time)
            int totalCycles = dao.getSessionCount();
            float avgTemp = 0;
            try {
                avgTemp = dao.getAverageSessionTemperature();
            } catch (Exception ignored) {}
            long totalDurationMs = 0;
            try {
                totalDurationMs = dao.getTotalChargingDurationMs();
            } catch (Exception ignored) {}
            int safeChargedCount = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    .getInt("counter", 0);

            // Build graph data
            List<ChronoGraphView.DataPoint> tempData = new ArrayList<>();
            List<ChronoGraphView.DataPoint> levelData = new ArrayList<>();
            for (BatterySnapshot s : snapshots) {
                tempData.add(new ChronoGraphView.DataPoint(s.timestampMs, s.temperature));
                levelData.add(new ChronoGraphView.DataPoint(s.timestampMs, s.batteryPercent));
            }

            List<ChronoGraphView.DataPoint> sessionData = new ArrayList<>();
            for (ChargeSession cs : sessions) {
                sessionData.add(new ChronoGraphView.DataPoint(cs.startTimeMs, cs.endPercent));
            }

            // Prediction: typical Li-ion degrades to ~80% after 500 full cycles
            // Estimate remaining cycles
            final int predictedTotalCycles = 500;
            int remainingCycles = Math.max(0, predictedTotalCycles - totalCycles);
            float healthPercent = Math.max(0, Math.min(100,
                    100f - (totalCycles / (float) predictedTotalCycles) * 20f));

            // Estimate days until 80% health based on average cycles/day
            long firstSessionTime = 0;
            if (!sessions.isEmpty()) {
                firstSessionTime = sessions.get(0).startTimeMs;
            }
            String predictionText;
            final int finalHealthPercent = Math.round(healthPercent);
            if (totalCycles < 3) {
                predictionText = "Need at least 3 charge cycles to estimate lifespan";
            } else {
                long daysSinceFirst = Math.max(1,
                        (System.currentTimeMillis() - firstSessionTime) / (24 * 60 * 60 * 1000));
                float cyclesPerDay = totalCycles / (float) daysSinceFirst;
                int daysRemaining = (cyclesPerDay > 0)
                        ? (int) (remainingCycles / cyclesPerDay) : 9999;

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, daysRemaining);
                String dateStr = new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        .format(cal.getTime());
                predictionText = "≈ 80% capacity by " + dateStr
                        + "\nBased on " + totalCycles + " cycles over "
                        + daysSinceFirst + " " + (daysSinceFirst == 1 ? "day" : "days");
            }

            // Final copies for lambda
            final float fAvgTemp = avgTemp;
            final long fTotalDuration = totalDurationMs;
            final int fSafeCharged = safeChargedCount;
            final int fTotalCycles = totalCycles;
            final String fPrediction = predictionText;
            final List<ChronoGraphView.DataPoint> fTempData = tempData;
            final List<ChronoGraphView.DataPoint> fLevelData = levelData;
            final List<ChronoGraphView.DataPoint> fSessionData = sessionData;

            mainHandler.post(() -> {
                // Update graphs
                graphTemperature.setData(fTempData);
                graphBatteryLevel.setData(fLevelData);
                graphSessions.setData(fSessionData);

                // Update stats
                txtStatCycles.setText(String.valueOf(fTotalCycles));
                txtStatCyclesLabel.setText(fTotalCycles == 1 ? "Charge Cycle" : "Charge Cycles");
                animateStatBounce(txtStatCycles);

                if (fAvgTemp > 0) {
                    txtStatTemp.setText(String.format(Locale.getDefault(), "%.1f°", fAvgTemp));
                } else {
                    txtStatTemp.setText("—");
                }
                animateStatBounce(txtStatTemp);

                long totalHours = fTotalDuration / (60 * 60 * 1000);
                if (totalHours > 0) {
                    txtStatHours.setText(totalHours + "h");
                    txtStatHoursLabel.setText(totalHours == 1 ? "Hour Monitored" : "Hours Monitored");
                } else {
                    long totalMins = fTotalDuration / (60 * 1000);
                    if (totalMins > 0) {
                        txtStatHours.setText(totalMins + "m");
                        txtStatHoursLabel.setText(totalMins == 1 ? "Minute Monitored" : "Minutes Monitored");
                    } else {
                        txtStatHours.setText("—");
                        txtStatHoursLabel.setText("Time Monitored");
                    }
                }
                animateStatBounce(txtStatHours);

                txtStatSafeCharged.setText(String.valueOf(fSafeCharged));
                animateStatBounce(txtStatSafeCharged);

                // Update prediction
                txtHealthPercent.setText(finalHealthPercent + "%");
                progressHealth.setProgress(finalHealthPercent, true);
                txtPrediction.setText(fPrediction);
            });
        });
    }

    /**
     * Bouncy scale animation for stat numbers when they update
     */
    private void animateStatBounce(View view) {
        view.setScaleX(0.5f);
        view.setScaleY(0.5f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(2.5f))
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
