/*
 * SPDX-FileCopyrightText: 2021 Project Radiant
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget;

import static android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED;

import android.animation.ValueAnimator;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.android.settingslib.utils.ThreadUtils;

import java.util.function.Consumer;

/**
 * The home screen wallpaper, cropped to fill the view, after Project Radiant's aboutWallpaperView
 * as Project Infinity X uses it. It is read once, off the main thread, when the view is attached.
 * A live wallpaper has no picture to show, so the view's background stays.
 */
public class CatalyxxWallpaperView extends ImageView {

    private static final String TAG = "CatalyxxWallpaperView";
    private static final float BLUR_RADIUS = 100f;
    private static final long BLUR_ANIMATION_MS = 300;

    private final Consumer<Boolean> mSystemBlurListener = this::onSystemBlurChanged;
    private boolean mFollowsSystemBlur;
    private boolean mSystemBlurKnown;
    private float mBlurRadius;
    @Nullable
    private ValueAnimator mBlurAnimator;

    public CatalyxxWallpaperView(Context context) {
        this(context, null);
    }

    public CatalyxxWallpaperView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.CENTER_CROP);
    }

    /**
     * Blurs the picture while the system blurs behind its windows (the shade, dialogs, the
     * launcher), so the picture shows whether system blur is on. On a device without system
     * blur, the picture stays blurred.
     */
    public void setFollowsSystemBlur(boolean follows) {
        if (mFollowsSystemBlur == follows) {
            return;
        }
        mFollowsSystemBlur = follows;
        if (isAttachedToWindow()) {
            if (follows) {
                startFollowingSystemBlur();
            } else {
                stopFollowingSystemBlur();
                setBlurRadius(0f, false);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mFollowsSystemBlur) {
            startFollowingSystemBlur();
        }
        if (getDrawable() != null) {
            return;
        }
        ThreadUtils.postOnBackgroundThread(() -> {
            final Drawable wallpaper = loadWallpaper();
            if (wallpaper != null) {
                post(() -> setImageDrawable(wallpaper));
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        stopFollowingSystemBlur();
        super.onDetachedFromWindow();
    }

    private void startFollowingSystemBlur() {
        if (!CROSS_WINDOW_BLUR_SUPPORTED) {
            setBlurRadius(BLUR_RADIUS, false);
            return;
        }
        // Called at once with the current state, then on every change, Battery Saver included
        getContext().getSystemService(WindowManager.class).addCrossWindowBlurEnabledListener(
                getContext().getMainExecutor(), mSystemBlurListener);
    }

    private void stopFollowingSystemBlur() {
        if (CROSS_WINDOW_BLUR_SUPPORTED) {
            getContext().getSystemService(WindowManager.class)
                    .removeCrossWindowBlurEnabledListener(mSystemBlurListener);
        }
        mSystemBlurKnown = false;
    }

    private void onSystemBlurChanged(boolean enabled) {
        // Only a change while on screen eases in; the state on attach just shows
        setBlurRadius(enabled ? BLUR_RADIUS : 0f, mSystemBlurKnown);
        mSystemBlurKnown = true;
    }

    private void setBlurRadius(float radius, boolean animate) {
        if (mBlurAnimator != null) {
            mBlurAnimator.cancel();
            mBlurAnimator = null;
        }
        if (!animate) {
            applyBlurRadius(radius);
            return;
        }
        mBlurAnimator = ValueAnimator.ofFloat(mBlurRadius, radius);
        mBlurAnimator.setDuration(BLUR_ANIMATION_MS);
        mBlurAnimator.addUpdateListener(
                animator -> applyBlurRadius((float) animator.getAnimatedValue()));
        mBlurAnimator.start();
    }

    private void applyBlurRadius(float radius) {
        mBlurRadius = radius;
        setRenderEffect(radius > 0f
                ? RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                : null);
    }

    @Nullable
    private Drawable loadWallpaper() {
        try {
            return WallpaperManager.getInstance(getContext()).getDrawable();
        } catch (SecurityException e) {
            // Needs READ_WALLPAPER_INTERNAL
            Log.w(TAG, "Can't read the wallpaper", e);
            return null;
        }
    }
}
