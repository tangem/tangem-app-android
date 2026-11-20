package com.tangem.features.earn.impl.widgets.chart.ui

/**
 * The bar a "previous" ([step] -1) or "next" (+1) accessibility action moves the selection to. With nothing selected
 * the newest bar is chosen first, as it is the one the header's current rate belongs to.
 */
internal fun steppedBar(selected: Int?, barCount: Int, step: Int): Int {
    val newestBar = barCount - 1

    return if (selected == null) newestBar else (selected + step).coerceIn(0, newestBar)
}