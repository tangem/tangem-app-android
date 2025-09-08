package com.tangem.features.yieldlending.impl.subcomponents.startearning.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import kotlinx.collections.immutable.ImmutableList

internal data class YieldLendingStartEarningUM(
    val bottomSheetConfig: TangemBottomSheetConfig,
)

@Immutable
internal sealed class YieldLendingStartEarningContentUM : TangemBottomSheetConfigContent {

    data class Main(
        val currencyIconState: CurrencyIconState,
        val fee: Fee?,
        val currentStepType: EnterStepType,
        val steps: ImmutableList<EnterStep>,
    ) : YieldLendingStartEarningContentUM()

    data class FeePolicy(
        val title: TextReference,
    ) : YieldLendingStartEarningContentUM()
}

data class EnterStep(
    val stepType: EnterStepType,
    val isComplete: Boolean,
    val isActive: Boolean,
)

enum class EnterStepType(val title: TextReference) {
    Deploy(title = stringReference("Deploy contract")),
    InitToken(title = stringReference("Init yield token")),
    Approve(title = stringReference("Approve")),
    Enter(title = stringReference("Enter protocol")),
}