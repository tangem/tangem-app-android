package com.tangem.features.tangempay.txhistory

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R

@Immutable
internal sealed interface TangemPayEmptyTransactionHistoryState {

    val text: TextReference

    data class FailedToLoad(
        val onReload: () -> Unit,
        override val text: TextReference = resourceReference(R.string.transaction_history_error_failed_to_load),
    ) : TangemPayEmptyTransactionHistoryState

    data object Empty : TangemPayEmptyTransactionHistoryState {
        override val text: TextReference = resourceReference(R.string.transaction_history_empty_transactions)
    }
}