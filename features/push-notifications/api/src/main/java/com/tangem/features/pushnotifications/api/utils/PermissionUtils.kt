package com.tangem.features.pushnotifications.api.utils

import android.Manifest
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
private val isRequirePushPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

val PUSH_PERMISSION = if (isRequirePushPermission) {
    Manifest.permission.POST_NOTIFICATIONS
} else {
    "android.permission.POST_NOTIFICATIONS"
}

fun getPushPermissionOrNull() = if (isRequirePushPermission) {
    Manifest.permission.POST_NOTIFICATIONS
} else {
    null
}