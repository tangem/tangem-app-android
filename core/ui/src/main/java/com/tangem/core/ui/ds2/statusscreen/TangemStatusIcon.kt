@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.statusscreen

import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 animated status icon for [TangemStatusScreen]'s asset slot: the DS lottie glyph draws
 * itself on in the status color, over a blurred copy of itself and under a white inner-shadow pass, per the
 * "Asset Breakdown" in the DS figma. The lottie ships monochrome, so the color comes from the theme.
 * Below Android 12 the glow and inner-shadow passes are skipped — the blur they need has no render effect
 * there — and only the crisp tinted glyph is drawn.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=9267-11586)
 *
 * The glyph is authored square and scales to whatever size the [modifier] gives it — size it explicitly
 * (canonically `Modifier.size(40.dp)`).
 *
 * @param kind The glyph to draw — see [TangemStatusIcon.Kind].
 * @param modifier Modifier for the icon; must provide the size.
 * @param tint Glyph and glow color; defaults to the status color matching [kind].
 * @param animated Whether to play the draw-on animation; `false` renders the final frame.
 * @param contentDescription Accessibility label; pass a value when the icon is the only carrier of the
 * status, `null` when the screen already announces it in text.
 */
@Composable
fun TangemStatusIcon(
    kind: TangemStatusIcon.Kind,
    modifier: Modifier = Modifier,
    tint: Color = kind.defaultTint(),
    animated: Boolean = true,
    contentDescription: String? = null,
) {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(kind.rawRes))
    val playing by animateLottieCompositionAsState(composition = composition, isPlaying = animated)
    val progress = if (animated) playing else 1f

    BoxWithConstraints(
        modifier = if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        },
        contentAlignment = Alignment.Center,
    ) {
        val glyphSize = minOf(maxWidth, maxHeight)

        if (isBlurSupported) {
            BlurredPass(
                glyphSize = glyphSize,
                blurRadius = GlowBlur,
                alpha = tint.alpha * GLOW_ALPHA,
            ) {
                Glyph(composition = composition, progress = progress, color = tint)
            }
        }
        Glyph(
            composition = composition,
            progress = progress,
            color = tint,
            modifier = Modifier
                .size(glyphSize)
                .alpha(alpha = tint.alpha),
        )
        if (isBlurSupported) {
            BlurredPass(
                glyphSize = glyphSize,
                blurRadius = HighlightBlur,
                alpha = HIGHLIGHT_ALPHA,
                modifier = Modifier.offset(y = HighlightOffsetY),
            ) {
                Glyph(composition = composition, progress = progress, color = Color.White)
            }
        }
    }
}

/**
 * Hosts one blurred copy of the glyph in a layer inflated well past [blurRadius] on every side. The blur
 * is cut at its layer bounds, so without that margin the glow ends abruptly and reads as a hard square
 * around the icon.
 */
@Composable
private fun BlurredPass(
    glyphSize: Dp,
    blurRadius: Dp,
    alpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val margin = blurRadius * 2

    Box(
        modifier = modifier
            .requiredSize(glyphSize + margin * 2)
            .alpha(alpha = alpha)
            .blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .padding(all = margin),
    ) {
        content()
    }
}

object TangemStatusIcon {

    /** The glyphs shipped by the DS: one per [TangemStatusScreen.Variant] mood except Neutral. */
    enum class Kind { Checkmark, HeartBroken, Warning, Info }
}

@Composable
private fun Glyph(composition: LottieComposition?, progress: Float, color: Color, modifier: Modifier = Modifier) {
    val argb = color.copy(alpha = 1f).toArgb()
    val properties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(LottieProperty.STROKE_COLOR, argb, "**"),
        rememberLottieDynamicProperty(LottieProperty.COLOR, argb, "**"),
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
        dynamicProperties = properties,
    )
}

@Composable
private fun TangemStatusIcon.Kind.defaultTint(): Color = when (this) {
    TangemStatusIcon.Kind.Checkmark -> TangemTheme.colors3.icon.status.success
    TangemStatusIcon.Kind.HeartBroken -> TangemTheme.colors3.icon.status.error
    TangemStatusIcon.Kind.Warning -> TangemTheme.colors3.icon.status.warning
    TangemStatusIcon.Kind.Info -> TangemTheme.colors3.icon.status.info
}

@get:RawRes
private val TangemStatusIcon.Kind.rawRes: Int
    get() = when (this) {
        TangemStatusIcon.Kind.Checkmark -> R.raw.anim_status_checkmark
        TangemStatusIcon.Kind.HeartBroken -> R.raw.anim_status_heart_broken
        TangemStatusIcon.Kind.Warning -> R.raw.anim_status_warning
        TangemStatusIcon.Kind.Info -> R.raw.anim_status_info
    }

private const val GLOW_ALPHA = 0.6f
private const val HIGHLIGHT_ALPHA = 0.24f
private val GlowBlur = 16.dp
private val HighlightBlur = 2.dp
private val HighlightOffsetY = 2.dp
private val isBlurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemStatusIconPreview() {
    TangemThemePreviewRedesign {
        Row(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            TangemStatusIcon.Kind.entries.forEach { kind ->
                TangemStatusIcon(
                    kind = kind,
                    modifier = Modifier.size(40.dp),
                    animated = false,
                )
            }
        }
    }
}