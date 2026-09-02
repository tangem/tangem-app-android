package com.tangem.core.ui.utils

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The physical rounding of the device screen's top-left corner, for full-screen surfaces that trace the
 * display edge (e.g. an edge-hugging glow). Falls back to [fallback] below Android 12 or when the system
 * reports no rounding.
 */
@Composable
fun rememberScreenCornerRadius(fallback: Dp = 24.dp): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    return remember(view, density, configuration) {
        val radiusPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
        } else {
            0
        }
        if (radiusPx > 0) with(density) { radiusPx.toDp() } else fallback
    }
}