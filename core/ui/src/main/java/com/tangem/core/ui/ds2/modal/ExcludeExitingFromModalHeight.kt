package com.tangem.core.ui.ds2.modal

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.unit.Constraints

/**
 * Hides an `AnimatedContent` child from the intrinsic height probe of [TangemModal] while it is leaving.
 *
 * The modal sizes its card from the content's intrinsic height, which during a transition is the larger of the
 * leaving and the arriving screen. Applied to every child, this makes the card start resizing to the arriving
 * screen as soon as the transition begins, driven by the card's own spring. Pair it with
 * `SizeTransform(clip = false) { _, _ -> snap() }` so `AnimatedContent` does not animate its size a second time.
 *
 * @param scope the scope of the `AnimatedContent` child this modifier is applied to.
 */
fun Modifier.excludeExitingFromModalHeight(scope: AnimatedVisibilityScope): Modifier =
    this then ExcludeFromIntrinsicHeightElement(
        isExcluded = scope.transition.targetState == EnterExitState.PostExit,
    )

private data class ExcludeFromIntrinsicHeightElement(
    val isExcluded: Boolean,
) : ModifierNodeElement<ExcludeFromIntrinsicHeightNode>() {

    override fun create() = ExcludeFromIntrinsicHeightNode(isExcluded)

    override fun update(node: ExcludeFromIntrinsicHeightNode) {
        node.isExcluded = isExcluded
        node.invalidateMeasurement()
    }
}

private class ExcludeFromIntrinsicHeightNode(var isExcluded: Boolean) : LayoutModifierNode, Modifier.Node() {

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        if (isExcluded) 0 else measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        if (isExcluded) 0 else measurable.maxIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.maxIntrinsicWidth(height)
}