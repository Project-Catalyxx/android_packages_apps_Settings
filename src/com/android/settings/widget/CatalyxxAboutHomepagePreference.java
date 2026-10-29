/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.deviceinfo.aboutphone.CatalyxxDeviceSpecs;

/**
 * The device card at the top of the homepage, after Project Infinity X's: the device's name and
 * Catalyxx version over its blurred wallpaper. A tap opens About phone, a long press turns the
 * blur off or back on.
 */
public class CatalyxxAboutHomepagePreference extends HomepagePreference {

    private static final String PREFS = "catalyxx_homepage_about";
    private static final String KEY_BLURRED = "blurred";

    public CatalyxxAboutHomepagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.catalyxx_homepage_about_card);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Context context = getContext();
        final String name = CatalyxxDeviceSpecs.getDeviceName(context);
        final String version = CatalyxxDeviceSpecs.getVersion(context);
        ((TextView) holder.findViewById(R.id.catalyxx_homepage_about_name)).setText(name);
        final TextView versionView =
                (TextView) holder.findViewById(R.id.catalyxx_homepage_about_version);
        versionView.setText(version);
        versionView.setVisibility(TextUtils.isEmpty(version) ? View.GONE : View.VISIBLE);
        holder.itemView.setContentDescription(
                TextUtils.isEmpty(version) ? name : name + ", " + version);

        final SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final CatalyxxWallpaperView wallpaper = (CatalyxxWallpaperView) holder.findViewById(
                R.id.catalyxx_homepage_about_wallpaper);
        wallpaper.setBlurred(prefs.getBoolean(KEY_BLURRED, true));
        holder.itemView.setOnLongClickListener(v -> {
            wallpaper.setBlurred(!wallpaper.isBlurred());
            prefs.edit().putBoolean(KEY_BLURRED, wallpaper.isBlurred()).apply();
            return true;
        });
    }
}
