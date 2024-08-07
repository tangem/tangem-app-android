package com.tangem.core.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.res.*
import com.valentinilk.shimmer.*

/**
 * Rectangle shimmer item with rounded shape from DS
 */
@Composable
fun RectangleShimmer(modifier: Modifier = Modifier, radius: Dp = TangemTheme.dimens.radius6) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(size = radius))
            .shimmer(LocalTangemShimmer.current),
    )
}

/**
 * Circle shimmer item
 * Size should be set in modifier
 */
@Composable
fun CircleShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .shimmer(LocalTangemShimmer.current),
    )
}

internal val TangemShimmer: Shimmer
    @Composable
    get() = rememberShimmer(
        shimmerBounds = ShimmerBounds.View,
        theme = defaultShimmerTheme.copy(
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 800,
                    easing = LinearEasing,
                    delayMillis = 800,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            shaderColors = TangemShimmerColors,
            blendMode = BlendMode.Src,
            shaderColorStops = null,
        ),
    )

private val TangemShimmerColors: List<Color>
    @Composable
    @ReadOnlyComposable
    get() {
        val isInDarkTheme = LocalIsInDarkTheme.current

        return buildList {
            if (isInDarkTheme) {
                TangemColorPalette.Dark3.let(::add)
                TangemColorPalette.Dark4.let(::add)
                TangemColorPalette.Dark6.let(::add)
                TangemColorPalette.Dark4.let(::add)
                TangemColorPalette.Dark3.let(::add)
            } else {
                TangemColorPalette.Light2.let(::add)
                TangemColorPalette.Light1.let(::add)
                TangemColorPalette.White.let(::add)
                TangemColorPalette.Light1.let(::add)
                TangemColorPalette.Light2.let(::add)
            }
        }
    }

// region preview

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ShimmersPreview() {
    TangemThemePreview {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors.background.primary),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing18),
        ) {
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TangemTheme.dimens.size24),
            )
            CircleShimmer(modifier = Modifier.size(size = TangemTheme.dimens.size42))
        }
    }
}
// endregion preview