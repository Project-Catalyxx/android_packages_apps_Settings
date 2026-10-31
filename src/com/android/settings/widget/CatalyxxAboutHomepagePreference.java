/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget;

import static android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.deviceinfo.aboutphone.CatalyxxDeviceSpecs;

/**
 * The device card at the top of the homepage, after Project Infinity X's: the device's name and
 * Catalyxx version over its wallpaper. A tap opens About phone. The wallpaper is blurred while
 * system blur is on, and a long press turns system blur off or back on, the same switch as
 * Accessibility > Color and motion > Reduce blur effects.
 */
public class CatalyxxAboutHomepagePreference extends HomepagePreference {

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

        ((CatalyxxWallpaperView) holder.findViewById(R.id.catalyxx_homepage_about_wallpaper))
                .setFollowsSystemBlur(true);
        holder.itemView.setOnLongClickListener(v -> toggleSystemBlur(v.getContext()));
    }

    /**
     * Turns system blur off or back on; the wallpaper follows once the system has. The periodic
     * home's eclipse card does the same on a long press.
     */
    public static boolean toggleSystemBlur(Context context) {
        if (!CROSS_WINDOW_BLUR_SUPPORTED) {
            return false;
        }
        if (context.getSystemService(PowerManager.class).isPowerSaveMode()) {
            // Battery Saver keeps blur off, and greys out the Accessibility switch too
            Toast.makeText(context, R.string.blur_switch_disabled_summary, Toast.LENGTH_SHORT)
                    .show();
            return true;
        }
        final ContentResolver resolver = context.getContentResolver();
        final boolean enable =
                Settings.Global.getInt(resolver, Settings.Global.DISABLE_WINDOW_BLURS, 0) != 0;
        Settings.Global.putInt(resolver, Settings.Global.DISABLE_WINDOW_BLURS, enable ? 0 : 1);
        Toast.makeText(context, enable
                ? R.string.catalyxx_homepage_blur_on : R.string.catalyxx_homepage_blur_off,
                Toast.LENGTH_SHORT).show();
        return true;
    }
}
