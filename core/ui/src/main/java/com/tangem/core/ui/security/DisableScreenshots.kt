package com.tangem.core.ui.security

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.tangem.core.ui.utils.findActivity
import timber.log.Timber

/**
 * Disables screenshots for the current composition.
 *
 * @see <a href = "https://developer.android.com/jetpack/androidx/releases/activity?hl=ru#1.10.0">Migration</a>
 */
@Composable
fun DisableScreenshotsDisposableEffect() {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity) {
        Timber.d("Security mode: enabled")
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        onDispose {
            Timber.d("Security mode: disabled")
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}