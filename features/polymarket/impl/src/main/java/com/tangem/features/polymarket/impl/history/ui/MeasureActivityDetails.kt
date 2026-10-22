package com.tangem.features.polymarket.impl.history.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

internal data class ActivityDetailsSize(val headerHeight: Int, val bodyHeight: Int)

/**
 * Measures the two slots before registering the modal. Nothing is placed, drawn or made interactive.
 * Fixed slot heights then give TangemModal accurate intrinsics from its very first layout, so its
 * opening anchor does not move after the slide has started. This is local to activity details.
 */
@Composable
internal fun MeasureActivityDetails(
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
    onMeasure: (ActivityDetailsSize) -> Unit,
) {
    Layout(content = {
        Box { header() }
        Box { body() }
    }) { measurables, constraints ->
        // TangemModal probes its content at the floating card width (8dp inset on either side),
        // including when tall content will ultimately expand to the full window width.
        val cardWidth = (constraints.maxWidth - 16.dp.roundToPx()).coerceAtLeast(0)
        val slotConstraints = Constraints.fixedWidth(cardWidth)
        val headerHeight = measurables[0].measure(slotConstraints).height
        val bodyHeight = measurables[1].measure(slotConstraints).height
        onMeasure(ActivityDetailsSize(headerHeight, bodyHeight))
        layout(0, 0) {}
    }
}