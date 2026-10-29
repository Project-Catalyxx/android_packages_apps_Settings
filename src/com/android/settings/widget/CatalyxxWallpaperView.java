/*
 * SPDX-FileCopyrightText: 2021 Project Radiant
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.android.settingslib.utils.ThreadUtils;

/**
 * The home screen wallpaper, cropped to fill the view, after Project Radiant's aboutWallpaperView
 * as Project Infinity X uses it. It is read once, off the main thread, when the view is attached.
 * A live wallpaper has no picture to show, so the view's background stays.
 */
public class CatalyxxWallpaperView extends ImageView {

    private static final String TAG = "CatalyxxWallpaperView";
    private static final float BLUR_RADIUS = 100f;

    private boolean mBlurred;

    public CatalyxxWallpaperView(Context context) {
        this(context, null);
    }

    public CatalyxxWallpaperView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.CENTER_CROP);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
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

    /** Blurs the picture, for text drawn on top of it. */
    public void setBlurred(boolean blurred) {
        mBlurred = blurred;
        setRenderEffect(blurred
                ? RenderEffect.createBlurEffect(BLUR_RADIUS, BLUR_RADIUS, Shader.TileMode.CLAMP)
                : null);
    }

    public boolean isBlurred() {
        return mBlurred;
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
