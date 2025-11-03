package com.tangem.features.yield.supply.impl.common.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.TransactionData
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

@Immutable
internal sealed class YieldSupplyFeeUM {
    data object Loading : YieldSupplyFeeUM()
    data object Error : YieldSupplyFeeUM()
    data class Content(
        val transactionDataList: ImmutableList<TransactionData.Uncompiled>,
        val feeFiatValue: TextReference,
        val tokenFeeFiatValue: TextReference,
        val maxNetworkFeeFiatValue: TextReference,
        val minTopUpFiatValue: TextReference,
        val feeNoteValue: TextReference = TextReference.EMPTY,
        val minFeeNoteValue: TextReference = TextReference.EMPTY,
    ) : YieldSupplyFeeUM()
}

internal data class YieldSupplyActionUM(
    val title: TextReference,
    val subtitle: TextReference,
    val footer: TextReference,
    val footerLink: TextReference,
    val currencyIconState: CurrencyIconState,
    val yieldSupplyFeeUM: YieldSupplyFeeUM,
    val isPrimaryButtonEnabled: Boolean,
    val isTransactionSending: Boolean,
    val maxFee: BigDecimal = BigDecimal.ZERO,
)