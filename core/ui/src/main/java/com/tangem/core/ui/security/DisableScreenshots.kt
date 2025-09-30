package com.tangem.core.ui.security

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.tangem.core.ui.utils.findActivity

private val LocalSecureFlagController = staticCompositionLocalOf<SecureFlagController> {
    error("No SecureFlagController provided")
}

private class SecureFlagController(private val activity: Activity) {
    private var count by mutableIntStateOf(0)

    fun enable() {
        if (count == 0) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        count++
    }

    fun disable() {
        count--
        if (count == 0) {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

@Composable
fun ProvideSecureFlagController(content: @Composable () -> Unit) {
    val activity = LocalContext.current.findActivity()
    val controller = remember(activity) { SecureFlagController(activity) }

    CompositionLocalProvider(
        LocalSecureFlagController provides controller,
        content = content,
    )
}

/**
 * Disables screenshots for the current composition.
 *
 * @see <a href = "https://developer.android.com/jetpack/androidx/releases/activity?hl=ru#1.10.0">Migration</a>
 */
@Composable
fun DisableScreenshotsDisposableEffect() {
    val secureFlagController = LocalSecureFlagController.current

    DisposableEffect(secureFlagController) {
        secureFlagController.enable()
        onDispose { secureFlagController.disable() }
    }
}