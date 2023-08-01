package com.tangem.feature.wallet.presentation.wallet.state.factory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.TransactionState
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.txhistory.error.TxHistoryStateError
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTxHistoryState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf

/**
 * Converter from loading tx history to [WalletTxHistoryState]
 *
 * @property currentStateProvider            current state provider
 * @property currentCardTypeResolverProvider current card type resolver provider
 *
[REDACTED_AUTHOR]
 */
internal class WalletLoadingTxHistoryConverter(
    private val currentStateProvider: Provider<WalletStateHolder>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
) : Converter<Either<TxHistoryStateError, Int>, WalletStateHolder> {

    override fun convert(value: Either<TxHistoryStateError, Int>): WalletStateHolder {
        return value.fold(ifLeft = ::convertError, ifRight = ::convert)
    }

    private fun convert(value: Int): WalletStateHolder {
        val state = currentStateProvider()

        return WalletStateHolder.SingleCurrencyContent(
            onBackClick = state.onBackClick,
            topBarConfig = state.topBarConfig,
            walletsListConfig = state.walletsListConfig,
            pullToRefreshConfig = state.pullToRefreshConfig,
            notifications = state.notifications,
            bottomSheet = state.bottomSheet,
            buttons = persistentListOf(
                WalletManageButton.Buy(onClick = {}),
                WalletManageButton.Send(onClick = {}),
                WalletManageButton.Receive(onClick = {}),
                WalletManageButton.Exchange(onClick = {}),
                WalletManageButton.CopyAddress(onClick = {}),
            )
                .map(WalletManageButton::config)
                .toImmutableList(),
            marketPriceBlockState = MarketPriceBlockState.Loading(
                currencyName = currentCardTypeResolverProvider().getBlockchain().currency,
            ),
            txHistoryState = createLoadingTxHistory(itemCount = value),
        )
    }

    private fun createLoadingTxHistory(itemCount: Int): WalletTxHistoryState {
        return WalletTxHistoryState.Content(
            items = flowOf(
                value = PagingData.from(
                    data = buildList(
                        capacity = itemCount,
                        builderAction = {
                            add(
                                element = WalletTxHistoryState.TxHistoryItemState.Transaction(
                                    state = TransactionState.Loading,
                                ),
                            )
                        },
                    ),
                ),
            ),
        )
    }

    private fun convertError(error: TxHistoryStateError): WalletStateHolder {
        val state = currentStateProvider()

        return WalletStateHolder.SingleCurrencyContent(
            onBackClick = state.onBackClick,
            topBarConfig = state.topBarConfig,
            walletsListConfig = state.walletsListConfig,
            pullToRefreshConfig = state.pullToRefreshConfig,
            notifications = state.notifications,
            bottomSheet = state.bottomSheet,
            buttons = WalletPreviewData.singleWalletScreenState.buttons,
            marketPriceBlockState = MarketPriceBlockState.Loading(
                currencyName = currentCardTypeResolverProvider().getBlockchain().currency,
            ),
            // TODO: [REDACTED_JIRA]
            txHistoryState = when (error) {
                is TxHistoryStateError.EmptyTxHistories -> WalletTxHistoryState.Empty(onBuyClick = {})
                is TxHistoryStateError.DataError -> WalletTxHistoryState.Error(onReloadClick = {})
                is TxHistoryStateError.TxHistoryNotImplemented -> WalletTxHistoryState.NotSupported(onExploreClick = {})
            },
        )
    }
}