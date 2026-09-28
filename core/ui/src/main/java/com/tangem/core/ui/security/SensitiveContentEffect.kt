package com.tangem.core.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Protects secrets (seed phrase, PIN, access code) shown in the current composition: blocks screenshots and
 * hides the window content from non-tool accessibility services.
 *
 * Call it inside the composition that renders the secret. A modal bottom sheet or dialog lives in a window
 * of its own, so the call must go into the sheet's content, not next to the sheet call.
 */
@Composable
fun SensitiveContentEffect() {
    if (LocalInspectionMode.current) return

    DisableScreenshotsDisposableEffect()
    HideFromAccessibilityServicesEffect()
}