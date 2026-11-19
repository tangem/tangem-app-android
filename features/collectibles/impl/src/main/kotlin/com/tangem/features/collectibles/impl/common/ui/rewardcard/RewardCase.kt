package com.tangem.features.collectibles.impl.common.ui.rewardcard

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tangem.features.collectibles.impl.common.ui.rewardcard.state.RewardCardAccent
import com.tangem.features.collectibles.impl.common.ui.rewardcard.state.RewardCardProgress

/** `cameraDistance` is in camera inches, projected at 72 px each. */
internal const val CAMERA_PX_PER_INCH = 72f

/** Rail the case leaves around the card (Figma `3558:70925`, a 260 x 406 card in a 280 x 426 case). */
internal val SLAB_INSET = PaddingValues(all = 10.dp)

/** Camera distance from the AE handoff: 2300 px on a 1196 px comp. */
private const val PERSPECTIVE_OF_SCREEN_WIDTH = 1.923f

/** The case's card window in the design (Figma `3558:70925`), on a frame of [DESIGN_FRAME_WIDTH]. */
private const val DESIGN_FRAME_WIDTH = 402f
private const val DESIGN_CARD_WIDTH = 260f
private const val DESIGN_CARD_HEIGHT = 406f

/** Height to width of the window until the card's image has loaded. */
internal const val DEFAULT_CARD_ASPECT = DESIGN_CARD_HEIGHT / DESIGN_CARD_WIDTH

/** The window keeps the design height, scaled with the screen width; its width follows the card. */
private const val CARD_HEIGHT_FRACTION = DESIGN_CARD_HEIGHT / DESIGN_FRAME_WIDTH

/** Widest a window may get for a flat image, so it still fits the case's design. */
private const val CARD_MAX_WIDTH_FRACTION = DESIGN_CARD_WIDTH / DESIGN_FRAME_WIDTH

private val SLAB_INSET_WIDTH =
    SLAB_INSET.calculateLeftPadding(LayoutDirection.Ltr) + SLAB_INSET.calculateRightPadding(LayoutDirection.Ltr)
private val SLAB_INSET_HEIGHT = SLAB_INSET.calculateTopPadding() + SLAB_INSET.calculateBottomPadding()

internal fun rewardCaseCameraDistance(widthPx: Int): Float = widthPx * PERSPECTIVE_OF_SCREEN_WIDTH / CAMERA_PX_PER_INCH

/** The case shrinks only when the gaps around it would otherwise drop below their minimum. */
internal fun rewardCaseSize(maxWidth: Dp, maxHeight: Dp, cardAspect: Float): DpSize {
    val cardHeight = minOf(
        maxWidth * CARD_HEIGHT_FRACTION,
        maxWidth * CARD_MAX_WIDTH_FRACTION * cardAspect,
        maxHeight - SLAB_INSET_HEIGHT,
    ).coerceAtLeast(0.dp)

    return DpSize(width = cardHeight / cardAspect + SLAB_INSET_WIDTH, height = cardHeight + SLAB_INSET_HEIGHT)
}

/**
 * The card in its case. The case and its glow stay still; only the card inside turns.
 *
 * @param isInteractive a tap turns the card over and a hold tilts it under a glare
 */
@Suppress("LongParameterList")
@Composable
internal fun RewardCase(
    frontImageUrl: String?,
    backImageUrl: String?,
    timing: RewardCardTiming,
    progress: RewardCardProgress,
    cameraDistance: Float,
    accent: RewardCardAccent = RewardCardAccent.Neutral,
    isInteractive: Boolean = progress.settle >= 1f,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cardWidth = maxWidth - SLAB_INSET_WIDTH
        val cardHeight = maxHeight - SLAB_INSET_HEIGHT

        val isGrowing = progress.grow < 1f
        val scale = if (isGrowing) {
            CardChoreography.growScale(grow = progress.grow, timing = timing)
        } else {
            CardChoreography.settleScale(settle = progress.settle, timing = timing)
        }
        val shift = if (isGrowing) {
            CardChoreography.growShift(grow = progress.grow, timing = timing)
        } else {
            CardChoreography.settleShift(settle = progress.settle, timing = timing)
        }

        val anchor = Modifier
            .align(Alignment.Center)
            .offset(y = (SLAB_INSET.calculateTopPadding() - SLAB_INSET.calculateBottomPadding()) / 2)

        val place = anchor
            .size(width = cardWidth, height = cardHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = shift * size.height
                alpha = progress.appear
                // An offscreen layer would clip the card's own perspective turn to these bounds
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }

        PsaSlab(
            accent = accent,
            settle = progress.settle,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            modifier = anchor,
        )

        CardFaces(
            angle = CardChoreography.spinDegrees(spin = progress.spin),
            cameraDistance = cameraDistance,
            frontImageUrl = frontImageUrl,
            backImageUrl = backImageUrl,
            isInteractive = isInteractive,
            modifier = place,
        )

        SlabGloss(
            settle = progress.settle,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            modifier = anchor,
        )
    }
}