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
import com.tangem.core.ui.res.LocalMaterialShadowEnabled
import com.tangem.core.ui.res.LocalRootBackgroundColor
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
 *   drop shadow (omitted on dark backdrops, where it is not representable in 8 bits and would show
 *   as a banding ring) and a gradient stroke (`material.border`), then renders a haze-blurred backdrop
 *   tinted with `material.fill.blur`. When `LocalHazeState.blurEnabled` is `false` (e.g.
 *   previews, low-end devices), the surface falls back to opaque `material.fill.solid` overlaid
 *   with translucent `material.tint.solid` so both layers remain visible. [materialStyle] picks the
 *   token set all of the above is read from.
 *
 * Interaction:
 * - When [onClick] is non-null the surface is clickable. The v2 ripple configuration is provided
 *   via [LocalRippleConfiguration] for the [content] subtree as well.
 * - [enabled] only gates the click handler — disabled surfaces don't change appearance here;
 *   callers are expected to handle visual disabled state themselves (e.g. via alpha).
 *
 * @param color Background color used in flat mode. Ignored when [isMaterial] is `true`.
 * @param isMaterial Switches to the haze-based translucent rendering.
 * @param materialStyle Material token set used when [isMaterial] is `true`. `Inverted` renders the
 *   material of the opposite theme — dark in light theme and light in dark theme. Ignored in flat
 *   mode. See [TangemSurface.MaterialStyle].
 * @param border Optional outer stroke. Drawn underneath the material gradient stroke when both
 *   are present.
 * @param shape Shape used for clipping, background, and borders.
 * @param onClick Click handler. `null` makes the surface non-interactive.
 * @param enabled Forwarded to the click handler.

 * @param shadowRadius Blur size of the material drop shadow, expressed as a Figma `box-shadow`
 *   blur. Ignored when [isMaterial] is `false`.
 * @param content Content rendered inside the clipped surface.
 */
@Suppress("UnsafeCallOnNullableType", "")
@Composable
@NonRestartableComposable
fun TangemSurface(
    modifier: Modifier = Modifier,
    color: Color = TangemTheme.colors3.bg.primary,
    isMaterial: Boolean = false,
    materialStyle: TangemSurface.MaterialStyle = TangemSurface.MaterialStyle.Default,
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
                .conditionalCompose(isMaterial) { materialBorder(shape = shape, style = materialStyle) }
                .clip(shape)
                .background(if (isMaterial) Color.Transparent else color, shape)
                .conditionalCompose(isMaterial) { materialFill(style = materialStyle) }
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
        val isInverseSurface = isInverseSurface(color = color, isMaterial = isMaterial, style = materialStyle)
        CompositionLocalProvider(LocalRippleConfiguration provides tangemSurfaceRipple(isInverseSurface)) {
            surface()
        }
    } else {
        surface()
    }
}

object TangemSurface {

    /**
     * Material token set the material rendering mode reads its fill, tint and stroke from.
     *
     * - [Default] — material of the current theme (`colors3.material`).
     * - [Inverted] — material of the opposite theme (`colors3.materialInverted`): dark in light
     *   theme, light in dark theme.
     */
    enum class MaterialStyle {
        Default,
        Inverted,
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
private fun Modifier.materialShadow(shape: Shape, radius: Dp): Modifier {
    // Inside a transient graphics layer the halo below would be clipped to this surface's bounding
    // box and read as a hard rectangle around it, so the layer's owner opts the shadow out.
    if (!LocalMaterialShadowEnabled.current) return this

    // Approximates the backdrop: a material surface sitting on a non-root background keeps the
    // root's verdict, which is accurate enough since only near-black backgrounds are rejected.
    val backdrop by LocalRootBackgroundColor.current
    if (!backdrop.canRenderShadow(MATERIAL_SHADOW_ALPHA)) return this

    val isBlurEnabled = LocalHazeState.current.blurEnabled
    return softLayerShadow(
        radius = radius,
        color = Color.Black.copy(alpha = MATERIAL_SHADOW_ALPHA),
        shape = shape,
        spread = 0.dp,
        offset = DpOffset(x = 0.dp, y = 8.dp),
        isAlphaContentClip = isBlurEnabled,
    )
}

/**
 * Whether a black shadow of [shadowAlpha] is representable over this color, i.e. whether it spans
 * at least [MIN_SHADOW_QUANTIZATION_STEPS] steps of the 8-bit channel range.
 */
private fun Color.canRenderShadow(shadowAlpha: Float): Boolean =
    maxOf(red, green, blue) * shadowAlpha * MAX_CHANNEL_VALUE >= MIN_SHADOW_QUANTIZATION_STEPS

private const val MATERIAL_SHADOW_ALPHA = 0.12f
private const val MIN_SHADOW_QUANTIZATION_STEPS = 3f
private const val MAX_CHANNEL_VALUE = 255f

/** Diagonal gradient stroke that wraps the material variant. */
@Composable
private fun Modifier.materialBorder(shape: Shape, style: TangemSurface.MaterialStyle): Modifier = border(
    width = 1.dp,
    brush = materialBorderBrush(style),
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
private fun Modifier.materialFill(style: TangemSurface.MaterialStyle): Modifier {
    val isBlurEnabled = LocalHazeState.current.blurEnabled
    val material = materialColors(style)
    val hazed = hazeEffectTangem(
        style = HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = 32.dp,
            tints = listOf(
                HazeTint(material.fillBlur),
            ),
        ),
    ) {
        fallbackTint = HazeTint(Color.Transparent)
    }
    return hazed.conditionalCompose(!isBlurEnabled) {
        // Paint the opaque fill first, then layer the translucent tint on top so both are visible.
        background(material.fillSolid)
            .background(material.tintSolid)
    }
}

/**
 * Material colors of the requested [style]. `colors3.material` and `colors3.materialInverted` are
 * generated as unrelated types, so the needed colors are copied into a common holder.
 */
@Composable
@ReadOnlyComposable
private fun materialColors(style: TangemSurface.MaterialStyle): MaterialColors {
    val colors = TangemTheme.colors3
    return when (style) {
        TangemSurface.MaterialStyle.Default -> MaterialColors(
            fillBlur = colors.material.fill.blur,
            fillSolid = colors.material.fill.solid,
            tintSolid = colors.material.tint.solid,
            borderStart = colors.material.border.start,
            borderEnd = colors.material.border.end,
        )
        TangemSurface.MaterialStyle.Inverted -> MaterialColors(
            fillBlur = colors.materialInverted.fill.blur,
            fillSolid = colors.materialInverted.fill.solid,
            tintSolid = colors.materialInverted.tint.solid,
            borderStart = colors.materialInverted.border.start,
            borderEnd = colors.materialInverted.border.end,
        )
    }
}

private class MaterialColors(
    val fillBlur: Color,
    val fillSolid: Color,
    val tintSolid: Color,
    val borderStart: Color,
    val borderEnd: Color,
)

@Suppress("MagicNumber")
@Composable
@ReadOnlyComposable
private fun materialBorderBrush(style: TangemSurface.MaterialStyle): Brush {
    val material = materialColors(style)
    val startColor = material.borderStart
    val midColor = Color.Transparent
    val endColor = material.borderEnd
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

/**
 * Whether the surface reads as inverse-colored, i.e. light content on a dark background in light
 * theme and vice versa. Such surfaces need the inverse press overlay — the default one is a
 * same-theme dim and stays invisible on them.
 *
 * A surface is inverse either because [color] is `bg.inverse`, or because it renders the inverted
 * material, whose fill lives in `materialInverted.*` rather than in [color].
 */
@Composable
@ReadOnlyComposable
private fun isInverseSurface(color: Color, isMaterial: Boolean, style: TangemSurface.MaterialStyle): Boolean =
    color == TangemTheme.colors3.bg.inverse || isMaterial && style == TangemSurface.MaterialStyle.Inverted

@Composable
@ReadOnlyComposable
private fun tangemSurfaceRipple(isInverseSurface: Boolean): RippleConfiguration = RippleConfiguration(
    color = if (isInverseSurface) {
        TangemTheme.colors3.interaction.press.inverse
    } else {
        TangemTheme.colors3.interaction.press.default
    },
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0f,
        focusedAlpha = 0f,
        hoveredAlpha = 0.05f,
        pressedAlpha = 0.1f,
    ),
)