package com.tangem.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
fun requestPushPermission(
    pushPermission: String?,
    isClicked: MutableState<Boolean>,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
): () -> Unit {
    val permissionState = pushPermission?.let { permission ->
        rememberPermissionState(permission = permission)
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
        {}
    } else {
        permissionState::launchPermissionRequest
    }
}