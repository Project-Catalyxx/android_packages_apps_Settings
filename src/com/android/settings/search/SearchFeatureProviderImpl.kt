/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.android.settings.search

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.provider.Settings
import com.android.settings.search.SearchIndexableResourcesFactory.createSearchIndexableResources
import com.android.settings.spa.search.SettingsSpaSearchRepository
import com.android.settingslib.search.SearchIndexableResources

/** FeatureProvider for the refactored search code. */
open class SearchFeatureProviderImpl : SearchFeatureProvider {
    private val lazySearchIndexableResources by lazy {
        createSearchIndexableResources().apply {
            for (indexableData in SettingsSpaSearchRepository().getSearchIndexableDataList()) {
                addIndex(indexableData)
            }
        }
    }

    override fun verifyLaunchSearchResultPageCaller(context: Context, callerPackage: String) {
        require(callerPackage.isNotEmpty()) {
            "ExternalSettingsTrampoline intents must be called with startActivityForResult"
        }
        val isSettingsPackage = callerPackage == context.packageName
        if (isSettingsPackage ||
            callerPackage == getSettingsIntelligencePkgName(context) ||
            isSignatureAllowlisted(context, callerPackage) ||
            isSystemLauncher(context, callerPackage)) {
            return
        }
        throw SecurityException(
            "Search result intents must be called with from an allowlisted package: $callerPackage"
        )
    }

    override fun getSearchIndexableResources(): SearchIndexableResources =
        lazySearchIndexableResources

    override fun buildSearchIntent(context: Context, pageId: Int): Intent =
        Intent(Settings.ACTION_APP_SEARCH_SETTINGS)
            .setPackage(getSettingsIntelligencePkgName(context))
            .putExtra(Intent.EXTRA_REFERRER, buildReferrer(context, pageId))

    protected open fun isSignatureAllowlisted(context: Context, callerPackage: String): Boolean =
        false

    /**
     * Catalyxx: the launchers QuickSwitch can make the home app (Launcher3, the Pixel Launcher,
     * Lawnchair), when they are system apps. Their "all set" page at the end of setup opens
     * System navigation through a search result: Google's Settings lets the Pixel Launcher do
     * that, AOSP's lets no launcher.
     */
    private fun isSystemLauncher(context: Context, callerPackage: String): Boolean {
        val launchers =
            Resources.getSystem()
                .getStringArray(com.android.internal.R.array.config_launcherPackages)
        if (callerPackage !in launchers) return false
        return try {
            val appInfo = context.packageManager.getApplicationInfo(callerPackage, 0)
            appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        private fun buildReferrer(context: Context, pageId: Int): Uri =
            Uri.Builder()
                .scheme("android-app")
                .authority(context.packageName)
                .path(pageId.toString())
                .build()
    }
}
