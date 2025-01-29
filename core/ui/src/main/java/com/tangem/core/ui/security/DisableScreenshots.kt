package com.tangem.core.ui.security

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.tangem.core.ui.utils.findActivity

/**
 * Disables screenshots for the current composition.
 */
@Composable
fun DisableScreenshotsDisposableEffect() {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}