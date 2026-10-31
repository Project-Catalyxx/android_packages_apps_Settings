/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.homepage.periodic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

/**
 * The eclipse on the periodic home: a dark disk with the battery level in it, ringed by an arc
 * that fills with the battery and haloed by a corona in the theme accent, brighter while the
 * phone charges, when a bolt shows under the level.
 */
public class EclipseView extends View {

    // Proportions of the view's size, as in the mockup's 104-unit eclipse
    private static final float RING_RADIUS = 47f / 104;
    private static final float RING_WIDTH = 3f / 104;
    private static final float DISK_RADIUS = 30f / 104;
    private static final float RIM_WIDTH = 1.5f / 104;
    private static final float TEXT_SIZE = 16f / 104;

    private final Paint mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCoronaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDiskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBoltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mArcBounds = new RectF();
    private final Path mBolt = new Path();

    private int mLevel = -1;
    private boolean mCharging;
    @Nullable
    private String mLevelText;
    @ColorInt
    private int mAccent;

    public EclipseView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mTrackPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mDiskPaint.setColor(PeriodicColors.DISK);
        mRimPaint.setStyle(Paint.Style.STROKE);
        mRimPaint.setColor(PeriodicColors.DISK_RIM);
        mTextPaint.setColor(0xFFFFFFFF);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    /** The colors: the corona's accent, the battery arc, and the arc's empty track. */
    void setColors(@ColorInt int accent, @ColorInt int ring, @ColorInt int track) {
        mAccent = accent;
        mArcPaint.setColor(ring);
        mBoltPaint.setColor(ring);
        mTrackPaint.setColor(track);
        updateCorona();
        invalidate();
    }

    /** The battery level in percent (-1 if unknown), as shown, and whether it charges. */
    void setBattery(int level, @Nullable String levelText, boolean charging) {
        if (level == mLevel && charging == mCharging) {
            return;
        }
        mLevel = level;
        mLevelText = levelText;
        mCharging = charging;
        updateCorona();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateCorona();
    }

    private void updateCorona() {
        final float size = Math.min(getWidth(), getHeight());
        if (size <= 0) {
            return;
        }
        // A soft halo from just inside the disk's rim out to the ring
        final int glow = PeriodicColors.alpha(mAccent, mCharging ? 0.6f : 0.33f);
        mCoronaPaint.setShader(new RadialGradient(getWidth() / 2f, getHeight() / 2f,
                size * RING_RADIUS, new int[] {glow, glow, 0},
                new float[] {0f, DISK_RADIUS / RING_RADIUS, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float size = Math.min(getWidth(), getHeight());
        final float cx = getWidth() / 2f;
        final float cy = getHeight() / 2f;

        canvas.drawCircle(cx, cy, size * RING_RADIUS, mCoronaPaint);

        final float ringRadius = size * RING_RADIUS;
        mTrackPaint.setStrokeWidth(size * RING_WIDTH);
        mArcPaint.setStrokeWidth(size * RING_WIDTH);
        canvas.drawCircle(cx, cy, ringRadius, mTrackPaint);
        if (mLevel > 0) {
            mArcBounds.set(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius);
            canvas.drawArc(mArcBounds, -90f, 360f * Math.min(mLevel, 100) / 100f, false,
                    mArcPaint);
        }

        final float diskRadius = size * DISK_RADIUS;
        canvas.drawCircle(cx, cy, diskRadius, mDiskPaint);
        mRimPaint.setStrokeWidth(size * RIM_WIDTH);
        canvas.drawCircle(cx, cy, diskRadius, mRimPaint);

        if (mLevelText != null) {
            mTextPaint.setTextSize(size * TEXT_SIZE);
            // Centered on the disk, or nudged up to make room for the bolt
            final float lift = mCharging ? size * 0.05f : 0f;
            final float baseline = cy - lift
                    - (mTextPaint.descent() + mTextPaint.ascent()) / 2f;
            canvas.drawText(mLevelText, cx, baseline, mTextPaint);
            if (mCharging) {
                drawBolt(canvas, cx, baseline + size * 0.035f, size * 0.09f);
            }
        }
    }

    // A small lightning bolt, its top centered at (cx, top), h tall
    private void drawBolt(Canvas canvas, float cx, float top, float h) {
        final float w = h * 18f / 22f;
        final float x0 = cx - w / 2f;
        final float s = h / 22f;
        mBolt.reset();
        mBolt.moveTo(x0 + 10.5f * s, top + 1f * s);
        mBolt.lineTo(x0 + 2f * s, top + 12.5f * s);
        mBolt.lineTo(x0 + 8f * s, top + 12.5f * s);
        mBolt.lineTo(x0 + 7f * s, top + 21f * s);
        mBolt.lineTo(x0 + 16f * s, top + 9f * s);
        mBolt.lineTo(x0 + 10f * s, top + 9f * s);
        mBolt.close();
        canvas.drawPath(mBolt, mBoltPaint);
    }
}
