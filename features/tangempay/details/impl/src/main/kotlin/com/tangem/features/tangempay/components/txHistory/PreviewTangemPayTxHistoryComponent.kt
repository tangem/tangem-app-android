package com.tangem.features.tangempay.components.txHistory

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.entity.TangemPayTransactionState
import com.tangem.features.tangempay.entity.TangemPayTxHistoryUM
import com.tangem.features.tangempay.ui.tangemPayTxHistoryItems
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class PreviewTangemPayTxHistoryComponent(txHistoryUM: TangemPayTxHistoryUM) : TangemPayTxHistoryComponent {
    override val state: StateFlow<TangemPayTxHistoryUM> = MutableStateFlow(txHistoryUM)

    override fun LazyListScope.txHistoryContent(listState: LazyListState, state: TangemPayTxHistoryUM) {
        tangemPayTxHistoryItems(listState = listState, state = state)
    }

    companion object {
        val loadingUM = TangemPayTxHistoryUM.Loading(isBalanceHidden = true)
        val emptyUM = TangemPayTxHistoryUM.Empty(isBalanceHidden = true)
        val errorUM = TangemPayTxHistoryUM.Error(isBalanceHidden = true, onReload = {})
        val contentUM = TangemPayTxHistoryUM.Content(
            isBalanceHidden = false,
            loadMore = { false },
            items = persistentListOf(
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Title,
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.GroupTitle(title = "Today", itemKey = "Today"),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Spend(
                        id = "signiferumque",
                        onClick = {},
                        amount = "-4.99 USD",
                        amountColor = { TangemTheme.colors.text.primary1 },
                        time = "16:41",
                        title = stringReference("StarbucksStarbucksStarbucksStarbucks"),
                        subtitle = stringReference("Food&Drinks"),
                        iconUrl = null,
                    ),
                ),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Payment(
                        id = "signiferumque",
                        amount = "-126.20 USD",
                        amountColor = { TangemTheme.colors.text.primary1 },
                        time = "12:04",
                        onClick = {},
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        isIncome = false,
                    ),
                ),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Payment(
                        id = "signiferumque",
                        amount = "+126.20 USD",
                        amountColor = { TangemTheme.colors.text.accent },
                        time = "12:04",
                        onClick = {},
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        isIncome = true,
                    ),
                ),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Spend(
                        id = "signiferumque",
                        amount = "-126.20 USD",
                        amountColor = { TangemTheme.colors.text.primary1 },
                        time = "12:04",
                        onClick = {},
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        iconUrl = null,
                    ),
                ),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.GroupTitle(title = "Yesterday", itemKey = "Yesterday"),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Spend(
                        id = "signiferumque",
                        amount = "-4.99 USD",
                        amountColor = { TangemTheme.colors.text.primary1 },
                        time = "21:41",
                        onClick = {},
                        title = stringReference("Starbucks"),
                        subtitle = stringReference("Food&Drinks"),
                        iconUrl = null,
                    ),
                ),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Spend(
                        id = "signiferumque",
                        amount = "-126.20 USD",
                        amountColor = { TangemTheme.colors.text.primary1 },
                        time = "10:04",
                        onClick = {},
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        iconUrl = null,
                    ),
                ),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Spend(
                        id = "signiferumque",
                        amount = "-4.99 USD",
                        amountColor = { TangemTheme.colors.text.primary1 },
                        time = "19:41",
                        onClick = {},
                        title = stringReference("Starbucks"),
                        subtitle = stringReference("Food&Drinks"),
                        iconUrl = null,
                    ),
                ),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Spend(
                        id = "signiferumque",
                        amount = "-126.20 USD",
                        amountColor = { TangemTheme.colors.text.primary1 },
                        time = "18:04",
                        onClick = {},
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        iconUrl = null,
                    ),
                ),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Spend(
                        id = "signiferumque",
                        amount = "-4.99 USD",
                        amountColor = { TangemTheme.colors.text.primary1 },
                        time = "17:41",
                        onClick = {},
                        title = stringReference("Starbucks"),
                        subtitle = stringReference("Food&Drinks"),
                        iconUrl = null,
                    ),
                ),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Spend(
                        id = "signiferumque",
                        amount = "-126.20 USD",
                        amountColor = { TangemTheme.colors.text.primary1 },
                        time = "16:04",
                        onClick = {},
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        iconUrl = null,
                    ),
                ),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Spend(
                        id = "signiferumque",
                        amount = "-4.99 USD",
                        amountColor = { TangemTheme.colors.text.primary1 },
                        time = "15:41",
                        onClick = {},
                        title = stringReference("Starbucks"),
                        subtitle = stringReference("Food&Drinks"),
                        iconUrl = null,
                    ),
                ),
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(
                    transaction = TangemPayTransactionState.Content.Spend(
                        id = "signiferumque",
                        amount = "-126.20 USD",
                        amountColor = { TangemTheme.colors.text.primary1 },
                        time = "14:04",
                        onClick = {},
                        title = stringReference("Wallmart"),
                        subtitle = stringReference("Supermarket"),
                        iconUrl = null,
                    ),
                ),
            ),
        )
    }
}