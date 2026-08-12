@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.modal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.components.sheetscaffold.TangemSheetValue
import kotlin.math.min
import kotlin.math.roundToInt

/** Height of the capsule the card collapses into: the drag indicator plus the top navigation row. */
private val ModalCapsuleHeight = 88.dp

/** Extra per-side margin of the capsule; melts away as the card expands into place. */
private val ModalCapsuleExtraInset = 24.dp

/** Resting margin of every non-full-height card. */
private val ModalInsetMedium = 8.dp

private val ModalTopCornerRadius = 36.dp
private val ModalBottomCornerRadius = 32.dp

/** Height fraction (of the available height) where the margin starts blending to edge-to-edge. */
private const val TIER_FULL_START = 0.9f

/**
 * Slide fraction where the capsule ↔ card morph happens: the capsule travels for the first 60%
 * of the way in and unfolds into the full card over the remaining stretch — so dismissal reads
 * "collapse to a capsule first, then slide away" and appearance "slide in, then unfold".
 */
private const val MORPH_PHASE_START = 0.6f

@PublishedApi
internal const val OPEN_PROGRESS_SETTLED: Float = 1f

/**
 * Per-frame morph geometry of one modal, written during layout and read by [ModalMorphShape] and
 * the opacity overlay. Plain (non-snapshot) values: consumers are re-invoked by the size changes
 * the morph itself produces.
 */
@PublishedApi
internal class ModalMorphGeometry {

    /**
     * How edge-to-edge the card currently is, 0..1: drives the bottom-corner flattening and the
     * fade of the glass into an opaque surface. 1 when a height-capped (or expanded) modal is
     * fully open.
     */
    var flatten: Float = 0f

    /** Capsule ↔ card phase, 0..1: 0 is the fully-rounded capsule, 1 the settled card. */
    var unfold: Float = 1f
}

/**
 * How far the modal has slid in: 0 at the hidden anchor, 1 at the expanded anchor, interpolated
 * per frame during drags and settle animations. 1 before the anchors are known.
 */
@OptIn(ExperimentalFoundationApi::class)
@PublishedApi
internal fun TangemSheetState.openProgress(): Float {
    val anchors = anchoredDraggableState.anchors
    val hidden = anchors.positionOf(TangemSheetValue.Hidden)
    val expanded = anchors.positionOf(TangemSheetValue.Expanded)
    val offset = anchoredDraggableState.offset
    val isResolvable = !hidden.isNaN() && !expanded.isNaN() && !offset.isNaN()
    if (!isResolvable || hidden == expanded) return 1f
    return ((hidden - offset) / (hidden - expanded)).coerceIn(0f, 1f)
}

/**
 * The capsule ↔ card phase of the slide, 0..1: stays 0 (capsule) while the sheet travels and runs
 * to 1 (full card) over the last stretch near the settled position (see [MORPH_PHASE_START]).
 */
@PublishedApi
internal fun morphPhase(openProgress: Float): Float {
    return ((openProgress - MORPH_PHASE_START) / (1f - MORPH_PHASE_START)).coerceIn(0f, 1f)
}

/**
 * Folds the predictive-back gesture into the slide progress: a full gesture stretch previews the
 * complete capsule fold (the settled card lands exactly at [MORPH_PHASE_START], where the card is
 * fully collapsed and still bottom-pinned), so committing continues the dismissal from the folded
 * state and cancelling unfolds back into place.
 */
@PublishedApi
internal fun combinedOpenProgress(openProgress: Float, backProgress: Float): Float {
    return openProgress * (1f - backProgress * (1f - MORPH_PHASE_START))
}

/**
 * Card layout of the modal, shtorka-style: the resting margin is 8dp for every non-full-height
 * card, blending to 0 (edge-to-edge) as the content reaches the height cap. The bottom margin
 * also keeps clear of [windowInsets] (the navigation bar) until the card goes edge-to-edge.
 *
 * While sliding in and out, the card is collapsed into a fully-rounded capsule
 * ([ModalCapsuleHeight] tall, [ModalCapsuleExtraInset] narrower per side) riding the sheet's
 * leading edge; over the last stretch of the slide it unfolds into the full card (see
 * [morphPhase]). The collapse is a real height re-measure — content is clipped by the card shape,
 * whose corners become a capsule automatically as the height drops below the corner diameter.
 *
 * The *reported* size stays constant for a given content (the full card plus its bottom margin):
 * it feeds the drag anchors, which must not move while the card inside morphs.
 *
 * The full content height is probed via intrinsic measurement, so the modal content must support
 * intrinsics (the built-in scrollable wrapper does; avoid a top-level lazy list as direct content).
 * [geometry] receives the resulting flatten progress for the shape and opacity.
 */
@PublishedApi
@Suppress("LongParameterList")
internal fun Modifier.modalMorphLayout(
    geometry: ModalMorphGeometry,
    isExpanded: Boolean,
    windowInsets: WindowInsets,
    progress: () -> Float,
    anchorProgress: () -> Float,
): Modifier {
    return layout { measurable, constraints ->
        val maxWidth = constraints.maxWidth
        val maxHeight = constraints.maxHeight
        val openNow = progress()
        val expandPhase = morphPhase(openNow)

        val probeWidth = (maxWidth - 2 * ModalInsetMedium.roundToPx()).coerceAtLeast(0)
        val intrinsicHeight = measurable.minIntrinsicHeight(probeWidth).coerceAtMost(maxHeight)

        val baseInset: Float
        val fullness: Float
        if (isExpanded) {
            baseInset = 0f
            fullness = 1f
        } else {
            val fraction = if (maxHeight > 0) intrinsicHeight.toFloat() / maxHeight else 0f
            baseInset = restingInset(fraction)
            fullness = ((fraction - TIER_FULL_START) / (1f - TIER_FULL_START)).coerceIn(0f, 1f)
        }
        geometry.flatten = fullness * expandPhase
        geometry.unfold = expandPhase

        val sideInset = (baseInset + ModalCapsuleExtraInset.toPx() * (1f - expandPhase))
            .roundToInt()
            .coerceAtLeast(0)
        val bottomInset = (baseInset + windowInsets.getBottom(this) * (1f - fullness))
            .roundToInt()
            .coerceAtLeast(0)

        val fullHeight = intrinsicHeight.coerceAtMost((maxHeight - bottomInset).coerceAtLeast(0))
        val capsuleHeight = ModalCapsuleHeight.roundToPx().coerceAtMost(fullHeight)
        val cardHeight = lerp(capsuleHeight.toFloat(), fullHeight.toFloat(), expandPhase).roundToInt()
        val cardWidth = (maxWidth - 2 * sideInset).coerceAtLeast(0)
        val placeable = measurable.measure(Constraints.fixed(cardWidth, cardHeight))

        // Constant reported size: the anchors see the full card regardless of the morph.
        val reportedHeight = fullHeight + bottomInset
        layout(maxWidth, reportedHeight) {
            // The in-box position compensates the sheet's own translation, keeping the card's
            // bottom edge screen-fixed while its top collapses/unfolds. The pin must derive from
            // the sheet's actual position ([anchorProgress]) — not the fold progress, which the
            // predictive back gesture moves without moving the sheet. Once the sheet travels past
            // the morph zone, the capsule freezes in the box and rides the slide.
            val anchoredProgress = anchorProgress().coerceAtLeast(MORPH_PHASE_START)
            val y = (anchoredProgress * reportedHeight - bottomInset - cardHeight).roundToInt()
            placeable.placeRelative(x = (maxWidth - cardWidth) / 2, y = y)
        }
    }
}

/** Resting side margin by content-height [fraction]: 8dp, blending to 0dp near the height cap. */
private fun Density.restingInset(fraction: Float): Float {
    val medium = ModalInsetMedium.toPx()
    return when {
        fraction < TIER_FULL_START -> medium
        fraction < 1f -> lerp(medium, 0f, (fraction - TIER_FULL_START) / (1f - TIER_FULL_START))
        else -> 0f
    }
}

/**
 * Card shape of the modal: 36dp top corners and 32dp bottom corners that flatten to zero as
 * [ModalMorphGeometry.flatten] approaches 1 — the same morph the shtorka does approaching its
 * full detent. As the card collapses into the capsule ([ModalMorphGeometry.unfold] → 0), all
 * corners grow into the fully-rounded half-height radius.
 *
 * The geometry is read inside [createOutline] on purpose: the card is re-measured whenever the
 * morph inputs change (its size tracks the slide progress), so the outline is rebuilt exactly
 * when the values can have changed, without any snapshot subscription.
 */
@PublishedApi
internal class ModalMorphShape(private val geometry: ModalMorphGeometry) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val halfMinSide = min(size.width, size.height) / 2f
        val unfold = geometry.unfold
        // The rounding target is the *final* capsule's radius, not the current card size —
        // otherwise a tall card mid-collapse balloons into a big rounded blob.
        val capsuleRadius = with(density) { ModalCapsuleHeight.toPx() / 2f }.coerceAtMost(halfMinSide)
        val topBase = with(density) { ModalTopCornerRadius.toPx() }.coerceAtMost(halfMinSide)
        val bottomBase = with(density) { ModalBottomCornerRadius.toPx() }.coerceAtMost(halfMinSide)
        val topRadius = CornerRadius(lerp(capsuleRadius, topBase, unfold))
        val bottomRadius = CornerRadius(lerp(capsuleRadius, bottomBase * (1f - geometry.flatten), unfold))
        return Outline.Rounded(
            RoundRect(
                rect = size.toRect(),
                topLeft = topRadius,
                topRight = topRadius,
                bottomRight = bottomRadius,
                bottomLeft = bottomRadius,
            ),
        )
    }
}