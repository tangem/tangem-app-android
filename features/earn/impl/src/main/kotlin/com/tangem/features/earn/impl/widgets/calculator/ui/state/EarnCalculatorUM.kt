package com.tangem.features.earn.impl.widgets.calculator.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class EarnCalculatorUM(
    val incentive: TangemBadgeUM?,
    val total: TextReference,
    val earned: TextReference,
    val earnedLabel: TextReference,
    val horizon: EarnHorizonSelectUM,
    val bars: ImmutableList<EarnProjectionBarUM>,
    val axisLabels: ImmutableList<TextReference>,
    val initialBalance: EarnStepperUM,
    val monthlyTopUp: EarnStepperUM,
)

@Immutable
internal data class EarnHorizonSelectUM(
    val selected: TextReference,
    val items: ImmutableList<EarnHorizonItemUM>,
)

@Immutable
internal data class EarnHorizonItemUM(
    val label: TextReference,
    val isSelected: Boolean,
    val onClick: () -> Unit,
)

/**
 * Layers from bottom to top as shares of the tallest bar: the initial balance, the total balance of the month and
 * the monthly top-up. The layers sit one above the other, so the tallest bar is exactly 1.
 */
@Immutable
internal data class EarnProjectionBarUM(
    val initial: Float,
    val total: Float,
    val topUp: Float,
)

@Immutable
internal data class EarnStepperUM(
    val label: TextReference,
    val value: TextReference,
    val isMinusEnabled: Boolean,
    val isPlusEnabled: Boolean,
    val minusDescription: TextReference,
    val plusDescription: TextReference,
    val onMinus: () -> Unit,
    val onPlus: () -> Unit,
)