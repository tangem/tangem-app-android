package com.tangem.core.ui.security

import android.os.Build
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap

private class AutofillSuppression(private val originalImportance: Int) {
    private var count = 0

    val isReleased: Boolean get() = count == 0

    fun suppress(host: View) {
        if (count == 0) {
            host.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        count++
    }

    fun release(host: View) {
        count--
        if (count == 0) {
            host.importantForAutofill = originalImportance
        }
    }
}

// Keyed by view rather than held in a CompositionLocal: `importantForAutofill` is resolved within the
// window that owns the view, so a dialog needs a suppression of its own. The value must not hold the
// view — a strong edge back to the key would pin the weak entry and leak the window.
private val suppressions = WeakHashMap<View, AutofillSuppression>()

/**
 * Disables autofill for the current window.
 *
 * Without an explicit content type the autofill heuristics take a masked field — a pin, an access code, a
 * seed phrase word — for a password one and offer the credentials stored for the app in it.
 */
@Composable
fun DisableAutofillEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val view = LocalView.current
    DisposableEffect(view) {
        // `AndroidComposeView` hardcodes `getImportantForAutofill()` to `IMPORTANT_FOR_AUTOFILL_YES`, so the
        // flag only takes effect on the plain view above it, which resolving a field's importance walks up to.
        val host = view.parent as? View
        if (host == null) return@DisposableEffect onDispose { }

        val suppression = suppressions.getOrPut(host) { AutofillSuppression(host.importantForAutofill) }
        suppression.suppress(host)
        onDispose {
            suppression.release(host)
            if (suppression.isReleased) suppressions.remove(host)
        }
    }
}