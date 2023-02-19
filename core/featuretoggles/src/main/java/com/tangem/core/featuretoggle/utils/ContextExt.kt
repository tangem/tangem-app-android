package com.tangem.core.featuretoggle.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

private const val VERSION_NAME_DELIMITER = "-"

/** Get application version */
internal fun Context.getVersion(): String? {
    val versionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
    } else {
        packageManager.getPackageInfo(packageName, 0).versionName
    }

    return runCatching { versionName.substringBefore(VERSION_NAME_DELIMITER) }
        .fold(
            onSuccess = { it },
            onFailure = { null },
        )
}
