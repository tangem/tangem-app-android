package com.tangem.features.tangempay.components.express

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.common.ui.expressStatus.expressTransactionsItemsLegacy
import com.tangem.common.ui.expressStatus.state.ExpressLinkUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemState
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateIconUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateInfoUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class PreviewEmptyExpressTransactionsComponent : ExpressTransactionsComponent {

    override val state: StateFlow<ExpressTransactionsBlockState> = MutableStateFlow(getInitialState())

    override fun LazyListScope.expressTransactionsContentLegacy(
        state: PersistentList<ExpressTransactionStateUM>,
        modifier: Modifier,
    ) {
        expressTransactionsItemsLegacy(expressTxs = state, modifier = modifier)
    }

    override fun LazyListScope.expressTransactionsContent(
        state: PersistentList<ExpressTransactionStateUM>,
        modifier: Modifier,
    ) {
        expressTransactionsItemsLegacy(expressTxs = state, modifier = modifier)
    }

    private fun getInitialState(): ExpressTransactionsBlockState {
        val sample = persistentListOf<ExpressTransactionStateUM>(
            sampleOnrampUM(
                txId = "preview-onramp-1",
                title = "Buying USDC",
                activeStatusText = "Verifying",
                activeStatus = OnrampStatus.Status.Verifying,
                timestampAgo = "5m ago",
                toAmount = "100.00",
                toSymbol = "USDC",
                fromAmount = "100.00",
                fromSymbol = "USD",
                iconState = ExpressTransactionStateIconUM.None,
            ),
            sampleOnrampUM(
                txId = "preview-onramp-2",
                title = "Buying USDC",
                activeStatusText = "Waiting for payment",
                activeStatus = OnrampStatus.Status.WaitingForPayment,
                timestampAgo = "1h ago",
                toAmount = "250.00",
                toSymbol = "USDC",
                fromAmount = "250.00",
                fromSymbol = "EUR",
                iconState = ExpressTransactionStateIconUM.Warning,
            ),
            sampleOnrampUM(
                txId = "preview-onramp-3",
                title = "Buying USDC",
                activeStatusText = "Failed",
                activeStatus = OnrampStatus.Status.Failed,
                timestampAgo = "2d ago",
                toAmount = "50.00",
                toSymbol = "USDC",
                fromAmount = "50.00",
                fromSymbol = "USD",
                iconState = ExpressTransactionStateIconUM.Error,
            ),
        )
        return ExpressTransactionsBlockState(
            transactions = sample,
            transactionsToDisplay = sample,
            bottomSheetSlot = null,
        )
    }

    @Suppress("LongParameterList")
    private fun sampleOnrampUM(
        txId: String,
        title: String,
        activeStatusText: String,
        activeStatus: OnrampStatus.Status,
        timestampAgo: String,
        toAmount: String,
        toSymbol: String,
        fromAmount: String,
        fromSymbol: String,
        iconState: ExpressTransactionStateIconUM,
    ): ExpressTransactionStateUM.OnrampUM {
        return ExpressTransactionStateUM.OnrampUM(
            info = ExpressTransactionStateInfoUM(
                title = TextReference.Str(title),
                status = ExpressStatusUM(
                    title = TextReference.Str("Status"),
                    link = ExpressLinkUM.Empty,
                    statuses = persistentListOf(
                        ExpressStatusItemUM(TextReference.Str("Created"), ExpressStatusItemState.Done),
                        ExpressStatusItemUM(TextReference.Str(activeStatusText), ExpressStatusItemState.Active),
                        ExpressStatusItemUM(TextReference.Str("Finished"), ExpressStatusItemState.Default),
                    ),
                ),
                notification = null,
                txId = txId,
                txExternalId = null,
                txExternalUrl = null,
                timestamp = 0L,
                timestampFormatted = TextReference.Str(timestampAgo),
                timestampAgoFormatted = TextReference.Str(timestampAgo),
                activeStatus = TextReference.Str(activeStatusText),
                onGoToProviderClick = {},
                onClick = {},
                onDisposeExpressStatus = {},
                iconState = iconState,
                toAmount = TextReference.Str(toAmount),
                toFiatAmount = null,
                toAmountSymbol = toSymbol,
                toCurrencyIcon = CurrencyIconState.Empty(),
                fromAmount = TextReference.Str(fromAmount),
                fromFiatAmount = null,
                fromAmountSymbol = fromSymbol,
                fromCurrencyIcon = CurrencyIconState.Empty(),
            ),
            providerName = "Preview Provider",
            providerImageUrl = "",
            providerType = "CEX",
            activeStatus = activeStatus,
            fromCurrencyCode = fromSymbol,
        )
    }
}