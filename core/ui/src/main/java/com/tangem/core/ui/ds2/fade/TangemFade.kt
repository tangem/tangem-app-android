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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint

private const val SOLID_RATIO = 40f / 96f
private const val HARD_ALPHA = 0.95f
private const val SOFT_ALPHA = 0.6f
private val FADE_HEIGHT = 96.dp
private val BLUR_RADIUS = 20.dp

/**
 * Design-system fade overlay used at the top or bottom edge of scrollable content.
 *
 * Renders a 96dp-tall gradient (and optional solid block in the [TangemFade.Variant.Hard]
 * variant) tinted with `colors3.bg.primary`, so it blends with the page background and masks
 * content as it scrolls under the edge. Place it inside a [Box] and align with
 * [androidx.compose.ui.Alignment.TopCenter] / [androidx.compose.ui.Alignment.BottomCenter]
 * depending on [position].
 *
 * @param position which edge the fade is anchored to — controls the gradient direction.
 * @param variant [TangemFade.Variant.Hard] adds an opaque solid block next to the edge for a
 *   harder cut-off, [TangemFade.Variant.Soft] is gradient-only and gentler.
 * @param blur when `true`, the content under the fade is blurred via Haze (radius 20dp,
 *   progressive intensity matching [position]).
 * @param backgroundColor base color of the fade gradient — defaults to `colors3.bg.primary` so
 *   the fade blends with the standard page background.
 * @param modifier modifier applied to the fade's root.
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
    val brush = buildBrush(color = backgroundColor, position = position, variant = variant)

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
            .height(FADE_HEIGHT)
            .then(blurModifier)
            .background(brush),
    )
}

private fun buildBrush(color: Color, position: TangemFade.Position, variant: TangemFade.Variant): Brush {
    val opaque = color.copy(alpha = HARD_ALPHA)
    val soft = color.copy(alpha = SOFT_ALPHA)
    val transparent = Color.Transparent

    return when (variant) {
        TangemFade.Variant.Hard -> when (position) {
            TangemFade.Position.Top -> Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to opaque,
                    SOLID_RATIO to opaque,
                    1f to transparent,
                ),
            )
            TangemFade.Position.Bottom -> Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to transparent,
                    1f - SOLID_RATIO to opaque,
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
            style = TangemTheme.typography.caption2,
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
                modifier = Modifier.align(
                    when (position) {
                        TangemFade.Position.Top -> Alignment.TopCenter
                        TangemFade.Position.Bottom -> Alignment.BottomCenter
                    },
                ),
            )
        }
    }
}