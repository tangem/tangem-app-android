package com.tangem.feature.wallet.presentation.wallet.state.factory

import androidx.paging.PagingData
import androidx.paging.map
import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.state.TxHistoryState.TxHistoryItemState
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletPullToRefreshConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState.TokensListItemState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

internal class WalletRefreshStateConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val clickIntents: WalletClickIntents,
) : Converter<Unit, WalletState> {

    override fun convert(value: Unit): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletMultiCurrencyState.Content -> state.getRefreshState()
            is WalletSingleCurrencyState.Content -> state.getRefreshState()
            else -> state
        }
    }

    private fun WalletMultiCurrencyState.Content.getRefreshState(): WalletMultiCurrencyState.Content {
        return copy(
            walletsListConfig = getWalletsListConfig(),
            pullToRefreshConfig = getPullToRefreshConfig(),
            tokensListState = getTokenListState(),
        )
    }

    private fun WalletSingleCurrencyState.Content.getRefreshState(): WalletSingleCurrencyState.Content {
        return copy(
            // TODO: https://tangem.atlassian.net/browse/AND-4271
            walletsListConfig = getWalletsListConfig(additionalInfo = ""),
            pullToRefreshConfig = getPullToRefreshConfig(),
            txHistoryState = getTxHistoryState(),
            marketPriceBlockState = MarketPriceBlockState.Loading(currencyName = marketPriceBlockState.currencyName),
        )
    }

    private fun WalletState.ContentState.getWalletsListConfig(additionalInfo: String? = null): WalletsListConfig {
        val selectedWallet = walletsListConfig.wallets[walletsListConfig.selectedWalletIndex]

        return walletsListConfig.copy(
            wallets = walletsListConfig.wallets
                .toPersistentList()
                .set(
                    index = walletsListConfig.selectedWalletIndex,
                    element = WalletCardState.Loading(
                        id = selectedWallet.id,
                        title = selectedWallet.title,
                        additionalInfo = additionalInfo ?: selectedWallet.additionalInfo,
                        imageResId = selectedWallet.imageResId,
                    ),
                ),
        )
    }

    private fun WalletState.ContentState.getPullToRefreshConfig(): WalletPullToRefreshConfig {
        return pullToRefreshConfig.copy(isRefreshing = true)
    }

    private fun WalletMultiCurrencyState.Content.getTokenListState(): WalletTokensListState {
        return when (tokensListState) {
            is WalletTokensListState.Content -> {
                WalletTokensListState.Loading(
                    items = tokensListState.items
                        .filterIsInstance<TokensListItemState.Token>()
                        .map {
                            TokensListItemState.Token(state = TokenItemState.Loading(id = it.state.id))
                        }
                        .toImmutableList(),
                )
            }
            is WalletTokensListState.Empty -> WalletTokensListState.Loading()
            is WalletTokensListState.Loading,
            is WalletTokensListState.Locked,
            -> tokensListState
        }
    }

    private fun WalletSingleCurrencyState.Content.getTxHistoryState(): TxHistoryState {
        return when (txHistoryState) {
            is TxHistoryState.Content -> {
                TxHistoryState.Loading(
                    onExploreClick = clickIntents::onExploreClick,
                    transactions = txHistoryState.contentItems
                        .filterIsInstance<PagingData<TxHistoryItemState.Transaction>>()
                        .mapPagingData { transaction ->
                            transaction.copy(
                                state = TransactionState.Loading(txHash = transaction.state.txHash),
                            )
                        },
                )
            }
            is TxHistoryState.Empty,
            is TxHistoryState.Error,
            is TxHistoryState.NotSupported,
            -> TxHistoryState.Loading(onExploreClick = clickIntents::onExploreClick)
            is TxHistoryState.Locked,
            is TxHistoryState.Loading,
            -> txHistoryState
        }
    }

    private fun Flow<PagingData<TxHistoryItemState.Transaction>>.mapPagingData(
        transform: (TxHistoryItemState.Transaction) -> TxHistoryItemState,
    ): Flow<PagingData<TxHistoryItemState>> {
        return map { it.map(transform) }
    }
}
