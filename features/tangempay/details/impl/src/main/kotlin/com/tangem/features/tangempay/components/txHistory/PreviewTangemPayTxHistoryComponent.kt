package com.tangem.features.tangempay.components.txHistory

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.features.txhistory.ui.txHistoryItems
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class PreviewTangemPayTxHistoryComponent(txHistoryUM: TxHistoryUM) : TangemPayTxHistoryComponent {
    override val state: StateFlow<TxHistoryUM> = MutableStateFlow(txHistoryUM)

    override fun LazyListScope.txHistoryContent(listState: LazyListState, state: TxHistoryUM) {
        txHistoryItems(listState, state)
    }

    companion object {
        val loadingUM = TxHistoryUM.Loading(isBalanceHidden = true, onExploreClick = {})
        val emptyUM = TxHistoryUM.Empty(isBalanceHidden = true, onExploreClick = {})
        val contentUM = TxHistoryUM.Content(
            isBalanceHidden = false,
            loadMore = { false },
            items = persistentListOf(
                TxHistoryUM.TxHistoryItemUM.Title(onExploreClick = {}),
                TxHistoryUM.TxHistoryItemUM.GroupTitle(title = "Today", itemKey = "Today"),
                TxHistoryUM.TxHistoryItemUM.Transaction(
                    state = TransactionState.Content(
                        txHash = "signiferumque",
                        amount = "-4.99 USD",
                        time = "16:41",
                        status = TransactionState.Content.Status.Confirmed,
                        direction = TransactionState.Content.Direction.OUTGOING,
                        onClick = {},
                        iconRes = R.drawable.ic_arrow_up_24,
                        title = stringReference("Starbucks"),
                        subtitle = stringReference("Food&Drinks"),
                        timestamp = 3464,
                    ),
                ),
                TxHistoryUM.TxHistoryItemUM.Transaction(
                    state = TransactionState.Content(
                        txHash = "signiferumque",
                        amount = "-126.20 USD",
                        time = "12:04",
                        status = TransactionState.Content.Status.Confirmed,
                        direction = TransactionState.Content.Direction.OUTGOING,
                        onClick = {},
                        iconRes = R.drawable.ic_arrow_up_24,
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        timestamp = 3465,
                    ),
                ),
                TxHistoryUM.TxHistoryItemUM.GroupTitle(title = "Yesterday", itemKey = "Yesterday"),
                TxHistoryUM.TxHistoryItemUM.Transaction(
                    state = TransactionState.Content(
                        txHash = "signiferumque",
                        amount = "-4.99 USD",
                        time = "21:41",
                        status = TransactionState.Content.Status.Confirmed,
                        direction = TransactionState.Content.Direction.OUTGOING,
                        onClick = {},
                        iconRes = R.drawable.ic_arrow_up_24,
                        title = stringReference("Starbucks"),
                        subtitle = stringReference("Food&Drinks"),
                        timestamp = 3464,
                    ),
                ),
                TxHistoryUM.TxHistoryItemUM.Transaction(
                    state = TransactionState.Content(
                        txHash = "signiferumque",
                        amount = "-126.20 USD",
                        time = "10:04",
                        status = TransactionState.Content.Status.Confirmed,
                        direction = TransactionState.Content.Direction.OUTGOING,
                        onClick = {},
                        iconRes = R.drawable.ic_arrow_up_24,
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        timestamp = 3465,
                    ),
                ),
                TxHistoryUM.TxHistoryItemUM.Transaction(
                    state = TransactionState.Content(
                        txHash = "signiferumque",
                        amount = "-4.99 USD",
                        time = "19:41",
                        status = TransactionState.Content.Status.Confirmed,
                        direction = TransactionState.Content.Direction.OUTGOING,
                        onClick = {},
                        iconRes = R.drawable.ic_arrow_up_24,
                        title = stringReference("Starbucks"),
                        subtitle = stringReference("Food&Drinks"),
                        timestamp = 3464,
                    ),
                ),
                TxHistoryUM.TxHistoryItemUM.Transaction(
                    state = TransactionState.Content(
                        txHash = "signiferumque",
                        amount = "-126.20 USD",
                        time = "18:04",
                        status = TransactionState.Content.Status.Confirmed,
                        direction = TransactionState.Content.Direction.OUTGOING,
                        onClick = {},
                        iconRes = R.drawable.ic_arrow_up_24,
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        timestamp = 3465,
                    ),
                ),
                TxHistoryUM.TxHistoryItemUM.Transaction(
                    state = TransactionState.Content(
                        txHash = "signiferumque",
                        amount = "-4.99 USD",
                        time = "17:41",
                        status = TransactionState.Content.Status.Confirmed,
                        direction = TransactionState.Content.Direction.OUTGOING,
                        onClick = {},
                        iconRes = R.drawable.ic_arrow_up_24,
                        title = stringReference("Starbucks"),
                        subtitle = stringReference("Food&Drinks"),
                        timestamp = 3464,
                    ),
                ),
                TxHistoryUM.TxHistoryItemUM.Transaction(
                    state = TransactionState.Content(
                        txHash = "signiferumque",
                        amount = "-126.20 USD",
                        time = "16:04",
                        status = TransactionState.Content.Status.Confirmed,
                        direction = TransactionState.Content.Direction.OUTGOING,
                        onClick = {},
                        iconRes = R.drawable.ic_arrow_up_24,
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        timestamp = 3465,
                    ),
                ),
                TxHistoryUM.TxHistoryItemUM.Transaction(
                    state = TransactionState.Content(
                        txHash = "signiferumque",
                        amount = "-4.99 USD",
                        time = "15:41",
                        status = TransactionState.Content.Status.Confirmed,
                        direction = TransactionState.Content.Direction.OUTGOING,
                        onClick = {},
                        iconRes = R.drawable.ic_arrow_up_24,
                        title = stringReference("Starbucks"),
                        subtitle = stringReference("Food&Drinks"),
                        timestamp = 3464,
                    ),
                ),
                TxHistoryUM.TxHistoryItemUM.Transaction(
                    state = TransactionState.Content(
                        txHash = "signiferumque",
                        amount = "-126.20 USD",
                        time = "14:04",
                        status = TransactionState.Content.Status.Confirmed,
                        direction = TransactionState.Content.Direction.OUTGOING,
                        onClick = {},
                        iconRes = R.drawable.ic_arrow_up_24,
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        timestamp = 3465,
                    ),
                ),
            ),
        )
    }
}