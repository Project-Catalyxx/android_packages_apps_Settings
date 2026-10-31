/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.homepage.periodic;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.settings.R;

import java.util.List;

/**
 * The elements of the periodic Settings home, in reading order. Each holds the homepage entries
 * of one area, by their keys in top_level_settings_expressive.xml. Extras holds every entry not
 * listed, such as the rows apps add (OnePlus settings, Google, Digital Wellbeing), so none goes
 * missing.
 */
public enum PeriodicElement {
    CONNECTIONS("Cn", R.string.periodic_cn_name, R.string.periodic_cn_name, Family.CONNECTIONS,
            "top_level_network", "top_level_connected_devices"),
    POWER("Ps", R.string.periodic_ps_name, R.string.periodic_ps_title, Family.POWER,
            "top_level_battery", "top_level_storage"),
    LOOK("Ls", R.string.periodic_ls_name, R.string.periodic_ls_name, Family.LOOK,
            "top_level_display", "top_level_wallpaper", "top_level_sound", "top_level_communal"),
    APPS("Ap", R.string.periodic_ap_name, R.string.periodic_ap_name, Family.APPS,
            "top_level_apps", "top_level_notifications", "top_level_priority_modes"),
    PRIVACY("Pr", R.string.periodic_pr_name, R.string.periodic_pr_title, Family.PRIVACY,
            "top_level_safety_center", "top_level_security", "top_level_privacy",
            "top_level_location", "top_level_accounts", "top_level_accounts_and_backup",
            "top_level_supervision"),
    SYSTEM("Sy", R.string.periodic_sy_name, R.string.periodic_sy_title, Family.SYSTEM,
            "top_level_system", "top_level_emergency", "top_level_about_device",
            "top_level_support"),
    ACCESSIBILITY("Ac", R.string.periodic_ac_name, R.string.periodic_ac_name, Family.SYSTEM,
            "top_level_accessibility"),
    EXTRAS("Ex", R.string.periodic_ex_name, R.string.periodic_ex_name, Family.EXTRAS);

    /** The color families, one color each in a palette; the Lab glows in the theme accent. */
    public enum Family { CONNECTIONS, POWER, LOOK, APPS, PRIVACY, SYSTEM, EXTRAS, LAB }

    /** Catalyxx Lab's homepage row, keyed by its activity; the home shows it on its own. */
    public static final String LAB_KEY = "dashboard_tile_pref_org.catalyxx.lab.LabActivity";
    /** The Lab's element number: 17, chlorine's, after Catalyxx's codename. */
    public static final int LAB_NUMBER = 17;
    /** The Lab's symbol. */
    public static final String LAB_SYMBOL = "Cx";
    /** The homepage device card, which the eclipse card stands in for. */
    public static final String DEVICE_CARD_KEY = "top_level_catalyxx_about";

    public final String symbol;
    @StringRes
    public final int nameRes;
    @StringRes
    public final int titleRes;
    public final Family family;
    private final List<String> mKeys;

    PeriodicElement(String symbol, @StringRes int nameRes, @StringRes int titleRes,
            Family family, String... keys) {
        this.symbol = symbol;
        this.nameRes = nameRes;
        this.titleRes = titleRes;
        this.family = family;
        mKeys = List.of(keys);
    }

    /**
     * The element holding the homepage entry with {@code key}: Extras for any entry not listed,
     * and null for the Lab and the device card, which the home shows on their own.
     */
    @Nullable
    public static PeriodicElement forKey(@Nullable String key) {
        if (LAB_KEY.equals(key) || DEVICE_CARD_KEY.equals(key)) {
            return null;
        }
        for (PeriodicElement element : values()) {
            if (element.mKeys.contains(key)) {
                return element;
            }
        }
        return EXTRAS;
    }
}
