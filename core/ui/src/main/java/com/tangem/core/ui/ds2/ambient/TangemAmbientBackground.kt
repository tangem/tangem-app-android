@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.ambient

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.TangemColorPalette

/**
 * Ambient light: two soft coloured lights bled behind a surface to tint it. Fills the space it is given
 * and paints nothing else, so it goes at the bottom of a container's stack (typically
 * `Modifier.matchParentSize()` inside a `Box`) and is clipped by whatever shape that container has.
 *
 * The lights are radial gradients rather than blurred shapes, so they cost one draw call each and work
 * on every API level. Their positions and sizes come from [placement]: a light may well be centred
 * outside the container — only its falloff reaches inside.
 *
 * @param variant Hue of the light.
 * @param modifier Modifier for the container; must carry the size to light up.
 * @param placement Where the two lights sit relative to the container and how wide they spread.
 * @param progress `0f..1f` — how far the lights have arrived: at `0f` nothing is painted, at `1f` they
 * are at full strength and size. A lambda, not a value, so an entrance animation redraws without
 * recomposing; the default is a fully arrived, static light.
 */
@Composable
fun TangemAmbientBackground(
    variant: TangemAmbientBackground.Variant,
    modifier: Modifier = Modifier,
    placement: TangemAmbientBackground.Placement = TangemAmbientBackground.Placement.Screen,
    progress: () -> Float = { 1f },
) {
    Box(
        modifier = modifier.drawBehind {
            val settled = progress().coerceIn(0f, 1f)
            drawLight(light = placement.far, color = variant.farColor, settled = settled)
            drawLight(light = placement.near, color = variant.nearColor, settled = settled)
        },
    )
}

private fun DrawScope.drawLight(light: TangemAmbientBackground.Light, color: Color, settled: Float) {
    val scale = lerp(start = 0.4f, stop = 1f, fraction = settled)
    val radius = (light.diameter.toPx() / 2f + light.blurRadius.toPx()) * scale
    val origin = light.originIn(width = size.width, height = size.height, density = this)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = light.maxAlpha * settled), Color.Transparent),
            center = origin,
            radius = radius,
        ),
        radius = radius,
        center = origin,
    )
}

object TangemAmbientBackground {

    /**
     * Hue of the light. The colors are palette primitives rather than semantic tokens: the design pins
     * them to `palette/<hue>/70` (far light) and `/40` (near light) identically in both themes.
     */
    enum class Variant(internal val farColor: Color, internal val nearColor: Color) {
        Neutral(farColor = TangemColorPalette.Neutral.`70`, nearColor = TangemColorPalette.Neutral.`40`),
        Green(farColor = TangemColorPalette.Green.`70`, nearColor = TangemColorPalette.Green.`40`),
        Red(farColor = TangemColorPalette.Red.`70`, nearColor = TangemColorPalette.Red.`40`),
        Yellow(farColor = TangemColorPalette.Yellow.`70`, nearColor = TangemColorPalette.Yellow.`40`),
        Blue(farColor = TangemColorPalette.Blue.`70`, nearColor = TangemColorPalette.Blue.`40`),
        Violet(farColor = TangemColorPalette.Violet.`70`, nearColor = TangemColorPalette.Violet.`40`),
    }

    /** Where the two lights sit and how far they spread. */
    enum class Placement(internal val far: Light, internal val near: Light) {

        /** Full screen: a dim wash from the top start corner, a bright one just off the top end corner. */
        Screen(
            far = Light(
                diameter = 360.dp,
                blurRadius = 256.dp,
                maxAlpha = 0.1f,
                anchor = Anchor.TopStart,
                offset = DpOffset(x = 116.dp, y = 116.dp),
            ),
            near = Light(
                diameter = 360.dp,
                blurRadius = 192.dp,
                maxAlpha = 0.4f,
                anchor = Anchor.TopEnd,
                offset = DpOffset(x = 32.dp, y = 32.dp),
            ),
        ),

        /** Card-sized surface: both lights sit below the bottom start corner and wash up into it. */
        Card(
            far = Light(
                diameter = 176.dp,
                blurRadius = 88.dp,
                maxAlpha = 0.1f,
                anchor = Anchor.BottomStart,
                offset = DpOffset(x = 24.dp, y = 88.dp),
            ),
            near = Light(
                diameter = 176.dp,
                blurRadius = 64.dp,
                maxAlpha = 0.4f,
                anchor = Anchor.BottomStart,
                offset = DpOffset(x = (-54).dp, y = 88.dp),
            ),
        ),
    }

    /** Corner the light's [Light.offset] is measured from; the offset runs right and down from it. */
    internal enum class Anchor { TopStart, TopEnd, BottomStart }

    /**
     * One light.
     *
     * @property diameter width of the light at full strength, before its falloff
     * @property blurRadius how far the falloff reaches past [diameter]
     * @property maxAlpha strength of the light's core once fully arrived
     */
    internal data class Light(
        val diameter: Dp,
        val blurRadius: Dp,
        val maxAlpha: Float,
        val anchor: Anchor,
        val offset: DpOffset,
    ) {

        fun originIn(width: Float, height: Float, density: Density): Offset = with(density) {
            when (anchor) {
                Anchor.TopStart -> Offset(x = offset.x.toPx(), y = offset.y.toPx())
                Anchor.TopEnd -> Offset(x = width + offset.x.toPx(), y = offset.y.toPx())
                Anchor.BottomStart -> Offset(x = offset.x.toPx(), y = height + offset.y.toPx())
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, widthDp = 402, heightDp = 640)
@Preview(showBackground = true, widthDp = 402, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemAmbientBackgroundScreenPreview() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors3.bg.primary),
        ) {
            TangemAmbientBackground(
                variant = TangemAmbientBackground.Variant.Violet,
                placement = TangemAmbientBackground.Placement.Screen,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 640)
@Preview(showBackground = true, widthDp = 402, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemAmbientBackgroundCardPreview() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors3.bg.primary)
                .padding(all = 24.dp),
        ) {
            TangemAmbientBackground.Variant.entries.take(n = 3).forEachIndexed { index, variant ->
                Box(
                    modifier = Modifier
                        .padding(top = (index * 200).dp)
                        .size(width = 200.dp, height = 180.dp)
                        .clip(shape = RoundedCornerShape(size = 20.dp))
                        .background(TangemTheme.colors3.bg.secondary),
                ) {
                    TangemAmbientBackground(
                        variant = variant,
                        placement = TangemAmbientBackground.Placement.Card,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

// endregion