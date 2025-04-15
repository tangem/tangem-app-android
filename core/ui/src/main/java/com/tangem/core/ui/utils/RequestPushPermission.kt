package com.tangem.core.ui.utils

import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Returns push permission requester.
 * Handles granting permission from app settings.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun requestPushPermission(pushPermission: String?, onAllow: () -> Unit, onDeny: () -> Unit): () -> Unit {
    val permissionState = pushPermission?.let { permission ->
        rememberPermissionState(
            permission = permission,
            onPermissionResult = { isGranted ->
                if (isGranted) {
                    onAllow()
                } else {
                    onDeny()
                }
            },
        )
    }

    return if (permissionState == null) {
        {}
    } else {
        permissionState::launchPermissionRequest
    }
}