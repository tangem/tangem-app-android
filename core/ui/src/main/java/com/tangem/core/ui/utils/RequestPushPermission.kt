package com.tangem.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Returns push permission requester.
 * Handles granting permission from app settings.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun requestPushPermission(
    isFirstTimeAsking: Boolean,
    pushPermission: String?,
    isClicked: MutableState<Boolean>,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    onOpenSettings: () -> Unit,
): () -> Unit {
    val permissionState = pushPermission?.let { permission ->
        val tempPermissionState = rememberPermissionState(permission = permission)
        rememberPermissionState(permission = permission) {
            val isGranted = tempPermissionState.status.isGranted
            val shouldShowRationale = tempPermissionState.status.shouldShowRationale

            if (isGranted && !shouldShowRationale && !isFirstTimeAsking) onOpenSettings()
        }
    }

    // Check if user granted permission and close bottom sheet
    LaunchedEffect(key1 = permissionState?.status, isClicked) {
        if (!isClicked.value) return@LaunchedEffect
        if (permissionState?.status?.isGranted == true) {
            onAllow()
        } else {
            onDeny()
        }
    }

    return if (permissionState == null) {
        onOpenSettings
    } else {
        permissionState::launchPermissionRequest
    }
}
