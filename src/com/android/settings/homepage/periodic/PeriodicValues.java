/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.homepage.periodic;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.text.format.Formatter;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType;
import com.android.settings.deviceinfo.storage.StorageCacheHelper;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.text.NumberFormat;

/**
 * The live values on the periodic home: battery, free storage, Wi-Fi and Bluetooth, dark theme,
 * Do Not Disturb, location and the Catalyxx version, all read from Android. While started it
 * follows their broadcasts and tells its listener when one changes.
 */
final class PeriodicValues {

    private final Context mContext;
    @Nullable
    private final Runnable mOnChange;

    private boolean mStarted;
    private int mBatteryLevel = -1;
    private boolean mCharging;
    private long mFreeBytes = -1;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                readBattery(intent);
            }
            if (mOnChange != null) {
                mOnChange.run();
            }
        }
    };

    /** @param onChange told on the main thread when a value changes while started */
    PeriodicValues(Context context, @Nullable Runnable onChange) {
        mContext = context.getApplicationContext();
        mOnChange = onChange;
        // A battery level now; the broadcast keeps it current once started
        readBattery(mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
        readStorage();
    }

    /** Follows the values' broadcasts. */
    void start() {
        if (mStarted) {
            return;
        }
        mStarted = true;
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
        filter.addAction(LocationManager.MODE_CHANGED_ACTION);
        // All protected broadcasts, so only the system sends them
        readBattery(mContext.registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED));
        readStorage();
    }

    /** Stops following them. */
    void stop() {
        if (!mStarted) {
            return;
        }
        mStarted = false;
        mContext.unregisterReceiver(mReceiver);
    }

    /** Battery level in percent, or -1 if unknown. */
    int getBatteryLevel() {
        return mBatteryLevel;
    }

    boolean isCharging() {
        return mCharging;
    }

    /** The battery level as "87%", or null if unknown. */
    @Nullable
    String getBatteryText() {
        return mBatteryLevel < 0 ? null
                : NumberFormat.getPercentInstance().format(mBatteryLevel / 100.0);
    }

    /** {@code element}'s live value, or null if it has none. */
    @Nullable
    CharSequence getValue(PeriodicElement element) {
        switch (element) {
            case CONNECTIONS: {
                final boolean wifi = isWifiOn();
                final boolean bluetooth = isBluetoothOn();
                return mContext.getString(wifi && bluetooth ? R.string.periodic_value_wifi_bt_on
                        : wifi ? R.string.periodic_value_wifi_on
                        : bluetooth ? R.string.periodic_value_bt_on
                        : R.string.periodic_value_connections_off);
            }
            case POWER: {
                final String battery = getBatteryText();
                if (mFreeBytes < 0) {
                    return battery;
                }
                return mContext.getString(R.string.periodic_value_power,
                        battery != null ? battery : "",
                        Formatter.formatShortFileSize(mContext, mFreeBytes));
            }
            case LOOK:
                return mContext.getString(isDarkTheme()
                        ? R.string.periodic_value_dark : R.string.periodic_value_light);
            case APPS:
                return mContext.getString(isDndOn()
                        ? R.string.periodic_value_dnd_on : R.string.periodic_value_dnd_off);
            case PRIVACY:
                return mContext.getString(isLocationOn()
                        ? R.string.periodic_value_location_on
                        : R.string.periodic_value_location_off);
            case SYSTEM: {
                final String version = SystemProperties.get("ro.catalyxx.version");
                return TextUtils.isEmpty(version) ? null : version;
            }
            default:
                return null;
        }
    }

    private void readBattery(@Nullable Intent battery) {
        if (battery == null) {
            return;
        }
        final int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        mBatteryLevel = level < 0 || scale <= 0 ? -1 : Math.round(level * 100f / scale);
        mCharging = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
    }

    // Like the Storage entry's summary: the size it last cached at once, then a fresh count
    private void readStorage() {
        final int userId = Utils.getCurrentUserIdOfType(
                mContext.getSystemService(UserManager.class), ProfileType.PERSONAL);
        final StorageCacheHelper cache = new StorageCacheHelper(mContext, userId);
        final long cachedUsed = cache.retrieveUsedSize();
        final long cachedTotal = cache.retrieveCachedSize().totalSize;
        if (cachedUsed != 0 && cachedTotal != 0) {
            mFreeBytes = cachedTotal - cachedUsed;
        }
        final StorageManagerVolumeProvider volumes = new StorageManagerVolumeProvider(
                mContext.getSystemService(StorageManager.class));
        ThreadUtils.postOnBackgroundThread(() -> {
            final PrivateStorageInfo info = PrivateStorageInfo.getPrivateStorageInfo(volumes);
            ThreadUtils.postOnMainThread(() -> {
                mFreeBytes = info.freeBytes;
                if (mStarted && mOnChange != null) {
                    mOnChange.run();
                }
            });
        });
    }

    private boolean isWifiOn() {
        final WifiManager wifi = mContext.getSystemService(WifiManager.class);
        return wifi != null && wifi.isWifiEnabled();
    }

    private boolean isBluetoothOn() {
        final BluetoothManager manager = mContext.getSystemService(BluetoothManager.class);
        final BluetoothAdapter adapter = manager != null ? manager.getAdapter() : null;
        return adapter != null && adapter.isEnabled();
    }

    private boolean isDarkTheme() {
        return (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private boolean isDndOn() {
        return mContext.getSystemService(NotificationManager.class)
                .getCurrentInterruptionFilter() > NotificationManager.INTERRUPTION_FILTER_ALL;
    }

    private boolean isLocationOn() {
        return mContext.getSystemService(LocationManager.class).isLocationEnabled();
    }
}
