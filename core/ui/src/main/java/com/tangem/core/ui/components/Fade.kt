package com.tangem.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalPowerSavingState
import com.tangem.core.ui.res.TangemTheme
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

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

/**
 * A composable that draws a fade effect at the bottom of the screen. Used on screens with a list of repeating
 * elements and floating button at the bottom of the screen.
 */
@Composable
fun BottomFade(gradientBrush: Brush, modifier: Modifier = Modifier) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size100 + bottomBarHeight)
            .background(gradientBrush),
    )
}

/**
 * A composable that draws a fade effect at the bottom of the screen. Same as [BottomFade]
 * but with a vertical blur.
 */
@Composable
fun BottomFadeWithBlur(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    hazeState: HazeState = LocalHazeState.current,
) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    val isPowerSavingState by LocalPowerSavingState.current.isPowerSavingModeEnabled.collectAsState()

    if (isPowerSavingState) {
        BottomFade(backgroundColor = backgroundColor, modifier = modifier)
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size100 + bottomBarHeight)
            .hazeEffectTangem(
                state = hazeState,
            ) {
                blurRadius = 20.dp
                progressive = HazeProgressive.verticalGradient(
                    startIntensity = 0f,
                    endIntensity = 1f,
                    preferPerformance = true,
                )
            },
    )
}

/**
 * A composable that draws a fade effect at the right end of the screen. Same as [HorizontalFade]
 * but with blur.
 */
@Composable
fun HorizontalFadeWithBlur(backgroundColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .hazeEffectTangem(
                style = HazeStyle(
                    blurRadius = 20.dp,
                    tint = HazeTint(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor,
                            ),
                        ),
                    ),
                    backgroundColor = Color.Transparent,
                ),
            ) {
                progressive =
                    HazeProgressive.horizontalGradient(
                        startIntensity = 0f,
                        endIntensity = 1f,
                        preferPerformance = true,
                    )
            },
    )
}

/**
 * A composable that draws a fade effect. Used on screens with a list of repeating
 * elements and floating button at the bottom of the screen.
 */
@Composable
fun Fade(
    modifier: Modifier = Modifier,
    backgroundColor: Color = TangemTheme.colors.background.secondary,
    height: Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
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