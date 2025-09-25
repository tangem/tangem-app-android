package com.tangem.features.yield.supply.impl.subcomponents.startearning.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class YieldSupplyStartEarningContentUM : TangemBottomSheetConfigContent {

    data class Main(
        val currencyIconState: CurrencyIconState,
        val fee: Fee?,
    ) : YieldSupplyStartEarningContentUM()

    data class FeePolicy(
        val title: TextReference,
    ) : YieldSupplyStartEarningContentUM()
}