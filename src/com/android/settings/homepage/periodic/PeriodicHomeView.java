/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.homepage.periodic;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.deviceinfo.aboutphone.CatalyxxDeviceSpecs;
import com.android.settings.homepage.periodic.PeriodicElement.Family;
import com.android.settings.widget.CatalyxxAboutHomepagePreference;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The periodic Settings home, in place of the homepage list: the eclipse card, the Catalyxx Lab
 * bar, then one strip per element. It's built from the homepage's own entries, the ones its
 * controllers leave visible and the rows apps add, and taps open them as the list would.
 */
public class PeriodicHomeView extends LinearLayout {

    /** Opens what the home's cards stand for. */
    public interface Host {
        /** Opens a homepage entry, as a tap on its row would. */
        void openEntry(Preference entry);

        /** Opens the page of an element that holds more than one entry. */
        void openElement(PeriodicElement element, int number);
    }

    private final Host mHost;
    private final PeriodicValues mValues;
    private final Map<PeriodicElement, Strip> mStrips = new EnumMap<>(PeriodicElement.class);
    @Nullable
    private EclipseView mEclipse;
    @Nullable
    private View mEclipseCard;
    @Nullable
    private String mBuiltFrom;

    // The parts of a strip that change with its live value
    private static final class Strip {
        final View view;
        final TextView value;
        final CharSequence name;
        final CharSequence summary;

        Strip(View view, CharSequence name, CharSequence summary) {
            this.view = view;
            this.value = view.findViewById(R.id.periodic_value);
            this.name = name;
            this.summary = summary;
        }
    }

    public PeriodicHomeView(Context context, Host host) {
        super(context);
        mHost = host;
        mValues = new PeriodicValues(context, this::updateValues);
        setOrientation(VERTICAL);
        final int side = getResources().getDimensionPixelSize(R.dimen.periodic_side_margin);
        setPadding(side, 0, side, side);
    }

    /** Follows the live values while the home is on screen. */
    public void start() {
        mValues.start();
        updateValues();
    }

    public void stop() {
        mValues.stop();
    }

    /**
     * Builds the home from {@code screen}'s visible entries, or only refreshes its values if
     * they're the same entries as last time.
     */
    public void bind(PreferenceGroup screen) {
        final List<Preference> entries = new ArrayList<>();
        collectVisibleEntries(screen, entries);
        // What the cards show of the entries: their titles, and the Lab's summary. Summaries
        // that controllers keep current, like the battery's, don't need a rebuild.
        final StringBuilder builtFrom = new StringBuilder();
        for (Preference entry : entries) {
            builtFrom.append(entry.getKey()).append('\n').append(entry.getTitle()).append('\n');
            if (PeriodicElement.LAB_KEY.equals(entry.getKey())) {
                builtFrom.append(entry.getSummary()).append('\n');
            }
        }
        if (!builtFrom.toString().equals(mBuiltFrom)) {
            build(entries);
            mBuiltFrom = builtFrom.toString();
        }
        updateValues();
    }

    // Leaf entries in their order on the homepage, through its groups
    private static void collectVisibleEntries(PreferenceGroup group, List<Preference> out) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            final Preference preference = group.getPreference(i);
            if (!preference.isVisible()) {
                continue;
            }
            if (preference instanceof PreferenceGroup) {
                collectVisibleEntries((PreferenceGroup) preference, out);
            } else {
                out.add(preference);
            }
        }
    }

    private void build(List<Preference> entries) {
        removeAllViews();
        mStrips.clear();
        Preference deviceCard = null;
        Preference lab = null;
        final Map<PeriodicElement, List<Preference>> held = new EnumMap<>(PeriodicElement.class);
        for (Preference entry : entries) {
            final String key = entry.getKey();
            if (PeriodicElement.DEVICE_CARD_KEY.equals(key)) {
                deviceCard = entry;
            } else if (PeriodicElement.LAB_KEY.equals(key)) {
                lab = entry;
            } else {
                held.computeIfAbsent(PeriodicElement.forKey(key), e -> new ArrayList<>())
                        .add(entry);
            }
        }

        final PeriodicColors colors = new PeriodicColors(getContext());
        addEclipseCard(colors, deviceCard);
        if (lab != null) {
            addLabStrip(colors, lab);
        }
        // Numbered in reading order among the elements this phone has
        int number = 0;
        for (PeriodicElement element : PeriodicElement.values()) {
            final List<Preference> elementEntries = held.get(element);
            if (elementEntries != null) {
                addElementStrip(colors, element, ++number, elementEntries);
            }
        }
    }

    private void addEclipseCard(PeriodicColors colors, @Nullable Preference deviceCard) {
        final Context context = getContext();
        final View card = LayoutInflater.from(context)
                .inflate(R.layout.catalyxx_periodic_eclipse_card, this, false);
        card.setBackground(card(colors.cardFill(), colors.cardBorder(),
                getResources().getDimension(R.dimen.periodic_eclipse_card_radius)));
        card.setClipToOutline(true);

        mEclipse = card.findViewById(R.id.periodic_eclipse);
        mEclipse.setColors(colors.accent(), colors.ring(), colors.track());
        ((TextView) card.findViewById(R.id.periodic_device_name))
                .setText(CatalyxxDeviceSpecs.getDeviceName(context));
        final TextView version = card.findViewById(R.id.periodic_device_version);
        final String versionText = CatalyxxDeviceSpecs.getVersion(context);
        version.setText(versionText);
        version.setTextColor(colors.ring());
        version.setVisibility(TextUtils.isEmpty(versionText) ? GONE : VISIBLE);

        if (deviceCard != null) {
            // The device card opens About phone
            card.setOnClickListener(v -> mHost.openEntry(deviceCard));
        } else {
            card.setClickable(false);
        }
        card.setOnLongClickListener(
                v -> CatalyxxAboutHomepagePreference.toggleSystemBlur(v.getContext()));
        mEclipseCard = card;
        addCard(card);
    }

    private void addLabStrip(PeriodicColors colors, Preference lab) {
        final View strip = inflateStrip(colors, Family.LAB, PeriodicElement.LAB_SYMBOL,
                PeriodicElement.LAB_NUMBER, lab.getTitle(), lab.getSummary());
        strip.setOnClickListener(v -> mHost.openEntry(lab));
        strip.setContentDescription(describe(lab.getTitle(), lab.getSummary()));
        addCard(strip);
    }

    private void addElementStrip(PeriodicColors colors, PeriodicElement element, int number,
            List<Preference> entries) {
        final List<CharSequence> titles = new ArrayList<>();
        for (Preference entry : entries) {
            if (!TextUtils.isEmpty(entry.getTitle())) {
                titles.add(entry.getTitle());
            }
        }
        final CharSequence name = getContext().getText(element.nameRes);
        final CharSequence summary = TextUtils.join(", ", titles);
        final View strip = inflateStrip(colors, element.family, element.symbol, number, name,
                summary);
        strip.setOnClickListener(v -> {
            if (entries.size() == 1) {
                // Nothing to choose from: straight to the entry
                mHost.openEntry(entries.get(0));
            } else {
                mHost.openElement(element, number);
            }
        });
        mStrips.put(element, new Strip(strip, name, summary));
        addCard(strip);
    }

    private View inflateStrip(PeriodicColors colors, Family family, String symbol, int number,
            CharSequence name, CharSequence summary) {
        final Resources res = getResources();
        final View strip = LayoutInflater.from(getContext())
                .inflate(R.layout.catalyxx_periodic_strip, this, false);
        strip.setBackground(card(colors.fill(family), colors.border(family),
                res.getDimension(R.dimen.periodic_strip_radius)));
        strip.setClipToOutline(true);

        final TextView symbolView = strip.findViewById(R.id.periodic_symbol);
        symbolView.setText(symbol);
        symbolView.setTextColor(colors.ink(family));
        symbolView.setBackground(card(colors.badgeFill(family), colors.badgeBorder(family),
                res.getDimension(R.dimen.periodic_badge_radius)));

        final TextView numberView = strip.findViewById(R.id.periodic_number);
        numberView.setText(NumberFormat.getIntegerInstance().format(number));
        numberView.setTextColor(colors.ink(family));
        ((TextView) strip.findViewById(R.id.periodic_value)).setTextColor(colors.ink(family));

        ((TextView) strip.findViewById(R.id.periodic_name)).setText(name);
        final TextView summaryView = strip.findViewById(R.id.periodic_summary);
        summaryView.setText(summary);
        summaryView.setVisibility(TextUtils.isEmpty(summary) ? GONE : VISIBLE);
        return strip;
    }

    private void addCard(View card) {
        final LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        if (getChildCount() > 0) {
            params.topMargin = getResources().getDimensionPixelSize(R.dimen.periodic_gap);
        }
        addView(card, params);
    }

    private void updateValues() {
        final boolean live = PeriodicHome.showsLiveValues(getContext());
        for (Map.Entry<PeriodicElement, Strip> entry : mStrips.entrySet()) {
            final Strip strip = entry.getValue();
            final CharSequence value = live ? mValues.getValue(entry.getKey()) : null;
            strip.value.setText(value);
            strip.value.setVisibility(TextUtils.isEmpty(value) ? INVISIBLE : VISIBLE);
            strip.view.setContentDescription(
                    describe(strip.name, TextUtils.isEmpty(value) ? strip.summary : value));
        }
        if (mEclipse != null && mEclipseCard != null) {
            final String battery = mValues.getBatteryText();
            mEclipse.setBattery(mValues.getBatteryLevel(), battery, mValues.isCharging());
            final Context context = getContext();
            final String version = CatalyxxDeviceSpecs.getVersion(context);
            String description = context.getString(R.string.periodic_eclipse_description,
                    CatalyxxDeviceSpecs.getDeviceName(context),
                    version != null ? version : "", battery != null ? battery : "");
            if (mValues.isCharging()) {
                description += ", " + context.getString(R.string.periodic_eclipse_charging);
            }
            mEclipseCard.setContentDescription(description);
        }
    }

    private CharSequence describe(CharSequence name, @Nullable CharSequence details) {
        if (TextUtils.isEmpty(details)) {
            return name;
        }
        return getContext().getString(R.string.periodic_element_description, name, details);
    }

    /** A rounded card of {@code fill}, outlined in {@code border}. */
    static GradientDrawable card(@ColorInt int fill, @ColorInt int border, float radius) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setStroke(Math.round(Resources.getSystem().getDisplayMetrics().density),
                border);
        drawable.setCornerRadius(radius);
        return drawable;
    }
}
