package com.tangem.features.earn.impl.widgets.chart.ui

import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.haptic.TangemHapticEffect

/**
 * Vico calls [onUpdated] whenever the marker targets change, and a target includes the colour of its column. The
 * columns are dimmed or restored right after the selection moves, so the same bar is reported twice: the selection and
 * the haptic go out only when the bar itself changes.
 */
internal class BarSelectionListener(
    private val haptic: HapticManager,
    private val onBarSelect: (Int?) -> Unit,
) : CartesianMarkerVisibilityListener {

    private var selectedBar: Int? = null

    override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
        select(bar = targets.barIndex(), effect = TangemHapticEffect.View.ContextClick)
    }

    override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
        select(bar = targets.barIndex(), effect = TangemHapticEffect.View.TextHandleMove)
    }

    override fun onHidden(marker: CartesianMarker) {
        selectedBar = null
        onBarSelect(null)
    }

    private fun select(bar: Int, effect: TangemHapticEffect) {
        if (bar == selectedBar) return

        selectedBar = bar
        onBarSelect(bar)
        haptic.perform(effect)
    }
}

internal fun List<CartesianMarker.Target>.barIndex(): Int = (first() as ColumnCartesianLayerMarkerTarget).x.toInt()