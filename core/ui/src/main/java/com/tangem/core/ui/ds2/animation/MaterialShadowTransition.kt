package com.tangem.core.ui.ds2.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.tangem.core.ui.res.LocalMaterialShadowEnabled

/**
 * Runs [content] with the material drop shadow suppressed while this enter/exit transition animates.
 *
 * A running transition draws its content into a graphics layer, which clips drawing to the content's
 * bounding box. A material surface paints its shadow outside its own bounds, so the clip turns the soft
 * halo into a hard rectangle for the length of the animation. See [LocalMaterialShadowEnabled].
 */
@Composable
internal fun AnimatedVisibilityScope.WithoutMaterialShadowWhileAnimating(content: @Composable () -> Unit) {
    // AND-ed with the current value so an outer animation keeps the shadow off while an inner one idles.
    val isShadowEnabled = LocalMaterialShadowEnabled.current && !transition.isRunning
    CompositionLocalProvider(LocalMaterialShadowEnabled provides isShadowEnabled) {
        content()
    }
}