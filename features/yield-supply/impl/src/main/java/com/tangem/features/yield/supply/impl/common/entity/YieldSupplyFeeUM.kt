package com.tangem.features.yield.supply.impl.common.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.TransactionData
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.models.TransactionFeeExtended
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class YieldSupplyFeeUM {
    data object Loading : YieldSupplyFeeUM()
    data object Error : YieldSupplyFeeUM()
    data class Content(
        val transactionDataList: ImmutableList<TransactionData.Uncompiled>,
        val feeFiatValue: TextReference,
        // TODO move to FeePolicyUM
        val estimatedFiatValue: TextReference,
        val maxNetworkFeeFiatValue: TextReference,
        val minTopUpFiatValue: TextReference,
        val feeNoteValue: TextReference = TextReference.EMPTY,
        val minFeeNoteValue: TextReference = TextReference.EMPTY,
        /** Smart Gas fee to send [transactionDataList] with; null when the native coin pays. */
        val gaslessFee: TransactionFeeExtended? = null,
        /** Currency [feeFiatValue] is paid in, kept with the fee it was estimated with. */
        val feeCurrency: CryptoCurrency? = null,
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
    val isHoldToConfirmEnabled: Boolean,
    /** Whether the fee row opens the fee token list. */
    val isFeeCurrencySelectable: Boolean = false,
)