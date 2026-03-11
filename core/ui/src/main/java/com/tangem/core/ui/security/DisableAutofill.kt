package com.tangem.core.ui.security

import android.os.Build
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Disables autofill for the current Compose view hierarchy.
 *
 * This prevents password managers and other autofill services from accessing
 * sensitive content (e.g., seed phrases, private keys) entered in text fields.
 *
 * Must be called within a Composable scope before the text fields that need protection.
 *
 * The original autofill setting is restored when this composable leaves the composition.
 */
@Composable
fun DisableAutofillEffect() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val view = LocalView.current
        DisposableEffect(view) {
            val previousValue = view.importantForAutofill
            view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            onDispose {
                view.importantForAutofill = previousValue
            }
        }
    }
}