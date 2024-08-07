package com.tangem.core.featuretoggle.version

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.tangem.utils.StringsSigns.MINUS
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
        return runCatching { getVersionName().substringBefore(VERSION_NAME_DELIMITER) }
            .fold(onSuccess = { it }, onFailure = { null })
    }

    private fun getVersionName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager
                .getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
                .versionName
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }
    }

    private companion object {
        const val VERSION_NAME_DELIMITER = MINUS
    }
}