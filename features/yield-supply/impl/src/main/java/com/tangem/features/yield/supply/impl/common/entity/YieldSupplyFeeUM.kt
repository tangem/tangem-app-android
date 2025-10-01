package com.tangem.features.yield.supply.impl.common.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.TransactionData
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class YieldSupplyFeeUM {
    data object Loading : YieldSupplyFeeUM()
    data object Error : YieldSupplyFeeUM()
    data class Content(
        val transactionDataList: ImmutableList<TransactionData.Uncompiled>,
        val feeValue: TextReference,
    ) : YieldSupplyFeeUM()
}

internal data class YieldSupplyActionUM(
    val title: TextReference,
    val subtitle: TextReference,
    val footer: TextReference,
    val currencyIconState: CurrencyIconState,
    val yieldSupplyFeeUM: YieldSupplyFeeUM,
    val isPrimaryButtonEnabled: Boolean,
)