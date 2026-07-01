package com.tangem.core.ui.ds2.surface

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.softLayerShadow
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

/**
 * Design-system v2 surface: a clipped, optionally-bordered, optionally-clickable container that
 * renders either as a flat colored surface or as a translucent "material" surface backed by a
 * haze blur effect.
 *
 * Rendering modes:
 * - **Flat** (`isMaterial = false`, default): a solid [color] background clipped to [shape].
 * - **Material** (`isMaterial = true`): the [color] parameter is ignored. The surface adds a soft
 *   drop shadow and a gradient stroke (`material.border`), then renders a haze-blurred backdrop
 *   tinted with `material.fill.blur`. When `LocalHazeState.blurEnabled` is `false` (e.g.
 *   previews, low-end devices), the surface falls back to opaque `material.fill.solid` overlaid
 *   with translucent `material.tint.solid` so both layers remain visible.
 *
 * Interaction:
 * - When [onClick] is non-null the surface is clickable. The v2 ripple configuration is provided
 *   via [LocalRippleConfiguration] for the [content] subtree as well.
 * - [enabled] only gates the click handler — disabled surfaces don't change appearance here;
 *   callers are expected to handle visual disabled state themselves (e.g. via alpha).
 *
 * @param color Background color used in flat mode. Ignored when [isMaterial] is `true`.
 * @param isMaterial Switches to the haze-based translucent rendering.
 * @param border Optional outer stroke. Drawn underneath the material gradient stroke when both
 *   are present.
 * @param shape Shape used for clipping, background, and borders.
 * @param onClick Click handler. `null` makes the surface non-interactive.
 * @param enabled Forwarded to the click handler.

 * @param content Content rendered inside the clipped surface.
 */
@Suppress("UnsafeCallOnNullableType", "")
@Composable
@NonRestartableComposable
fun TangemSurface(
    modifier: Modifier = Modifier,
    color: Color = TangemTheme.colors3.bg.primary,
    isMaterial: Boolean = false,
    border: BorderStroke? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    shadowRadius: Dp = 40.dp,
    content: @Composable () -> Unit,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    val surface: @Composable () -> Unit = {
        Box(
            modifier = modifier
                .conditionalCompose(isMaterial) { materialShadow(shape, shadowRadius) }
                .conditionalCompose(border != null) { border(border!!, shape) }
                .conditionalCompose(isMaterial) { materialBorder(shape) }
                .clip(shape)
                .background(if (isMaterial) Color.Transparent else color, shape)
                .conditionalCompose(isMaterial) { materialFill() }
                .conditionalCompose(onClick != null) {
                    clickable(
                        interactionSource = resolvedInteractionSource,
                        indication = LocalIndication.current,
                        enabled = enabled,
                        onClick = onClick!!,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }

    if (onClick != null) {
        CompositionLocalProvider(LocalRippleConfiguration provides tangemSurfaceRipple()) {
            surface()
        }
    } else {
        surface()
    }
}

// region material rendering

/**
 * Drop shadow for the material variant.
 *
 * The material fill is translucent, so the shadow is clipped to the area outside [shape] (via
 * `isAlphaContentClip`) to avoid the dark blur bleeding through the surface.
 */
@Composable
private fun Modifier.materialShadow(shape: Shape, radius: Dp): Modifier = softLayerShadow(
    radius = radius,
    color = Color.Black.copy(alpha = 0.12f),
    shape = shape,
    spread = 0.dp,
    offset = DpOffset(x = 0.dp, y = 8.dp),
    isAlphaContentClip = true,
)

/** Diagonal gradient stroke that wraps the material variant. */
@Composable
private fun Modifier.materialBorder(shape: Shape): Modifier = border(
    width = 1.dp,
    brush = materialBorderBrush(),
    shape = shape,
)

/**
 * Translucent fill for the material variant.
 *
 * When the haze state is enabled, paints a haze-blurred backdrop. When disabled, layers two
 * solid colors so the result still reads as "tinted fill" instead of going transparent.
 *
 * Reads [LocalHazeState]'s `blurEnabled` so the solid fallback is applied whenever blur is off —
 * otherwise the haze modifier's `fallbackTint = HazeTint(Color.Transparent)` would leave the
 * surface fully transparent.
 */
@Composable
private fun Modifier.materialFill(): Modifier {
    val isBlurEnabled = LocalHazeState.current.blurEnabled
    val material = TangemTheme.colors3.material
    val hazed = hazeEffectTangem(
        style = HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = 32.dp,
            tints = listOf(
                HazeTint(material.fill.blur),
            ),
        ),
    ) {
        fallbackTint = HazeTint(Color.Transparent)
    }
    return hazed.conditionalCompose(!isBlurEnabled) {
        // Paint the opaque fill first, then layer the translucent tint on top so both are visible.
        background(material.fill.solid)
            .background(material.tint.solid)
    }
}

@Suppress("MagicNumber")
@Composable
@ReadOnlyComposable
private fun materialBorderBrush(): Brush {
    val border = TangemTheme.colors3.material.border
    val startColor = border.start
    val midColor = Color.Transparent
    val endColor = border.end
    return object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            val w = size.width
            val h = size.height
            val k = 2f * w * h / (w * w + h * h)
            return LinearGradientShader(
                from = Offset.Zero,
                to = Offset(x = k * h, y = k * w),
                colors = listOf(startColor, midColor, midColor, endColor),
                colorStops = listOf(0f, 0.40f, 0.60f, 1f),
            )
        }
    }
}

// endregion

@Composable
@ReadOnlyComposable
private fun tangemSurfaceRipple(): RippleConfiguration = RippleConfiguration(
    color = TangemTheme.colors3.interaction.press.default,
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0f,
        focusedAlpha = 0f,
        hoveredAlpha = 0.05f,
        pressedAlpha = 0.1f,
    ),
)