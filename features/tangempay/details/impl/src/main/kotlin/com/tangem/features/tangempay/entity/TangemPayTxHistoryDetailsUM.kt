package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference

internal data class ButtonState(
    val text: TextReference,
    val onClick: () -> Unit,
    val startIcon: ImageReference.Res? = null,
)

internal data class TangemPayTxHistoryDetailsUMV2(
    val isBalanceHidden: Boolean,
    val title: TextReference,
    val subtitle: TextReference,
    val iconState: TangemIconUM,
    val transactionTitle: TextReference,
    val detail: TransactionDetailUM?,
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

internal enum class TransactionLoadState { Loading, Loaded, Error }