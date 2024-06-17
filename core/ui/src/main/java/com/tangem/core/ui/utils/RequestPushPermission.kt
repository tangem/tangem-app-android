package com.tangem.core.ui.utils

import android.Manifest
import android.os.Build
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
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun requestPushPermission(
    isFirstTimeAsking: Boolean,
    isClicked: MutableState<Boolean>,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    onOpenSettings: () -> Unit,
): () -> Unit {
    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val tempPermissionState = rememberPermissionState(permission = permission)
        rememberPermissionState(permission = permission) {
            when {
                it -> onAllow()
                !tempPermissionState.status.shouldShowRationale && !isFirstTimeAsking -> onOpenSettings()
                else -> onDeny()
            }
        }
    } else {
        null
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