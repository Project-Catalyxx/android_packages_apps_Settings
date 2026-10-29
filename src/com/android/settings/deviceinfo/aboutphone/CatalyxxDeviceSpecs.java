/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.aboutphone;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.Display;

import androidx.annotation.Nullable;

import com.android.internal.os.PowerProfile;
import com.android.settings.R;

/**
 * What the About phone cards and the homepage device card say about the device and the build,
 * after Project Infinity X's AboutPhoneData. A device names itself, its chip and its cameras with
 * the config_catalyxx_device_* strings, an overlay in vendor/catalyxx/products; its battery and
 * display are read from the device.
 */
public final class CatalyxxDeviceSpecs {

    /** RAM sizes as sold, in GB. The kernel's total, less what firmware keeps, rounds up to one. */
    private static final int[] RAM_SIZES_GB = {2, 3, 4, 6, 8, 10, 12, 16, 18, 20, 24, 32, 48, 64};

    private CatalyxxDeviceSpecs() {}

    /** The retail name, e.g. "OnePlus 11". */
    public static String getDeviceName(Context context) {
        final String name = context.getString(R.string.config_catalyxx_device_name);
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        final String marketName = SystemProperties.get("ro.product.marketname");
        return TextUtils.isEmpty(marketName) ? Build.MODEL : marketName;
    }

    /** The codename, e.g. "salami". */
    public static String getCodename() {
        return SystemProperties.get("ro.lineage.device", Build.DEVICE);
    }

    /**
     * The Catalyxx version and release codename, e.g. "Catalyxx 17.0 Chlorine", or null on a
     * build without them, such as plain LineageOS from the same tree.
     */
    @Nullable
    public static String getVersion(Context context) {
        final String version = SystemProperties.get("ro.catalyxx.version");
        if (TextUtils.isEmpty(version)) {
            return null;
        }
        return context.getString(R.string.catalyxx_about_version, version,
                SystemProperties.get("ro.catalyxx.codename")).trim();
    }

    /** Whether this is an official build. */
    public static boolean isOfficial() {
        return "OFFICIAL".equals(SystemProperties.get("ro.lineage.releasetype"));
    }

    /** The chip, e.g. "Snapdragon 8 Gen 2"; without an overlay, the SoC model, e.g. "SM8550". */
    public static String getSoc(Context context) {
        final String soc = context.getString(R.string.config_catalyxx_device_soc);
        if (!TextUtils.isEmpty(soc)) {
            return soc;
        }
        return Build.UNKNOWN.equals(Build.SOC_MODEL)
                ? SystemProperties.get("ro.board.platform") : Build.SOC_MODEL;
    }

    /** The cameras, e.g. "50 MP + 48 MP + 32 MP", or null when the device doesn't say. */
    @Nullable
    public static String getCamera(Context context) {
        final String camera = context.getString(R.string.config_catalyxx_device_camera);
        return TextUtils.isEmpty(camera) ? null : camera;
    }

    /** The screen's full resolution and fastest refresh rate, e.g. "1440 × 3216 · 120 Hz". */
    @Nullable
    public static String getDisplay(Context context) {
        final Display display = context.getSystemService(DisplayManager.class)
                .getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            return null;
        }
        // The largest mode is the panel's own resolution, whichever one the user picked
        int width = 0;
        int height = 0;
        float refreshRate = 0;
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() * mode.getPhysicalHeight() > width * height) {
                width = mode.getPhysicalWidth();
                height = mode.getPhysicalHeight();
            }
            refreshRate = Math.max(refreshRate, mode.getRefreshRate());
        }
        if (width == 0 || height == 0) {
            return null;
        }
        return context.getString(R.string.catalyxx_about_display_value,
                Math.min(width, height), Math.max(width, height), Math.round(refreshRate));
    }

    /** The battery's design capacity in mAh, or 0 when the power profile doesn't give one. */
    public static int getBatteryCapacity(Context context) {
        // AOSP's own power profile says 1000, a placeholder
        final double capacity = new PowerProfile(context).getBatteryCapacity();
        return capacity > 1000 ? (int) Math.round(capacity) : 0;
    }

    /** The RAM size as sold, in GB, e.g. 16 for a kernel total of 14.8 GiB. */
    public static int getRamSizeGb(long totalBytes) {
        for (int size : RAM_SIZES_GB) {
            if (size * (1L << 30) >= totalBytes) {
                return size;
            }
        }
        return (int) Math.ceil(totalBytes / (double) (1L << 30));
    }
}
