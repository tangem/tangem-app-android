package com.tangem.feature.wallet.presentation.wallet.state.factory

import androidx.paging.PagingData
import androidx.paging.map
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.TransactionState
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.txhistory.error.TxHistoryListError
import com.tangem.domain.txhistory.model.TxHistoryItem
import com.tangem.feature.wallet.presentation.wallet.state.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTxHistoryState
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTxHistoryState.TxHistoryItemState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Converter from loaded tx history to [WalletTxHistoryState]
 *
 * @property currentStateProvider            current state provider
 * @property currentCardTypeResolverProvider current card type resolver provider
 *
* [REDACTED_AUTHOR]
 */
internal class WalletLoadedTxHistoryConverter(
    private val currentStateProvider: Provider<WalletStateHolder>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
) : Converter<Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>, WalletStateHolder> {

    override fun convert(value: Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>): WalletStateHolder {
        return value.fold(ifLeft = { convertError() }, ifRight = ::convert)
    }

    private fun convertError(): WalletStateHolder {
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
// [REDACTED_TODO_COMMENT]
            txHistoryState = WalletTxHistoryState.Error(onReloadClick = {}),
        )
    }

    private fun convert(items: Flow<PagingData<TxHistoryItem>>): WalletStateHolder {
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
            txHistoryState = createContentTxHistory(items),
        )
    }

    private fun createContentTxHistory(items: Flow<PagingData<TxHistoryItem>>): WalletTxHistoryState {
        return WalletTxHistoryState.Content(
            items = items.map { pagingData ->
                pagingData.map { item ->
                    TxHistoryItemState.Transaction(
                        state = when (val direction = item.direction) {
                            is TxHistoryItem.TransactionDirection.Incoming -> {
                                when (item.status) {
                                    TxHistoryItem.TxStatus.Confirmed -> TransactionState.Receive(
                                        address = direction.from,
                                        amount = item.amount.toPlainString(),
                                        timestamp = item.timestamp.toString(),
                                    )
                                    TxHistoryItem.TxStatus.Unconfirmed -> TransactionState.Receiving(
                                        address = direction.from,
                                        amount = item.amount.toPlainString(),
                                        timestamp = item.timestamp.toString(),
                                    )
                                }
                            }
                            is TxHistoryItem.TransactionDirection.Outgoing -> {
                                when (item.status) {
                                    TxHistoryItem.TxStatus.Confirmed -> TransactionState.Send(
                                        address = direction.to,
                                        amount = item.amount.toPlainString(),
                                        timestamp = item.timestamp.toString(),
                                    )
                                    TxHistoryItem.TxStatus.Unconfirmed -> TransactionState.Sending(
                                        address = direction.to,
                                        amount = item.amount.toPlainString(),
                                        timestamp = item.timestamp.toString(),
                                    )
                                }
                            }
                        },
                    )
                }
            },
        )
    }
}
