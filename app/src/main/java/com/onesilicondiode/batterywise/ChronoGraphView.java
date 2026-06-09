package com.onesilicondiode.batterywise;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Custom Canvas-drawn smooth line graph with:
 * - Bézier curves between data points
 * - Gradient fill beneath the line
 * - Animated dot markers with glow
 * - Touch-to-inspect tooltip
 * - Smooth entrance animation
 */
public class ChronoGraphView extends View {

    // ── Data ──
    private final List<DataPoint> dataPoints = new ArrayList<>();
    private float minY = Float.MAX_VALUE;
    private float maxY = Float.MIN_VALUE;
    private long minX = Long.MAX_VALUE;
    private long maxX = Long.MIN_VALUE;

    // ── Paints ──
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Paths ──
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    // ── Layout constants ──
    private static final float PADDING_LEFT = 12f;
    private static final float PADDING_RIGHT = 16f;
    private static final float PADDING_TOP = 24f;
    private static final float PADDING_BOTTOM = 36f;
    private static final int GRID_LINES = 4;

    // ── Colors ──
    private int lineColor = 0xFF006E1C; // primary green
    private int fillColorTop = 0x40006E1C;
    private int fillColorBottom = 0x00006E1C;

    // ── Animation ──
    private float animationProgress = 0f;
    private ValueAnimator entryAnimator;

    // ── Touch / tooltip ──
    private int selectedIndex = -1;

    // ── Unit label ──
    private String unitLabel = "";
    private String emptyMessage = "No data yet";

    public ChronoGraphView(Context context) {
        super(context);
        init();
    }

    public ChronoGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChronoGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        // Resolve theme-aware colors for dark/light mode
        int onSurfaceColor = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface, 0xDE000000);
        int surfaceInverseColor = resolveThemeColor(com.google.android.material.R.attr.colorSurfaceInverse, 0xE6000000);
        int onSurfaceInverseColor = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceInverse, 0xFFFFFFFF);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(lineColor);

        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(lineColor);

        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setColor(lineColor);
        glowPaint.setAlpha(40);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.5f * density);
        gridPaint.setColor(setAlpha(onSurfaceColor, 0x15));

        labelPaint.setTextSize(10f * density);
        labelPaint.setColor(setAlpha(onSurfaceColor, 0x99));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        tooltipBgPaint.setColor(setAlpha(surfaceInverseColor, 0xE6));
        tooltipBgPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint.setColor(onSurfaceInverseColor);
        tooltipTextPaint.setTextSize(11f * density);
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);

        emptyTextPaint.setTextSize(14f * density);
        emptyTextPaint.setColor(setAlpha(onSurfaceColor, 0x66));
        emptyTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Resolves a color from the current theme, falling back to a default.
     */
    private int resolveThemeColor(int attr, int fallback) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = getContext().obtainStyledAttributes(typedValue.data, new int[]{attr});
        int color = a.getColor(0, fallback);
        a.recycle();
        return color;
    }

    /**
     * Replaces the alpha channel of a color.
     */
    private static int setAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public void setLineColor(int color) {
        this.lineColor = color;
        linePaint.setColor(color);
        dotPaint.setColor(color);
        glowPaint.setColor(color);
        glowPaint.setAlpha(40);
        fillColorTop = (color & 0x00FFFFFF) | 0x40000000;
        fillColorBottom = (color & 0x00FFFFFF) | 0x00000000;
        invalidate();
    }

    public void setUnitLabel(String label) {
        this.unitLabel = label;
    }

    public void setEmptyMessage(String msg) {
        this.emptyMessage = msg;
    }

    public void setData(List<DataPoint> points) {
        dataPoints.clear();
        if (points != null) {
            dataPoints.addAll(points);
        }
        recalculateBounds();
        selectedIndex = -1;
        animateEntry();
    }

    private void recalculateBounds() {
        minY = Float.MAX_VALUE;
        maxY = Float.MIN_VALUE;
        minX = Long.MAX_VALUE;
        maxX = Long.MIN_VALUE;
        for (DataPoint dp : dataPoints) {
            if (dp.value < minY) minY = dp.value;
            if (dp.value > maxY) maxY = dp.value;
            if (dp.timestampMs < minX) minX = dp.timestampMs;
            if (dp.timestampMs > maxX) maxX = dp.timestampMs;
        }
        // If all values are the same (flat line), add a small symmetric range
        float yRange = maxY - minY;
        if (yRange < 1f) {
            minY -= 5f;
            maxY += 5f;
        } else {
            // Only add a tiny bottom margin so the lowest point isn't clipped;
            // the top stays at the real max (no phantom values above data)
            minY -= yRange * 0.05f;
        }
    }

    private void animateEntry() {
        if (entryAnimator != null) entryAnimator.cancel();
        entryAnimator = ValueAnimator.ofFloat(0f, 1f);
        entryAnimator.setDuration(800);
        entryAnimator.setInterpolator(new OvershootInterpolator(0.6f));
        entryAnimator.addUpdateListener(a -> {
            animationProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        entryAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dataPoints.size() < 2) {
            // Draw empty state
            canvas.drawText(emptyMessage, getWidth() / 2f, getHeight() / 2f, emptyTextPaint);
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        float padLeft = PADDING_LEFT * density;
        float padRight = PADDING_RIGHT * density;
        float padTop = PADDING_TOP * density;
        float padBottom = PADDING_BOTTOM * density;
        float graphWidth = getWidth() - padLeft - padRight;
        float graphHeight = getHeight() - padTop - padBottom;

        // ── Draw grid lines ──
        for (int i = 0; i <= GRID_LINES; i++) {
            float y = padTop + (graphHeight * i / GRID_LINES);
            canvas.drawLine(padLeft, y, getWidth() - padRight, y, gridPaint);

            // Y-axis labels
            float labelValue = maxY - ((maxY - minY) * i / GRID_LINES);
            String label = formatValue(labelValue);
            canvas.drawText(label, padLeft + graphWidth / 2f, y - 3 * density, labelPaint);
        }

        // ── Map data points to canvas coordinates ──
        float[] xCoords = new float[dataPoints.size()];
        float[] yCoords = new float[dataPoints.size()];
        long xRange = maxX - minX;
        if (xRange == 0) xRange = 1;
        float yRange = maxY - minY;
        if (yRange == 0) yRange = 1;

        for (int i = 0; i < dataPoints.size(); i++) {
            DataPoint dp = dataPoints.get(i);
            xCoords[i] = padLeft + ((dp.timestampMs - minX) / (float) xRange) * graphWidth;
            float rawY = padTop + graphHeight - ((dp.value - minY) / yRange) * graphHeight;
            // Animate Y position from bottom
            yCoords[i] = padTop + graphHeight + (rawY - padTop - graphHeight) * animationProgress;
        }

        // ── Build smooth Bézier curve path ──
        linePath.reset();
        fillPath.reset();

        linePath.moveTo(xCoords[0], yCoords[0]);
        fillPath.moveTo(xCoords[0], padTop + graphHeight); // bottom-left
        fillPath.lineTo(xCoords[0], yCoords[0]);

        for (int i = 1; i < dataPoints.size(); i++) {
            float cpx = (xCoords[i - 1] + xCoords[i]) / 2f;
            linePath.cubicTo(cpx, yCoords[i - 1], cpx, yCoords[i], xCoords[i], yCoords[i]);
            fillPath.cubicTo(cpx, yCoords[i - 1], cpx, yCoords[i], xCoords[i], yCoords[i]);
        }

        // Close fill path
        fillPath.lineTo(xCoords[xCoords.length - 1], padTop + graphHeight);
        fillPath.close();

        // ── Draw gradient fill ──
        fillPaint.setShader(new LinearGradient(
                0, padTop, 0, padTop + graphHeight,
                fillColorTop, fillColorBottom,
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, fillPaint);

        // ── Draw line ──
        canvas.drawPath(linePath, linePaint);

        // ── Draw data point dots ──
        for (int i = 0; i < dataPoints.size(); i++) {
            float dotRadius = (i == selectedIndex) ? 6f * density : 3.5f * density;
            float glowRadius = dotRadius * 3f;

            if (i == selectedIndex) {
                canvas.drawCircle(xCoords[i], yCoords[i], glowRadius, glowPaint);
            }
            canvas.drawCircle(xCoords[i], yCoords[i], dotRadius, dotPaint);
        }

        // ── Draw X-axis date labels (first, middle, last) ──
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
        int[] labelIndices = {0, dataPoints.size() / 2, dataPoints.size() - 1};
        for (int idx : labelIndices) {
            if (idx >= 0 && idx < dataPoints.size()) {
                String dateLabel = sdf.format(new Date(dataPoints.get(idx).timestampMs));
                canvas.drawText(dateLabel, xCoords[idx],
                        padTop + graphHeight + 20 * density, labelPaint);
            }
        }

        // ── Draw tooltip for selected point ──
        if (selectedIndex >= 0 && selectedIndex < dataPoints.size()) {
            DataPoint dp = dataPoints.get(selectedIndex);
            String tooltipText = formatValue(dp.value) + unitLabel + "  •  "
                    + new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                    .format(new Date(dp.timestampMs));

            float textW = tooltipTextPaint.measureText(tooltipText);
            float tooltipW = textW + 24 * density;
            float tooltipH = 28 * density;
            float tx = xCoords[selectedIndex] - tooltipW / 2f;
            float ty = yCoords[selectedIndex] - 22 * density - tooltipH;

            // Keep tooltip on-screen
            if (tx < padLeft) tx = padLeft;
            if (tx + tooltipW > getWidth() - padRight) tx = getWidth() - padRight - tooltipW;
            if (ty < 0) ty = yCoords[selectedIndex] + 14 * density;

            RectF tooltipRect = new RectF(tx, ty, tx + tooltipW, ty + tooltipH);
            canvas.drawRoundRect(tooltipRect, 10 * density, 10 * density, tooltipBgPaint);
            canvas.drawText(tooltipText, tooltipRect.centerX(),
                    tooltipRect.centerY() + 4 * density, tooltipTextPaint);
        }
    }

    private String formatValue(float value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (dataPoints.size() < 2) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {
            float touchX = event.getX();
            float density = getResources().getDisplayMetrics().density;
            float padLeft = PADDING_LEFT * density;
            float padRight = PADDING_RIGHT * density;
            float graphWidth = getWidth() - padLeft - padRight;
            long xRange = maxX - minX;
            if (xRange == 0) xRange = 1;

            float closestDist = Float.MAX_VALUE;
            int closestIdx = -1;
            for (int i = 0; i < dataPoints.size(); i++) {
                float cx = padLeft + ((dataPoints.get(i).timestampMs - minX) / (float) xRange) * graphWidth;
                float dist = Math.abs(touchX - cx);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestIdx = i;
                }
            }
            if (closestIdx != selectedIndex) {
                selectedIndex = closestIdx;
                invalidate();
                // Haptic tick
                performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            }
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
            // Keep selection visible for a moment
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * A single data point with a timestamp and a floating-point value.
     */
    public static class DataPoint {
        public final long timestampMs;
        public final float value;

        public DataPoint(long timestampMs, float value) {
            this.timestampMs = timestampMs;
            this.value = value;
        }
    }
}
