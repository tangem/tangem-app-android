package com.tangem.features.pushnotifications.api.utils

import android.Manifest
import android.os.Build

val PUSH_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Manifest.permission.POST_NOTIFICATIONS
} else {
    "android.permission.POST_NOTIFICATIONS"
}