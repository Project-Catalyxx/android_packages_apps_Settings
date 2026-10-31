/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.homepage.periodic;

import android.content.Context;
import android.provider.Settings;

/**
 * Catalyxx's periodic Settings home: the homepage drawn as a periodic table of elements, each
 * holding a few homepage entries. It's picked in Catalyxx Lab (User interface > Settings home),
 * which writes these per-user Settings.System values under the same keys; Classic, Android's
 * own homepage, is the default.
 */
public final class PeriodicHome {

    /** The homepage style: {@link #STYLE_CLASSIC} or {@link #STYLE_STRIPS}. */
    public static final String STYLE = "catalyxx_settings_home_style";
    /** Android's own homepage. */
    public static final int STYLE_CLASSIC = 0;
    /** The periodic home, one element per row. */
    public static final int STYLE_STRIPS = 1;

    /** 1 (the default) shows each element's live value, 0 names only. */
    public static final String LIVE_VALUES = "catalyxx_settings_home_live_values";

    private PeriodicHome() {}

    /** The homepage style now; Classic for a value this build doesn't know. */
    public static int getStyle(Context context) {
        final int style =
                Settings.System.getInt(context.getContentResolver(), STYLE, STYLE_CLASSIC);
        return style == STYLE_STRIPS ? style : STYLE_CLASSIC;
    }

    /** Whether elements show their live values. */
    public static boolean showsLiveValues(Context context) {
        return Settings.System.getInt(context.getContentResolver(), LIVE_VALUES, 1) != 0;
    }
}
