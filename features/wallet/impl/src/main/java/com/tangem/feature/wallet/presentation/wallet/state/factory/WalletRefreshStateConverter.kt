package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
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
import kotlinx.coroutines.flow.update

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
            // TODO: [REDACTED_JIRA]
            walletsListConfig = getWalletsListConfig(),
            pullToRefreshConfig = getPullToRefreshConfig(),
            txHistoryState = getTxHistoryState(),
            marketPriceBlockState = MarketPriceBlockState.Loading(currencyName = marketPriceBlockState.currencyName),
        )
    }

    private fun WalletState.ContentState.getWalletsListConfig(): WalletsListConfig {
        val selectedWallet = walletsListConfig.wallets[walletsListConfig.selectedWalletIndex]

        return walletsListConfig.copy(
            wallets = walletsListConfig.wallets
                .toPersistentList()
                .set(
                    index = walletsListConfig.selectedWalletIndex,
                    element = WalletCardState.Loading(
                        id = selectedWallet.id,
                        title = selectedWallet.title,
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
                txHistoryState.contentItems.update {
                    TxHistoryState.getDefaultLoadingTransactions(onExploreClick = clickIntents::onExploreClick)
                }
                txHistoryState
            }
            is TxHistoryState.Empty,
            is TxHistoryState.Error,
            is TxHistoryState.NotSupported,
            -> txHistoryState
        }
    }
}