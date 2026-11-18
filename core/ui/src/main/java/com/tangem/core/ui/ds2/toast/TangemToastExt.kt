@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.toast

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_success_20

internal val ToastShowEasing: Easing = CubicBezierEasing(a = 0.4f, b = 1.15f, c = 0.8f, d = 1f)

internal val ToastShrinkEasing: Easing = CubicBezierEasing(a = 0.3f, b = -0.05f, c = 0.65f, d = -0.2f)

internal fun TangemToast.Duration.toMillis(): Long = when (this) {
    TangemToast.Duration.Short -> 2000L
    TangemToast.Duration.Long -> 3000L
}

internal data class ToastIconTokens(val icon: ImageVector?, val tint: Color)

@Composable
@ReadOnlyComposable
internal fun TangemToast.Appearance.iconTokens(neutralIcon: ImageVector?): ToastIconTokens {
    val icons = TangemTheme.colors3.icon
    return when (this) {
        TangemToast.Appearance.Neutral -> ToastIconTokens(icon = neutralIcon, tint = icons.primary)
        TangemToast.Appearance.Error -> ToastIconTokens(icon = Icons.ic_error_20, tint = icons.status.error)
        TangemToast.Appearance.Info -> ToastIconTokens(icon = Icons.ic_info_20, tint = icons.status.info)
        TangemToast.Appearance.Success -> ToastIconTokens(icon = Icons.ic_success_20, tint = icons.status.success)
    }
}