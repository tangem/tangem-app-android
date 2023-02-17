package com.tangem.core.featuretoggle.comparator

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.tangem.core.featuretoggle.models.Version
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Implementation of component for getting the availability of feature toggle by version
 *
 * @property context application context
 *
 * @author Andrew Khokhlov on 08/02/2023
 */
class VersionContract(@ApplicationContext private val context: Context) {

    private var currentVersion: String? = null

    operator fun invoke(localVersion: String): Boolean {
        if (localVersion == DISABLED_FEATURE_TOGGLE_VERSION) return false
        val current = context.getVersion()?.let(Version::create) ?: return false
        val local = Version.create(localVersion) ?: return false

        return current >= local
    }

    private fun Context.getVersion(): String? {
        if (currentVersion != null) return currentVersion

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                packageManager.getPackageInfo(packageName, 0).versionName
            }
                .substringBefore(VERSION_NAME_DELIMITER)
                .also { currentVersion = it }
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val DISABLED_FEATURE_TOGGLE_VERSION = "undefined"
        const val VERSION_NAME_DELIMITER = "-"
    }
}
