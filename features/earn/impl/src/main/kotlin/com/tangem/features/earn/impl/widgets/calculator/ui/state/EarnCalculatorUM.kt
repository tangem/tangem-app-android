package com.tangem.features.earn.impl.widgets.calculator.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.impl.ui.EarnIncentiveUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class EarnCalculatorUM(
    val incentive: EarnIncentiveUM?,
    val total: TextReference,
    val earned: TextReference,
    val earnedLabel: TextReference,
    val horizon: EarnHorizonSelectUM,
    val bars: ImmutableList<EarnProjectionBarUM>,
    val axisLabels: ImmutableList<TextReference>,
    val selectedBar: Int?,
    val onBarSelected: (Int?) -> Unit,
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

@Immutable
internal data class EarnProjectionBarUM(
    val height: Float,
    val balance: TextReference,
    val earned: TextReference,
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