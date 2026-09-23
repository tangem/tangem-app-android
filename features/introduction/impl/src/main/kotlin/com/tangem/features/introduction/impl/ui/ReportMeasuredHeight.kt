package com.tangem.features.introduction.impl.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

private const val UNMEASURED = -1

/**
 * Publishes the child's measured height as this node's intrinsic height.
 *
 * [com.tangem.core.ui.ds2.modal.TangemModal] sizes its card from `minIntrinsicHeight`, while every size
 * animation in Compose — `SizeTransform`, `animateContentSize`, `expandVertically` — is built on
 * `LayoutModifierNodeWithPassThroughIntrinsics` and so stays invisible to that probe, which instead sees
 * `max(outgoing, incoming)` of the target composition. That maximum is why the card used to sit still for the
 * whole fade whenever a step got shorter. This node bridges the two: the measured height is published as
 * snapshot state, so reading it in the card's measure block re-measures the card on every frame of whatever
 * animation the content is running, in both directions.
 */
internal fun Modifier.reportMeasuredHeight(): Modifier = this then ReportMeasuredHeightElement

private object ReportMeasuredHeightElement : ModifierNodeElement<ReportMeasuredHeightNode>() {

    override fun create() = ReportMeasuredHeightNode()

    override fun update(node: ReportMeasuredHeightNode) {
        // The node carries no parameters, so there is nothing to push into it.
    }

    override fun hashCode(): Int = "reportMeasuredHeight".hashCode()

    override fun equals(other: Any?): Boolean = other === this
}

private class ReportMeasuredHeightNode : LayoutModifierNode, Modifier.Node() {

    private var reportedHeight by mutableIntStateOf(UNMEASURED)

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        // Loosened so the child reports the height its own animation is currently at, rather than the height
        // the card was forced to on the previous frame.
        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))

        if (reportedHeight != placeable.height) {
            reportedHeight = placeable.height
        }

        return layout(
            width = constraints.constrainWidth(placeable.width),
            height = constraints.constrainHeight(placeable.height),
        ) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        reportedHeight.takeIf { it != UNMEASURED } ?: measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        reportedHeight.takeIf { it != UNMEASURED } ?: measurable.maxIntrinsicHeight(width)

    // Delegated on purpose: the default implementations re-run measure() with a stand-in measurable, which
    // would publish a placeholder height.
    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.maxIntrinsicWidth(height)

    override fun onDetach() {
        reportedHeight = UNMEASURED
    }
}