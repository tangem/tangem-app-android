package com.tangem.tap.google

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.tangem.google.auth.GoogleAuthActivityResultBridge

/**
 * Invisible host that owns the [ActivityResultContracts.StartIntentSenderForResult] launcher and
 * wires it into [GoogleAuthActivityResultBridge] for the lifetime of the composition. Mounted once
 * at the app root so any Google authorization flow can drive the system consent screen.
 */
@Composable
fun GoogleAuthLauncherHost(bridge: GoogleAuthActivityResultBridge) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = bridge::onResult,
    )

    DisposableEffect(launcher) {
        bridge.registerLauncher(launcher)
        onDispose { bridge.registerLauncher(null) }
    }
}