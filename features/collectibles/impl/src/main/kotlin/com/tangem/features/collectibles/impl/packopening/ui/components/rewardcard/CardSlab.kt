package com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.features.collectibles.impl.packopening.PackOpeningConst.SLAB_INSET
import com.tangem.features.collectibles.impl.packopening.ui.state.RewardCardUM

private val SLAB_RADIUS = 20.dp

/** The two light streaks on the case glass, as fractions of the case (Figma `76:11512`, `76:11513`). */
private val GLOSS_BANDS = listOf(
    GlossBand(topLeftFraction = 0.599f, widthFraction = 0.309f, heightFraction = 0.796f, shearFraction = 0.701f),
    GlossBand(topLeftFraction = 0.928f, widthFraction = 0.097f, heightFraction = 0.414f, shearFraction = 0.365f),
)

/** Opacity of the streaks layer; the peak is the gradient's top stop. */
private const val GLOSS_ALPHA = 0.48f
private const val GLOSS_PEAK_ALPHA = 0.12f

@Composable
internal fun PsaSlab(
    accent: RewardCardUM.Accent,
    settle: Float,
    cardWidth: Dp,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = cardWidth + slabInsetWidth(), height = slabHeight(cardHeight))
            .offset(y = (SLAB_INSET.calculateBottomPadding() - SLAB_INSET.calculateTopPadding()) / 2)
            .graphicsLayer {
                alpha = settle
                scaleX = CardChoreography.frameScale(settle)
                scaleY = CardChoreography.frameScale(settle)
            },
    ) {
        TangemSurface(
            isMaterial = true,
            shape = RoundedCornerShape(SLAB_RADIUS),
            modifier = Modifier.matchParentSize(),
            content = {},
        )
        accent.glowVariant()?.let { variant ->
            TangemGlowRing(variant = variant, cornerRadius = SLAB_RADIUS, modifier = Modifier.matchParentSize())
        }
    }
}

private fun slabHeight(cardHeight: Dp): Dp =
    cardHeight + SLAB_INSET.calculateTopPadding() + SLAB_INSET.calculateBottomPadding()

@Composable
private fun slabInsetWidth(): Dp {
    val direction = LocalLayoutDirection.current
    return SLAB_INSET.calculateLeftPadding(direction) + SLAB_INSET.calculateRightPadding(direction)
}

@Composable
internal fun SlabGloss(settle: Float, cardWidth: Dp, cardHeight: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = cardWidth + slabInsetWidth(), height = slabHeight(cardHeight))
            .offset(y = (SLAB_INSET.calculateBottomPadding() - SLAB_INSET.calculateTopPadding()) / 2)
            .graphicsLayer {
                alpha = settle * GLOSS_ALPHA
                scaleX = CardChoreography.frameScale(settle)
                scaleY = CardChoreography.frameScale(settle)
            }
            .clip(RoundedCornerShape(SLAB_RADIUS))
            .drawBehind {
                GLOSS_BANDS.forEach { band ->
                    drawPath(path = band.path(size), brush = glossBrush(size.height * band.heightFraction))
                }
            },
    )
}

private fun DrawScope.glossBrush(height: Float): Brush = Brush.verticalGradient(
    colors = listOf(Color.White.copy(alpha = GLOSS_PEAK_ALPHA), Color.Transparent),
    startY = 0f,
    endY = height,
)

private data class GlossBand(
    val topLeftFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val shearFraction: Float,
) {

    fun path(size: Size): Path {
        val left = size.width * topLeftFraction
        val width = size.width * widthFraction
        val bottom = size.height * heightFraction
        val shear = size.width * shearFraction

        return Path().apply {
            moveTo(left, 0f)
            lineTo(left + width, 0f)
            lineTo(left + width - shear, bottom)
            lineTo(left - shear, bottom)
            close()
        }
    }
}

/** A common card gets no glow; the rest follow the tier colours, epic taking the shared multicolour glow. */
private fun RewardCardUM.Accent.glowVariant(): TangemGlowRing.Variant? = when (this) {
    RewardCardUM.Accent.Neutral -> null
    RewardCardUM.Accent.Green -> TangemGlowRing.Variant.Success
    RewardCardUM.Accent.Blue -> TangemGlowRing.Variant.Info
    RewardCardUM.Accent.Violet -> TangemGlowRing.Variant.Magic
}