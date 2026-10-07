package com.tangem.core.ui.security

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private class AccessibilityDataSensitivity {
    private var count = 0

    val isReleased: Boolean get() = count == 0

    fun enable(host: View) {
        if (count == 0) {
            host.setAccessibilityDataSensitive(View.ACCESSIBILITY_DATA_SENSITIVE_YES)
        }
        count++
    }

    fun release(host: View) {
        count--
        if (count == 0) {
            // The platform exposes no getter for the mode, only the resolved boolean, so restore the default.
            host.setAccessibilityDataSensitive(View.ACCESSIBILITY_DATA_SENSITIVE_AUTO)
        }
    }
}

// Keyed by view rather than held in a CompositionLocal: the flag lives on the view hosting the composition,
// so a dialog or bottom sheet window needs an entry of its own. The value must not hold the view — a strong
// edge back to the key would pin the weak entry and leak the window.
private val sensitivities = WeakHashMap<View, AccessibilityDataSensitivity>()

/**
 * Hides the current window's content from accessibility services that are not genuine accessibility tools
 * (`isAccessibilityTool="false"`): automation, password managers, parental control and the like.
 * TalkBack, Switch Access and other assistive services keep working.
 *
 * No-op below Android 14, where the platform offers no per-service filtering.
 */
@Composable
internal fun HideFromAccessibilityServicesEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return

    val view = LocalView.current
    DisposableEffect(view) {
        val sensitivity = sensitivities.getOrPut(view) { AccessibilityDataSensitivity() }
        sensitivity.enable(view)
        onDispose {
            sensitivity.release(view)
            if (sensitivity.isReleased) sensitivities.remove(view)
        }
    }
}