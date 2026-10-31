/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.homepage.periodic;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.homepage.TopLevelSettings;
import com.android.settings.homepage.periodic.PeriodicElement.Family;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.support.SupportPreferenceController;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.Tile;

import java.text.NumberFormat;

/**
 * An element's page on the periodic Settings home: the homepage entries the element holds, as
 * rows, under its badge and live value. It loads the homepage itself and keeps only those
 * entries, so their controllers, icons and the pages behind them are the homepage's own.
 */
public class PeriodicElementFragment extends DashboardFragment {

    private static final String TAG = "PeriodicElementFragment";
    private static final String ARG_ELEMENT = "periodic_element";
    private static final String ARG_NUMBER = "periodic_number";

    private PeriodicElement mElement = PeriodicElement.EXTRAS;
    private int mNumber;
    @Nullable
    private View mHeader;

    /** The arguments that open {@code element}'s page; {@code number} is its number. */
    public static Bundle args(PeriodicElement element, int number) {
        final Bundle args = new Bundle();
        args.putString(ARG_ELEMENT, element.name());
        args.putInt(ARG_NUMBER, number);
        return args;
    }

    @Override
    public void onAttach(Context context) {
        final Bundle args = getArguments();
        if (args != null) {
            try {
                mElement = PeriodicElement.valueOf(args.getString(ARG_ELEMENT, ""));
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Unknown element, showing Extras", e);
            }
            mNumber = args.getInt(ARG_NUMBER);
        }
        super.onAttach(context);
        // As the homepage does, for the support entry when there is one
        final SupportPreferenceController support = use(SupportPreferenceController.class);
        if (support != null) {
            support.setActivity(getActivity());
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return TopLevelSettings.getPreferenceLayoutResId(getContext());
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DASHBOARD_SUMMARY;
    }

    // The rows apps add to the homepage, for Extras
    @Override
    public String getCategoryKey() {
        return CategoryKey.CATEGORY_HOMEPAGE;
    }

    @Override
    protected boolean displayTile(Tile tile) {
        if (!super.displayTile(tile)) {
            return false;
        }
        final String key = FeatureFactory.getFeatureFactory().getDashboardFeatureProvider()
                .getDashboardKeyForTile(tile);
        return PeriodicElement.forKey(key) == mElement;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        keepElementEntries(getPreferenceScreen());
    }

    // Removes the entries the element doesn't hold, and the groups that leaves empty. Removed
    // rather than hidden: the controllers would show a hidden entry again.
    private void keepElementEntries(PreferenceGroup group) {
        for (int i = group.getPreferenceCount() - 1; i >= 0; i--) {
            final Preference preference = group.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                final PreferenceGroup child = (PreferenceGroup) preference;
                keepElementEntries(child);
                if (child.getPreferenceCount() == 0) {
                    group.removePreference(child);
                }
            } else if (PeriodicElement.forKey(preference.getKey()) != mElement) {
                group.removePreference(preference);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mHeader = setPinnedHeaderView(R.layout.catalyxx_periodic_element_header);
        bindHeader();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindHeader();
    }

    private void bindHeader() {
        if (mHeader == null) {
            return;
        }
        final Context context = getContext();
        final PeriodicColors colors = new PeriodicColors(context);
        final Family family = mElement.family;
        mHeader.findViewById(R.id.periodic_header_badge).setBackground(PeriodicHomeView.card(
                colors.fill(family), colors.border(family),
                getResources().getDimension(R.dimen.periodic_header_badge_radius)));

        final TextView number = mHeader.findViewById(R.id.periodic_header_number);
        number.setText(mNumber > 0 ? NumberFormat.getIntegerInstance().format(mNumber) : null);
        number.setTextColor(colors.ink(family));
        final TextView symbol = mHeader.findViewById(R.id.periodic_header_symbol);
        symbol.setText(mElement.symbol);
        symbol.setTextColor(colors.ink(family));

        final CharSequence value = PeriodicHome.showsLiveValues(context)
                ? new PeriodicValues(context, null).getValue(mElement) : null;
        final TextView valueView = mHeader.findViewById(R.id.periodic_header_value);
        valueView.setText(value);
        valueView.setTextColor(colors.ink(family));
        valueView.setVisibility(TextUtils.isEmpty(value) ? View.GONE : View.VISIBLE);
    }
}
