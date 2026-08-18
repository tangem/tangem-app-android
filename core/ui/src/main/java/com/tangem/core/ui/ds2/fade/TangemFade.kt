@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.fade

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint

private const val HARD_ALPHA = 0.95f
private const val SOFT_ALPHA = 0.6f
private val HARD_SOLID_HEIGHT = 40.dp
private val HARD_BOTTOM_GRADIENT_HEIGHT = 24.dp
private val BLUR_RADIUS = 20.dp

/**
 * Design-system fade overlay used at the top or bottom edge of scrollable content.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=2272-21336)
 *
 * @param position which edge the fade is anchored to — controls the gradient direction.
 * @param variant [TangemFade.Variant.Hard] combines a short gradient band with an opaque block for
 *   a harder cut-off — 40dp opaque at the edge for [TangemFade.Position.Top], and for
 *   [TangemFade.Position.Bottom] a 24dp gradient band with the remaining height opaque;
 *   [TangemFade.Variant.Soft] is a gradient-only fade across the whole height, gentler.
 * @param blur when `true`, the content under the fade is blurred via Haze (radius 20dp,
 *   progressive intensity matching [position]).
 * @param backgroundColor base color of the fade gradient — defaults to `colors3.bg.primary` so
 *   the fade blends with the standard page background.
 * @param modifier modifier applied to the fade's root. The fade has no intrinsic height — the
 *   call site must set it here (e.g. `Modifier.height(…)` or `Modifier.matchParentSize()`),
 *   otherwise the fade renders with zero height.
 */
@Composable
fun TangemFade(
    position: TangemFade.Position,
    modifier: Modifier = Modifier,
    variant: TangemFade.Variant = TangemFade.Variant.Soft,
    blur: Boolean = false,
    backgroundColor: Color = TangemTheme.colors3.bg.primary,
) {
    val hazeState = LocalHazeState.current

    val blurModifier = if (blur) {
        Modifier.hazeEffectTangem(state = hazeState) {
            blurRadius = BLUR_RADIUS
            fallbackTint = HazeTint(Color.Transparent)
            progressive = HazeProgressive.verticalGradient(
                startIntensity = if (position == TangemFade.Position.Top) 1f else 0f,
                endIntensity = if (position == TangemFade.Position.Top) 0f else 1f,
                preferPerformance = true,
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(blurModifier)
            .drawWithCache {
                val brush = buildBrush(
                    color = backgroundColor,
                    position = position,
                    variant = variant,
                    solidRatio = edgeRatio(HARD_SOLID_HEIGHT),
                    bottomGradientRatio = edgeRatio(HARD_BOTTOM_GRADIENT_HEIGHT),
                )
                onDrawBehind { drawRect(brush) }
            },
    )
}

private fun CacheDrawScope.edgeRatio(edgeHeight: Dp): Float = if (size.height > 0f) {
    (edgeHeight.toPx() / size.height).coerceAtMost(maximumValue = 1f)
} else {
    1f
}

private fun buildBrush(
    color: Color,
    position: TangemFade.Position,
    variant: TangemFade.Variant,
    solidRatio: Float,
    bottomGradientRatio: Float,
): Brush {
    val opaque = color.copy(alpha = HARD_ALPHA)
    val soft = color.copy(alpha = SOFT_ALPHA)
    val transparent = color.copy(alpha = 0f)

    return when (variant) {
        TangemFade.Variant.Hard -> when (position) {
            TangemFade.Position.Top -> Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to opaque,
                    solidRatio to opaque,
                    1f to transparent,
                ),
            )
            TangemFade.Position.Bottom -> Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to transparent,
                    0.3f * bottomGradientRatio to color.copy(alpha = 0.4f),
                    0.7f * bottomGradientRatio to color.copy(alpha = 0.85f),
                    bottomGradientRatio to opaque,
                    1f to opaque,
                ),
            )
        }
        TangemFade.Variant.Soft -> when (position) {
            TangemFade.Position.Top -> Brush.verticalGradient(colors = listOf(soft, transparent))
            TangemFade.Position.Bottom -> Brush.verticalGradient(colors = listOf(transparent, soft))
        }
    }
}

object TangemFade {
    enum class Position { Top, Bottom }
    enum class Variant { Hard, Soft }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemFade_Preview() {
    TangemThemePreviewRedesign {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary)
                .padding(12.dp),
        ) {
            TangemFade.Variant.entries.forEach { variant ->
                TangemFade.Position.entries.forEach { position ->
                    FadePreviewRow(variant = variant, position = position)
                }
            }
        }
    }
}

@Composable
private fun FadePreviewRow(variant: TangemFade.Variant, position: TangemFade.Position) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "${variant.name} / ${position.name}",
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(TangemTheme.colors3.bg.accent.blue),
        ) {
            TangemFade(
                position = position,
                variant = variant,
                modifier = when (variant) {
                    TangemFade.Variant.Hard -> Modifier.matchParentSize()
                    TangemFade.Variant.Soft -> Modifier
                        .height(96.dp)
                        .align(
                            when (position) {
                                TangemFade.Position.Top -> Alignment.TopCenter
                                TangemFade.Position.Bottom -> Alignment.BottomCenter
                            },
                        )
                },
            )
        }
    }
}