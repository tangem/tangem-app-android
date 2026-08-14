package com.tangem.features.tangempay.txhistory.details

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

internal data class ButtonState(
    val text: TextReference,
    val onClick: () -> Unit,
)

internal data class TangemPayTxHistoryDetailsUM(
    val isBalanceHidden: Boolean,
    val title: TextReference,
    val subtitle: TextReference,
    val iconState: TangemIconUM,
    val transactionTitle: TextReference,
    val detail: TransactionDetailUM?,
    val cashbackDetail: CashbackDetailUM?,
    val transactionCategory: TextReference,
    val mcc: TextReference?,
    val transactionAmount: String,
    val localTransactionText: String?,
    val label: TransactionLabelUM?,
    val buttonState: ButtonState,
    val dismiss: () -> Unit,
)

internal data class TransactionLabelUM(
    val transactionStateType: TransactionStateType,
    val icon: TangemIconUM,
    val title: TextReference,
    val subtitle: TextReference? = null,
)

internal enum class TransactionStateType {
    Completed,
    InProgress,
    Rejected,
    Reversed,
}

@Immutable
internal sealed interface TransactionDetailUM {
    data object Loading : TransactionDetailUM
    data class Content(
        val cardNumber: TextReference?,
        val cardName: TextReference?,
    ) : TransactionDetailUM
    data class Error(val onRefreshClick: () -> Unit) : TransactionDetailUM
}

@Immutable
internal sealed interface CashbackDetailUM {
    data object Loading : CashbackDetailUM
    data object AwaitingCalculation : CashbackDetailUM
    data class Content(
        val value: TextReference,
        val subvalue: TextReference?,
    ) : CashbackDetailUM
    data class Error(val onRefreshClick: () -> Unit) : CashbackDetailUM
}

internal enum class TransactionLoadState { Loading, Loaded, Error }