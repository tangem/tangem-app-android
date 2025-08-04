package com.tangem.core.ui.utils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Returns push permission requester.
 * Handles granting permission from app settings.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun requestPermission(permission: String, onAllow: () -> Unit, onDeny: () -> Unit): () -> Unit {
    val permissionState = rememberPermissionState(
        permission = permission,
        onPermissionResult = { isGranted ->
            if (isGranted) {
                onAllow()
            } else {
                onDeny()
            }
        },
    )
    return when {
        permissionState.status.isGranted == false -> {
            if (isRequirePushPermission) {
                permissionState::launchPermissionRequest
            } else {
                onDeny // on versions below Tiramisu call onDeny directly and then open settings
            }
        }
        else -> {
            onAllow
        }
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
private val isRequirePushPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU