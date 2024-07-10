package com.tangem.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.tangem.core.ui.res.TangemTheme

/**
 * A composable that draws a fade effect at the bottom of the screen. Used on screens with a list of repeating
 * elements and floating button at the bottom of the screen.
 */
@Composable
fun BottomFade(modifier: Modifier = Modifier, backgroundColor: Color = TangemTheme.colors.background.secondary) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size100 + bottomBarHeight)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        backgroundColor,
                    ),
                ),
            ),
    )
}
