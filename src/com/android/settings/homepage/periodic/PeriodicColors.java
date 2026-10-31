/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.homepage.periodic;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

import androidx.annotation.ColorInt;

import com.android.settings.homepage.periodic.PeriodicElement.Family;
import com.android.settingslib.Utils;

/**
 * The periodic home's colors: one per element family from the Catalyxx palette, and the theme
 * accent for the Lab. The dark theme shows a family's color as is; the light theme darkens the
 * text drawn in it, so it stays readable (4.5:1 or better) on the light background.
 */
final class PeriodicColors {

    // Connections, power, look, apps, privacy, system, extras
    private static final int[] CATALYXX = {
        0xFF5CC8FF, 0xFFFFB547, 0xFFB47CFF, 0xFF4FE0B0, 0xFFFF7A9A, 0xFFC9C4D8, 0xFF8F889E,
    };

    // How much of a color's brightness its text keeps on the light theme
    private static final float LIGHT_INK = 0.55f;

    // The eclipse disk and its rim, the same on both themes
    @ColorInt static final int DISK = 0xFF050308;
    @ColorInt static final int DISK_RIM = 0xFFEADCFF;

    final boolean night;
    @ColorInt private final int mAccent;
    @ColorInt private final int mAccentLight;

    PeriodicColors(Context context) {
        night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        mAccent = Utils.getColorAccentDefaultColor(context);
        mAccentLight = mix(mAccent, Color.WHITE, 0.62f);
    }

    /** The family's own color. */
    @ColorInt
    int base(Family family) {
        return family == Family.LAB ? mAccent : CATALYXX[family.ordinal()];
    }

    /** A tile's symbol, number and live value. */
    @ColorInt
    int ink(Family family) {
        if (family == Family.LAB) {
            return night ? Color.WHITE : darken(mAccent);
        }
        return night ? base(family) : darken(base(family));
    }

    /** A tile's fill. */
    @ColorInt
    int fill(Family family) {
        if (family == Family.LAB) {
            return alpha(mAccent, night ? 0.3f : 0.16f);
        }
        return alpha(base(family), night ? 0.13f : 0.16f);
    }

    /** A tile's border. */
    @ColorInt
    int border(Family family) {
        if (family == Family.LAB) {
            return night ? alpha(mAccentLight, 0.9f) : alpha(mAccent, 0.6f);
        }
        return alpha(base(family), night ? 0.38f : 0.5f);
    }

    /** The badge a symbol sits in. */
    @ColorInt
    int badgeFill(Family family) {
        if (family == Family.LAB) {
            return night ? alpha(Color.WHITE, 0.1f) : alpha(mAccent, 0.12f);
        }
        return alpha(base(family), night ? 0.16f : 0.22f);
    }

    /** The badge's border. */
    @ColorInt
    int badgeBorder(Family family) {
        if (family == Family.LAB) {
            return night ? alpha(mAccentLight, 0.5f) : alpha(mAccent, 0.4f);
        }
        return alpha(base(family), night ? 0.35f : 0.45f);
    }

    /** The eclipse's corona, in the theme accent. */
    @ColorInt
    int accent() {
        return mAccent;
    }

    /** The battery ring and the version line under the phone's name. */
    @ColorInt
    int ring() {
        return night ? mAccentLight : darken(mAccent);
    }

    /** The empty part of the battery ring. */
    @ColorInt
    int track() {
        return night ? alpha(Color.WHITE, 0.08f) : alpha(Color.BLACK, 0.08f);
    }

    /** The quiet card behind the eclipse. */
    @ColorInt
    int cardFill() {
        return night ? alpha(Color.WHITE, 0.025f) : alpha(Color.BLACK, 0.03f);
    }

    /** That card's border. */
    @ColorInt
    int cardBorder() {
        return night ? alpha(Color.WHITE, 0.06f) : alpha(Color.BLACK, 0.06f);
    }

    @ColorInt
    static int alpha(@ColorInt int color, float alpha) {
        return (Math.round(alpha * 255) << 24) | (color & 0xFFFFFF);
    }

    @ColorInt
    private static int darken(@ColorInt int color) {
        return Color.rgb(Math.round(Color.red(color) * LIGHT_INK),
                Math.round(Color.green(color) * LIGHT_INK),
                Math.round(Color.blue(color) * LIGHT_INK));
    }

    @ColorInt
    private static int mix(@ColorInt int from, @ColorInt int to, float amount) {
        return Color.rgb(
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * amount),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * amount),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount));
    }
}
