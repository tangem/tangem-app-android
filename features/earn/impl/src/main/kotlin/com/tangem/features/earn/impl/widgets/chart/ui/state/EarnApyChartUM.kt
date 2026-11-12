package com.tangem.features.earn.impl.widgets.chart.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * @property currentRate the rate the header shows, the same one the Info screen headlines
 * @property scale vertical axis range and grid step, sized to the tallest bar
 * @property xLabels month names, oldest first; the UI spreads them evenly under the plot
 * @property selectedBar index into [bars] while a finger is down on the chart, `null` otherwise
 * @property onCalculatorClick `null` hides the calculator button
 */
@Immutable
internal data class EarnApyChartUM(
    val periods: ImmutableList<TangemSegmentedControl.Item>,
    val rateTitle: TextReference,
    val currentRate: TextReference,
    val bars: ImmutableList<EarnApyBarUM>,
    val scale: EarnApyScale,
    val average: EarnApyAverageUM?,
    val xLabels: ImmutableList<String>,
    val selectedBar: Int?,
    val isLoading: Boolean,
    val onBarSelected: (Int?) -> Unit,
    val onCalculatorClick: (() -> Unit)?,
)

/** @property value rate of the point in percent, `5.12` is 5.12% */
@Immutable
internal data class EarnApyBarUM(val value: Double, val tooltip: TextReference)

/**
 * @property value period mean in percent, drawn as the dashed line
 * @property text caption on the dashed line, `Avg 5.12%`
 * @property title caption of the mean in the header, beside the current rate
 * @property rate the mean as the header shows it, `5.12%`
 */
@Immutable
internal data class EarnApyAverageUM(
    val value: Double,
    val text: TextReference,
    val title: TextReference,
    val rate: TextReference,
)

/**
 * @property max top of the vertical axis, a whole number of [step]s at or above the tallest bar
 * @property decimals fraction digits a grid label needs to write [step] out
 */
@Immutable
internal data class EarnApyScale(val max: Double, val step: Double, val decimals: Int)