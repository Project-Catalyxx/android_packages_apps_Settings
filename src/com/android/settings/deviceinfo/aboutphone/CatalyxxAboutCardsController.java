/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.aboutphone;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.usage.StorageStatsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.internal.app.PlatLogoActivity;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.LayoutPreference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * The cards at the top of About phone, after Project Infinity X's OosAboutPreference: the device
 * and its wallpaper, its specs next to the Catalyxx logo, and the build. RAM and storage use are
 * read again each time the page comes back.
 */
public class CatalyxxAboutCardsController extends BasePreferenceController
        implements DefaultLifecycleObserver {

    private static final String TAG = "CatalyxxAboutCards";

    /** From this share of RAM or storage in use, its bar turns red. */
    private static final int USAGE_WARNING_PERCENT = 85;

    private View mRoot;
    private boolean mShowFullKernelVersion;

    public CatalyxxAboutCardsController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference preference = screen.findPreference(getPreferenceKey());
        if (preference == null) {
            return;
        }
        mRoot = preference.findViewById(R.id.catalyxx_about_root);
        bindDevice();
        bindSpecs();
        bindBuild();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mRoot != null) {
            updateUsage();
        }
    }

    private void bindDevice() {
        setText(R.id.catalyxx_about_device_name, CatalyxxDeviceSpecs.getDeviceName(mContext));
        setText(R.id.catalyxx_about_codename, CatalyxxDeviceSpecs.getCodename());
        setText(R.id.catalyxx_about_version, CatalyxxDeviceSpecs.getVersion(mContext));
        final boolean official = CatalyxxDeviceSpecs.isOfficial();
        setText(R.id.catalyxx_about_build_type, mContext.getString(official
                ? R.string.catalyxx_about_official : R.string.catalyxx_about_unofficial));
        mRoot.findViewById(R.id.catalyxx_about_official_dot)
                .setVisibility(official ? View.VISIBLE : View.GONE);
    }

    private void bindSpecs() {
        setItem(R.id.catalyxx_about_processor, R.id.catalyxx_about_processor_value,
                CatalyxxDeviceSpecs.getSoc(mContext));
        setItem(R.id.catalyxx_about_display, R.id.catalyxx_about_display_value,
                CatalyxxDeviceSpecs.getDisplay(mContext));
        final int battery = CatalyxxDeviceSpecs.getBatteryCapacity(mContext);
        setItem(R.id.catalyxx_about_battery, R.id.catalyxx_about_battery_value, battery > 0
                ? mContext.getString(R.string.catalyxx_about_battery_value, battery) : null);
        setItem(R.id.catalyxx_about_camera, R.id.catalyxx_about_camera_value,
                CatalyxxDeviceSpecs.getCamera(mContext));

        final View logo = mRoot.findViewById(R.id.catalyxx_about_logo);
        logo.setOnClickListener(v -> startActivity(v,
                new Intent().setClassName("org.catalyxx.lab", "org.catalyxx.lab.LabActivity")));
        // A long press opens Android's easter egg, like the Android version row
        logo.setOnLongClickListener(v -> startActivity(v, new Intent(Intent.ACTION_MAIN)
                .setClassName("android", PlatLogoActivity.class.getName())));
        // Shrinks while held and bounces back, after Infinity X's Android version badge
        logo.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(200)
                            .setInterpolator(new DecelerateInterpolator()).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(400)
                            .setInterpolator(new OvershootInterpolator(3f)).start();
                    break;
            }
            return false;
        });
    }

    private void bindBuild() {
        setItem(R.id.catalyxx_about_maintainer, R.id.catalyxx_about_maintainer_value,
                SystemProperties.get("ro.catalyxx.maintainer"));
        setItem(R.id.catalyxx_about_android, R.id.catalyxx_about_android_value,
                Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY);
        setItem(R.id.catalyxx_about_security_patch, R.id.catalyxx_about_security_patch_value,
                DeviceInfoUtils.getSecurityPatch());
        final String kernel = DeviceInfoUtils.getFormattedKernelVersion(mContext);
        setItem(R.id.catalyxx_about_kernel, R.id.catalyxx_about_kernel_value, kernel);
        // A tap shows all of /proc/version, with the compiler and the build host
        mRoot.findViewById(R.id.catalyxx_about_kernel).setOnClickListener(v -> {
            mShowFullKernelVersion = !mShowFullKernelVersion;
            setText(R.id.catalyxx_about_kernel_value,
                    mShowFullKernelVersion ? readFullKernelVersion(kernel) : kernel);
        });
    }

    private void updateUsage() {
        ThreadUtils.postOnBackgroundThread(() -> {
            final ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            mContext.getSystemService(ActivityManager.class).getMemoryInfo(memory);

            // The same figures as Settings > Storage
            long storageTotal = 0;
            long storageFree = 0;
            try {
                final StorageStatsManager stats =
                        mContext.getSystemService(StorageStatsManager.class);
                storageTotal = stats.getTotalBytes(StorageManager.UUID_DEFAULT);
                storageFree = stats.getFreeBytes(StorageManager.UUID_DEFAULT);
            } catch (IOException e) {
                Log.w(TAG, "Can't read internal storage", e);
            }
            final long total = storageTotal;
            final long used = storageTotal - storageFree;

            mContext.getMainExecutor().execute(() -> {
                final long ramUsed = memory.totalMem - memory.availMem;
                final long ramSize = CatalyxxDeviceSpecs.getRamSizeGb(memory.totalMem)
                        * 1_000_000_000L;
                setUsage(R.id.catalyxx_about_ram, R.id.catalyxx_about_ram_value,
                        R.id.catalyxx_about_ram_bar,
                        mContext.getString(R.string.catalyxx_about_ram_value,
                                Formatter.formatShortFileSize(mContext, ramSize),
                                Formatter.formatShortFileSize(mContext, ramUsed)),
                        ramUsed, memory.totalMem);
                setUsage(R.id.catalyxx_about_storage, R.id.catalyxx_about_storage_value,
                        R.id.catalyxx_about_storage_bar,
                        mContext.getString(R.string.catalyxx_about_storage_value,
                                Formatter.formatShortFileSize(mContext, used),
                                Formatter.formatShortFileSize(mContext, total)),
                        used, total);
            });
        });
    }

    private void setUsage(int itemId, int valueId, int barId, String value, long used,
            long total) {
        final boolean known = total > 0;
        mRoot.findViewById(itemId).setVisibility(known ? View.VISIBLE : View.GONE);
        if (!known) {
            return;
        }
        setText(valueId, value);
        final int percent = (int) (used * 100 / total);
        final ProgressBar bar = mRoot.findViewById(barId);
        bar.setProgressTintList(percent >= USAGE_WARNING_PERCENT
                ? ColorStateList.valueOf(mContext.getColor(
                        com.android.settingslib.widget.theme.R.color.settingslib_materialColorError))
                : null);
        final ObjectAnimator fill = ObjectAnimator.ofInt(bar, "progress", 0, percent);
        fill.setDuration(1000);
        fill.setInterpolator(new AccelerateDecelerateInterpolator());
        fill.start();
    }

    private void setItem(int itemId, int valueId, @Nullable String value) {
        mRoot.findViewById(itemId)
                .setVisibility(TextUtils.isEmpty(value) ? View.GONE : View.VISIBLE);
        setText(valueId, value);
    }

    private void setText(int viewId, @Nullable String text) {
        final TextView view = mRoot.findViewById(viewId);
        view.setText(text);
        view.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    private static boolean startActivity(View view, Intent intent) {
        try {
            view.getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Nothing to open for " + intent, e);
        }
        return true;
    }

    private static String readFullKernelVersion(String fallback) {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256)) {
            final String line = reader.readLine();
            return TextUtils.isEmpty(line) ? fallback : line;
        } catch (IOException e) {
            Log.w(TAG, "Can't read /proc/version", e);
            return fallback;
        }
    }
}
