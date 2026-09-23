@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.modal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.components.sheetscaffold.TangemSheetValue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Resting margin of every non-full-height card. */
private val ModalInsetMedium = 8.dp

private val ModalTopCornerRadius = 36.dp
private val ModalBottomCornerRadius = 32.dp

/** Height fraction (of the available height) where the margin starts blending to edge-to-edge. */
private const val TIER_FULL_START = 0.9f

/**
 * Share of the dismissal a full predictive-back stretch previews: the card slides down by this
 * much of its height and the scrim dims by the same fraction (see [combinedOpenProgress]), so
 * committing the gesture continues the dismissal from where the preview left it.
 */
internal const val MODAL_BACK_PREVIEW_FRACTION: Float = 0.4f

@PublishedApi
internal const val OPEN_PROGRESS_SETTLED: Float = 1f

/**
 * Edge-to-edge geometry of one modal. [modalCardLayout] measures how full the card should be and
 * writes it as [target]; [ModalMorphAnimationEffect] chases the target with a spring; the layout
 * renders the animated value and publishes it as [flatten] for the shape, the opacity overlay and
 * the drag indicator.
 */
@PublishedApi
internal class ModalMorphGeometry {

    /**
     * Measured morph target, 0..1 — how edge-to-edge the card *should* be given its content height.
     * Written during layout, [Float.NaN] until the first pass. Snapshot-backed so
     * [ModalMorphAnimationEffect] observes the changes.
     */
    var target: Float by mutableFloatStateOf(Float.NaN)

    /** Fullness riding toward [target]. Driven by [ModalMorphAnimationEffect]. */
    val animatedFullness = Animatable(initialValue = 0f)

    /**
     * Whether [animatedFullness] has caught its first target and drives the layout from now on.
     * Until then — and forever in previews, where effects don't run — the layout renders the
     * measured target directly, so the first frame (and a modal opening with full-height content)
     * starts settled instead of morphing in.
     */
    var isAnimationDriven: Boolean by mutableStateOf(false)

    /**
     * Whether a field inside the card holds focus. Written by the focus observer of the card
     * content; [ModalMorphAnimationEffect] rides [focusExpansion] toward it.
     */
    var isFocused: Boolean by mutableStateOf(false)

    /**
     * 0..1 share of the focus-driven expansion: a card with a focused field grows to the full
     * height (and back when focus clears), regardless of its content height — the iOS
     * keyboard-detent behavior. Independent of [animatedFullness] so a focus change mid-morph
     * cannot jump the card height.
     */
    val focusExpansion = Animatable(initialValue = 0f)

    /**
     * How edge-to-edge the card currently renders, 0..1: drives the bottom-corner flattening, the
     * fade of the glass into an opaque surface and the drag-indicator width. 1 when a full-height
     * modal is open. Written during layout from the animated fullness; snapshot-backed so
     * draw-phase consumers pick up the frames where the morph moves without a size change.
     */
    var flatten: Float by mutableFloatStateOf(0f)

    /**
     * Bottom strip (px) [modalCardFill] carves out of the card for the navigation bar, growing with
     * [flatten] as the bottom margin blends away — the content stays clear of the bar throughout.
     * Plain value: written by [modalCardLayout] and consumed by [modalCardFill] in the same pass.
     */
    var contentBottomInset: Int = 0

    /** Cache of the content's probed intrinsic height across morph frames. */
    val contentHeightCache = ModalContentHeightCache()
}

/**
 * Caches the content's intrinsic height across morph animation frames: while one of the morph
 * springs is running, the extra layout passes are caused by the animated values — the content
 * itself has not changed — so the last probed height is reused instead of traversing the content
 * tree on every frame. The trade-off: content that resizes itself mid-morph keeps the previous
 * height until the spring settles, and the settling pass re-probes it.
 */
@PublishedApi
internal class ModalContentHeightCache {

    private var probeWidth: Int = -1
    private var height: Int = -1

    fun heightOf(measurable: IntrinsicMeasurable, width: Int, isMorphing: Boolean): Int {
        if (width != probeWidth || height < 0 || !isMorphing) {
            probeWidth = width
            height = measurable.minIntrinsicHeight(width)
        }
        return height
    }
}

/** Spring the morph rides between its measured targets — the iOS detent-change feel. */
private val ModalMorphAnimationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)

/**
 * Animates [ModalMorphGeometry] between its measured targets: the first target is adopted without
 * animation — a modal whose content is full-height from the start opens already edge-to-edge — and
 * every later change (content growing to the screen height or shrinking back) plays the morph.
 * A field gaining or losing focus plays the same spring on [ModalMorphGeometry.focusExpansion].
 */
@Composable
@PublishedApi
internal fun ModalMorphAnimationEffect(geometry: ModalMorphGeometry) {
    LaunchedEffect(geometry) {
        launch {
            snapshotFlow { geometry.target }
                .filter { !it.isNaN() }
                .collectLatest { target ->
                    if (geometry.isAnimationDriven) {
                        geometry.animatedFullness.animateTo(
                            targetValue = target,
                            animationSpec = ModalMorphAnimationSpec,
                        )
                    } else {
                        geometry.animatedFullness.snapTo(targetValue = target)
                        geometry.isAnimationDriven = true
                    }
                }
        }
        launch {
            snapshotFlow { geometry.isFocused }.collectLatest { isFocused ->
                geometry.focusExpansion.animateTo(
                    targetValue = if (isFocused) 1f else 0f,
                    animationSpec = ModalMorphAnimationSpec,
                )
            }
        }
    }
}

/**
 * Morph target of a card whose content measures [intrinsicHeight] against [maxHeight] available:
 * 0 for every card resting below [TIER_FULL_START] of the height, blending linearly to 1 as the
 * content reaches the full height.
 */
@PublishedApi
internal fun modalMorphTarget(intrinsicHeight: Int, maxHeight: Int): Float {
    if (maxHeight <= 0) return 0f
    val fraction = intrinsicHeight.toFloat() / maxHeight
    return ((fraction - TIER_FULL_START) / (1f - TIER_FULL_START)).coerceIn(0f, 1f)
}

/**
 * How far the modal has slid in: 0 at the hidden anchor, 1 at the expanded anchor, interpolated
 * per frame during drags and settle animations. While the anchors or the offset are unresolvable it
 * reports the settled [TangemSheetState.currentValue] instead: the first passes run before the
 * anchors exist, and a fraction derived from an unresolved offset would report a sheet still parked
 * at the hidden anchor as open.
 */
@OptIn(ExperimentalFoundationApi::class)
@PublishedApi
internal fun TangemSheetState.openProgress(): Float {
    val anchors = anchoredDraggableState.anchors
    val hidden = anchors.positionOf(TangemSheetValue.Hidden)
    val expanded = anchors.positionOf(TangemSheetValue.Expanded)
    val offset = anchoredDraggableState.offset
    val isResolvable = !hidden.isNaN() && !expanded.isNaN() && !offset.isNaN()
    if (!isResolvable || hidden == expanded) {
        return if (currentValue == TangemSheetValue.Hidden) 0f else OPEN_PROGRESS_SETTLED
    }
    return ((hidden - offset) / (hidden - expanded)).coerceIn(0f, 1f)
}

/**
 * Folds the predictive-back gesture into the slide progress: a full gesture stretch dims the scrim
 * by the share of the dismissal the gesture previews (see [MODAL_BACK_PREVIEW_FRACTION]), so
 * committing continues the dismissal from the previewed state and cancelling restores it.
 */
@PublishedApi
internal fun combinedOpenProgress(openProgress: Float, backProgress: Float): Float {
    return openProgress * (1f - backProgress * MODAL_BACK_PREVIEW_FRACTION)
}

/**
 * Card layout of the modal, shtorka-style: the card grows with its content until it reaches
 * [windowInsets]' top (the status bar), and the resting margin is 8dp for every card shorter than
 * that, blending to 0 (edge-to-edge) as the content approaches the full height. The bottom margin
 * keeps clear of [windowInsets] (the navigation bar) while it exists; as it blends away the same
 * clearance moves inside the card (see [ModalMorphGeometry.contentBottomInset]).
 *
 * The card does not morph while the sheet slides in and out — it keeps its settled size and shape
 * and rides the slide. It morphs when its *content* height moves across the full-height threshold:
 * the measured target lands in [ModalMorphGeometry.target] and the layout renders the spring-driven
 * value chasing it (see [ModalMorphAnimationEffect]), re-measuring per animation frame.
 *
 * A focused field (see [ModalMorphGeometry.isFocused]) expands the card to the full height
 * regardless of its content height — the height rides [ModalMorphGeometry.focusExpansion] between
 * the natural and the full height, and the edge-to-edge chrome follows whichever of the two
 * drivers is further along.
 *
 * The content height is probed via intrinsic measurement, so the modal content must support
 * intrinsics (the built-in scrollable wrapper does; avoid a top-level lazy list as direct
 * content). [geometry] receives the resulting flatten progress for the shape and opacity.
 */
@PublishedApi
internal fun Modifier.modalCardLayout(geometry: ModalMorphGeometry, windowInsets: WindowInsets): Modifier {
    return layout { measurable, constraints ->
        val maxWidth = constraints.maxWidth
        // The status bar is the only height limit: a card as tall as its content gets it.
        val maxHeight = (constraints.maxHeight - windowInsets.getTop(this)).coerceAtLeast(0)

        val probeWidth = (maxWidth - 2 * ModalInsetMedium.roundToPx()).coerceAtLeast(0)
        val isMorphing = geometry.animatedFullness.isRunning || geometry.focusExpansion.isRunning
        val intrinsicHeight = geometry.contentHeightCache
            .heightOf(measurable = measurable, width = probeWidth, isMorphing = isMorphing)
            .coerceAtMost(maxHeight)

        val target = modalMorphTarget(intrinsicHeight = intrinsicHeight, maxHeight = maxHeight)
        if (geometry.target != target) geometry.target = target
        val contentFullness = if (geometry.isAnimationDriven) geometry.animatedFullness.value else target
        val focusExpansion = geometry.focusExpansion.value
        val fullness = max(contentFullness, focusExpansion)
        if (geometry.flatten != fullness) geometry.flatten = fullness

        val navBottom = windowInsets.getBottom(this)
        val navInset = (navBottom * fullness).roundToInt()
        geometry.contentBottomInset = navInset

        val baseInset = lerp(ModalInsetMedium.toPx(), 0f, fullness)
        val sideInset = baseInset.roundToInt().coerceAtLeast(0)
        val bottomInset = (baseInset + navBottom * (1f - fullness)).roundToInt().coerceAtLeast(0)

        val maxCardHeight = (maxHeight - bottomInset).coerceAtLeast(0)
        val naturalHeight = (intrinsicHeight + navInset).coerceAtMost(maxCardHeight)
        val cardHeight = lerp(naturalHeight, maxCardHeight, focusExpansion)
        val cardWidth = (maxWidth - 2 * sideInset).coerceAtLeast(0)

        val placeable = measurable.measure(Constraints.fixed(cardWidth, cardHeight))

        // The reported size — the card plus its bottom margin — feeds the drag anchors.
        layout(maxWidth, cardHeight + bottomInset) {
            placeable.placeRelative(x = (maxWidth - cardWidth) / 2, y = 0)
        }
    }
}

/**
 * Fills the card with its content — measured at the card's exact size and pinned to its top edge,
 * where the drag indicator and the title belong — instead of letting it wrap and be centered by
 * the surface. The two sizes agree to the pixel only as long as the content measures exactly as
 * tall as the intrinsic probe promised; a card that ended up wider than the probe width can
 * re-wrap its text shorter, and a centered card would then float its title below the top corner.
 *
 * The bottom of the card carves out [ModalMorphGeometry.contentBottomInset] — the navigation-bar
 * clearance an edge-to-edge card provides *inside* instead of as a bottom margin.
 *
 * Intrinsic queries pass straight through to the content — deliberately without the inset — so the
 * height probe of [modalCardLayout] reads the content's own intrinsics (a `Column` computes those
 * from its children and its weights) and the inset cannot feed back into the probed height.
 *
 * Expects the fixed constraints [modalCardLayout] measures the card with.
 */
@PublishedApi
internal fun Modifier.modalCardFill(geometry: ModalMorphGeometry): Modifier {
    return this then ModalCardFillElement(geometry)
}

private data class ModalCardFillElement(val geometry: ModalMorphGeometry) : ModifierNodeElement<ModalCardFillNode>() {

    override fun create(): ModalCardFillNode = ModalCardFillNode(geometry)

    override fun update(node: ModalCardFillNode) {
        node.geometry = geometry
    }
}

private class ModalCardFillNode(var geometry: ModalMorphGeometry) : Modifier.Node(), LayoutModifierNode {

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val cardWidth = constraints.maxWidth
        val cardHeight = constraints.maxHeight
        val bottomInset = geometry.contentBottomInset.coerceIn(0, cardHeight)
        val placeable = measurable.measure(Constraints.fixed(width = cardWidth, height = cardHeight - bottomInset))
        return layout(cardWidth, cardHeight) {
            placeable.placeRelative(x = 0, y = 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.maxIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.maxIntrinsicWidth(height)
}

/**
 * Card shape of the modal: 36dp top corners and 32dp bottom corners that flatten to zero as
 * [ModalMorphGeometry.flatten] approaches 1 — the same morph the shtorka does approaching its
 * full detent.
 *
 * The geometry is read inside [createOutline] on purpose: the flatten is snapshot-backed and the
 * card re-measures on every morph frame, so the outline is rebuilt as the morph animates.
 */
@PublishedApi
internal class ModalMorphShape(private val geometry: ModalMorphGeometry) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val halfMinSide = min(size.width, size.height) / 2f
        val topBase = with(density) { ModalTopCornerRadius.toPx() }.coerceAtMost(halfMinSide)
        val bottomBase = with(density) { ModalBottomCornerRadius.toPx() }.coerceAtMost(halfMinSide)
        val topRadius = CornerRadius(topBase)
        val bottomRadius = CornerRadius(bottomBase * (1f - geometry.flatten))
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