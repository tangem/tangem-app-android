package com.tangem.core.ui.ds2.for_you_temp.models

import androidx.compose.ui.graphics.Color

/**
 * One colored slice of a [com.tangem.core.ui.ds2.for_you_temp.DonutChart].
 *
 * @param weight Fraction of the full circle this slice occupies, in `0f..1f`. The slices are laid out
 *   contiguously; whatever is left after `sum(weight)` shows through as the track.
 *   For a portfolio where slices sum to 1f the ring fills completely and no track is visible.
 *   Also doubles as the slice's share for the selection tooltip (rendered as `weight * 100%`).
 * @param color Solid fill of the slice.
 * @param title Human-readable name of the asset this slice represents (e.g. `"Ethereum"`). Shown in the
 *   selection tooltip. Empty by default for slices that don't need a label.
 * @param fiatValue Pre-formatted fiat value of the slice (e.g. `"$5,720.22"`). Shown in the selection
 *   tooltip next to the share. Empty by default.
 */
data class DonutSegment(
    val weight: Float,
    val color: Color,
    val title: String = "",
    val fiatValue: String = "",
)