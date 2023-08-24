package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.common.CardTypesResolver
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.*
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState.TokensListItemState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update

internal class WalletRefreshStateConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
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
            walletsListConfig = createWalletsListConfig(),
            pullToRefreshConfig = createPullToRefreshConfig(),
            tokensListState = createTokenListState(),
        )
    }

    private fun WalletSingleCurrencyState.Content.getRefreshState(): WalletSingleCurrencyState.Content {
        return copy(
            walletsListConfig = createWalletsListConfig(),
            pullToRefreshConfig = createPullToRefreshConfig(),
            buttons = buttons.mapToDisabledButton(),
            txHistoryState = createTxHistoryState(),
            marketPriceBlockState = MarketPriceBlockState.Loading(currencyName = marketPriceBlockState.currencyName),
        )
    }

    private fun WalletState.ContentState.createWalletsListConfig(): WalletsListConfig {
        val selectedWallet = walletsListConfig.wallets[walletsListConfig.selectedWalletIndex]
        val additionalInfo = if (currentCardTypeResolverProvider().isMultiwalletAllowed()) {
            selectedWallet.additionalInfo
        } else {
            null
        }

        return walletsListConfig.copy(
            wallets = walletsListConfig.wallets.toPersistentList().set(
                index = walletsListConfig.selectedWalletIndex,
                element = WalletCardState.Loading(
                    id = selectedWallet.id,
                    title = selectedWallet.title,
                    additionalInfo = additionalInfo,
                    imageResId = selectedWallet.imageResId,
                    onRenameClick = selectedWallet.onRenameClick,
                    onDeleteClick = selectedWallet.onDeleteClick,
                ),
            ),
        )
    }

    private fun WalletState.ContentState.createPullToRefreshConfig(): WalletPullToRefreshConfig {
        return pullToRefreshConfig.copy(isRefreshing = true)
    }

    private fun WalletMultiCurrencyState.Content.createTokenListState(): WalletTokensListState {
        return when (tokensListState) {
            is WalletTokensListState.Content -> {
                WalletTokensListState.Loading(
                    items = tokensListState.items
                        .filterIsInstance<TokensListItemState.Token>()
                        .mapToLoadingTokenState(),
                )
            }
            is WalletTokensListState.Empty -> WalletTokensListState.Loading()
            is WalletTokensListState.Loading,
            is WalletTokensListState.Locked,
            -> tokensListState
        }
    }

    private fun List<TokensListItemState.Token>.mapToLoadingTokenState(): ImmutableList<TokensListItemState.Token> {
        return this
            .map { TokensListItemState.Token(state = TokenItemState.Loading(id = it.state.id)) }
            .toImmutableList()
    }

    private fun ImmutableList<WalletManageButton>.mapToDisabledButton(): ImmutableList<WalletManageButton> {
        return this
            .mapNotNull { button ->
                when (button) {
                    is WalletManageButton.Buy -> button.copy(enabled = false)
                    is WalletManageButton.Send -> button.copy(enabled = false)
                    is WalletManageButton.Receive -> button
                    is WalletManageButton.Sell -> button.copy(enabled = false)
                    is WalletManageButton.Swap -> null
                }
            }
            .toImmutableList()
    }

    private fun WalletSingleCurrencyState.Content.createTxHistoryState(): TxHistoryState {
        if (txHistoryState is TxHistoryState.Content) {
            txHistoryState.contentItems.update {
                TxHistoryState.getDefaultLoadingTransactions(onExploreClick = clickIntents::onExploreClick)
            }
        }

        return txHistoryState
    }
}