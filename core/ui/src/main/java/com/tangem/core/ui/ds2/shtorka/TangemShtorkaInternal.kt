@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.shtorka

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.DraggableAnchors
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Spring the shtorka lands on a detent with: soft, slightly under-damped — approximates the iOS
 * sheet's `response ≈ 0.5s, dampingFraction ≈ 0.85` settle.
 */
internal val ShtorkaSettleSpring: AnimationSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 300f)

/**
 * Decay the settle *moves* with when the fling is strong enough to reach the target on its own.
 * Heavy friction, so most flings hand over to [ShtorkaSettleSpring] carrying their velocity.
 */
internal val ShtorkaMotionDecay: DecayAnimationSpec<Float> = exponentialDecay(
    frictionMultiplier = 10f,
    absVelocityThreshold = 0.5f,
)

/**
 * Decay used only to *project* where a fling would naturally stop; the detent closest to that
 * point becomes the settle target. Lighter friction than [ShtorkaMotionDecay], so a decisive
 * flick projects far enough to skip intermediate detents.
 */
internal val ShtorkaProjectionDecay: DecayAnimationSpec<Float> = exponentialDecay(frictionMultiplier = 1.2f)

/** Spring that relaxes the rubber-band overdrag back to zero after the finger lifts. */
internal val ShtorkaOverdragReleaseSpring: AnimationSpec<Float> = spring(dampingRatio = 1f, stiffness = 400f)

/** Margin around the floating card at the collapsed (smallest) detent. */
internal val ShtorkaCollapsedInset = 24.dp

/** Margin around the floating card at intermediate detents; morphs to zero approaching [TangemShtorka.Detent.Full]. */
internal val ShtorkaFloatingInset = 8.dp

/** Top corner radius of the card. */
internal val ShtorkaTopCornerRadius = 36.dp

/** Bottom corner radius of the card; flattens to zero approaching [TangemShtorka.Detent.Full]. */
internal val ShtorkaBottomCornerRadius = 47.dp

/** Asymptotic limit of the rubber-band translation when dragging beyond the outermost detents. */
internal val ShtorkaOverdragLimit = 56.dp

/**
 * Core layout of the shtorka: fills the parent, resolves [TangemShtorkaState.detents] into
 * draggable anchors against the parent height, and sizes/places the card off the live drag offset
 * every frame — `height = parentHeight - offset - bottomInset`, so the card's bottom edge stays
 * pinned while the top edge follows the finger, exactly like the iOS sheet.
 *
 * Every detent carries its own floating margin — [ShtorkaCollapsedInset] at the collapsed
 * (smallest) detent, [ShtorkaFloatingInset] at intermediate ones and zero at
 * [TangemShtorka.Detent.Full] — and the rendered margin interpolates between the two neighboring
 * detents as the card is dragged. [TangemShtorkaState.expandProgress] and
 * [TangemShtorkaState.collapseProgress] are published for [ShtorkaShape] and the opacity overlay.
 *
 * The bottom margin additionally includes [windowInsets]' bottom (typically the navigation bar),
 * so at partial detents the card floats above the system bar and at full it extends beneath it.
 * [TangemShtorka.Detent.Full] stops at [windowInsets]' top (the status bar).
 *
 * The rendered offset is clamped at the top anchor: settle-spring overshoot past full would
 * otherwise slide the height-capped card up and reveal a gap under its bottom edge.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.shtorkaLayout(state: TangemShtorkaState, windowInsets: WindowInsets): Modifier {
    return layout { measurable, constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val topSafeInset = windowInsets.getTop(density = this).toFloat()
        val bottomSafeInset = windowInsets.getBottom(density = this).toFloat()

        val geometry = resolveGeometry(
            detents = state.detents,
            layoutHeight = layoutHeight,
            topSafeInset = topSafeInset,
            bottomSafeInset = bottomSafeInset,
            density = this,
        )
        val anchors = DraggableAnchors {
            geometry.forEach { anchor -> anchor.detent at anchor.position }
        }
        state.anchoredDraggableState.updateAnchors(anchors, state.targetDetent)

        val offset = state.requireOffset().coerceAtLeast(geometry.first().position)
        val progress = expandProgress(geometry = geometry, offset = offset)
        state.expandProgress = progress
        state.collapseProgress = collapseProgress(geometry = geometry, offset = offset)

        val baseInset = interpolatedBaseInset(geometry = geometry, offset = offset)
        val horizontalInset = baseInset
        val bottomInset = baseInset + bottomSafeInset * (1f - progress)
        val cardWidth = (layoutWidth - 2 * horizontalInset).roundToInt().coerceIn(0, layoutWidth)
        val cardHeight = (layoutHeight - offset - bottomInset).roundToInt().coerceIn(0, layoutHeight)
        val placeable = measurable.measure(Constraints.fixed(cardWidth, cardHeight))

        layout(layoutWidth, layoutHeight) {
            placeable.placeRelative(x = (layoutWidth - cardWidth) / 2, y = offset.roundToInt())
        }
    }
}

/** Resolved per-detent geometry: top-edge anchor [position] and the card's floating [baseInset]. */
internal class ShtorkaAnchor(
    val detent: TangemShtorka.Detent,
    val position: Float,
    val baseInset: Float,
)

/**
 * Resolves each detent into an anchor. A detent's resolved value is the *visible card height*; the
 * anchor position additionally accounts for the detent's own bottom margin, so e.g. a collapsed
 * `Height(96.dp)` detent shows exactly 96dp of card floating [ShtorkaCollapsedInset] (+ system
 * inset) above the parent bottom. [TangemShtorka.Detent.Full] spans from [topSafeInset] (the
 * status bar) to the parent bottom.
 */
private fun resolveGeometry(
    detents: List<TangemShtorka.Detent>,
    layoutHeight: Int,
    topSafeInset: Float,
    bottomSafeInset: Float,
    density: Density,
): List<ShtorkaAnchor> {
    val heights = detents.associateWith { detent ->
        when (detent) {
            is TangemShtorka.Detent.Height -> with(density) { detent.value.toPx() }
            is TangemShtorka.Detent.Fraction -> layoutHeight * detent.value
            is TangemShtorka.Detent.Full -> layoutHeight - topSafeInset
        }
    }
    val collapsedDetent = heights.minByOrNull { it.value }?.key
    return detents
        .map { detent ->
            val baseInset = with(density) {
                when {
                    detent is TangemShtorka.Detent.Full -> 0f
                    detent == collapsedDetent -> ShtorkaCollapsedInset.toPx()
                    else -> ShtorkaFloatingInset.toPx()
                }
            }
            val bottomInset = if (detent is TangemShtorka.Detent.Full) 0f else baseInset + bottomSafeInset
            ShtorkaAnchor(
                detent = detent,
                position = (layoutHeight - bottomInset - heights.getValue(detent)).coerceAtLeast(topSafeInset),
                baseInset = baseInset,
            )
        }
        .sortedBy { it.position }
}

/** Piecewise-linear interpolation of the floating margin between the two anchors around [offset]. */
private fun interpolatedBaseInset(geometry: List<ShtorkaAnchor>, offset: Float): Float {
    val first = geometry.first()
    if (offset <= first.position) return first.baseInset
    for (index in 0 until geometry.size - 1) {
        val from = geometry[index]
        val to = geometry[index + 1]
        if (offset <= to.position) {
            val fraction = (offset - from.position) / (to.position - from.position)
            return lerp(from.baseInset, to.baseInset, fraction)
        }
    }
    return geometry.last().baseInset
}

/**
 * 0 while the shtorka floats at partial detents, 1 at [TangemShtorka.Detent.Full]; interpolates
 * across the drag between the highest non-full detent and full. 0 forever when there is no full
 * detent, 1 forever when full is the only detent.
 */
private fun expandProgress(geometry: List<ShtorkaAnchor>, offset: Float): Float {
    val fullPosition = geometry.firstOrNull { it.detent is TangemShtorka.Detent.Full }?.position ?: return 0f
    val morphStart = geometry.map { it.position }.filter { it > fullPosition + 1f }.minOrNull() ?: return 1f
    return ((morphStart - offset) / (morphStart - fullPosition)).coerceIn(0f, 1f)
}

/**
 * 1 at the collapsed (bottom-most) detent, 0 at the neighboring detent above; interpolates across
 * the drag between them. Drives the fully-rounded (capsule) corners of the collapsed card.
 */
private fun collapseProgress(geometry: List<ShtorkaAnchor>, offset: Float): Float {
    if (geometry.size < 2) {
        return if (geometry.first().detent is TangemShtorka.Detent.Full) 0f else 1f
    }
    val collapsed = geometry.last()
    val previous = geometry[geometry.size - 2]
    if (collapsed.position - previous.position < 1f) return 0f
    return ((offset - previous.position) / (collapsed.position - previous.position)).coerceIn(0f, 1f)
}

/**
 * Card shape: [ShtorkaTopCornerRadius] top corners and [ShtorkaBottomCornerRadius] bottom corners
 * that grow into a fully-rounded capsule as [collapseProgress] approaches 1, while the bottom
 * corners flatten as [expandProgress] approaches 1.
 *
 * The progresses are read inside [createOutline] on purpose: the card is re-measured on every
 * dragged frame (its height tracks the offset), so the outline is rebuilt exactly when they can
 * have changed, without any snapshot subscription.
 */
internal class ShtorkaShape(
    private val expandProgress: () -> Float,
    private val collapseProgress: () -> Float,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val capsuleRadius = min(size.width, size.height) / 2f
        val collapse = collapseProgress()
        val topBase = with(density) { ShtorkaTopCornerRadius.toPx() }.coerceAtMost(capsuleRadius)
        val bottomBase = with(density) { ShtorkaBottomCornerRadius.toPx() }.coerceAtMost(capsuleRadius)
        val topRadius = CornerRadius(lerp(topBase, capsuleRadius, collapse))
        val bottomRadius = CornerRadius(lerp(bottomBase, capsuleRadius, collapse) * (1f - expandProgress()))
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

/**
 * iOS-style rubber-band resistance: identity slope near zero, asymptotically approaching [limit]
 * as the raw finger travel grows.
 */
internal fun rubberBandOffset(raw: Float, limit: Float): Float {
    if (raw == 0f) return 0f
    return raw * limit / (abs(raw) + limit)
}