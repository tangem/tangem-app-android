package com.tangem.core.featuretoggle.version

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Implementation of application version provider
 *
 * @property context application context
 */
internal class DefaultVersionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : VersionProvider {

    override fun get(): String? {
        val versionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager
                .getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
                .versionName
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }

        return runCatching { versionName.substringBefore(VERSION_NAME_DELIMITER) }
            .fold(onSuccess = { it }, onFailure = { null })
    }

    private companion object {
        const val VERSION_NAME_DELIMITER = "-"
    }
}