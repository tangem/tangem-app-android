package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Provider
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder.MultiCurrencyContent
import com.tangem.feature.wallet.presentation.wallet.state.WalletsListConfig
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListToWalletStateConverter.TokensListModel
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class TokenListToWalletStateConverter(
    private val currentStateProvider: Provider<WalletStateHolder>,
    private val cardTypeResolverProvider: Provider<CardTypesResolver>,
    private val isWalletContentHidden: Boolean,
    private val fiatCurrencyCode: String,
    private val fiatCurrencySymbol: String,
    clickIntents: WalletClickIntents,
) : Converter<TokensListModel, WalletStateHolder> {

    private val tokenListToContentConverter = TokenListToContentItemsConverter(
        isWalletContentHidden = isWalletContentHidden,
        fiatCurrencyCode = fiatCurrencyCode,
        fiatCurrencySymbol = fiatCurrencySymbol,
        clickIntents = clickIntents,
    )

    override fun convert(value: TokensListModel): WalletStateHolder {
        val state = currentStateProvider()
        return state
            .updateWithTokenList(tokenList = value.tokenList)
            .copySealed(
                walletsListConfig = state.updateSelectedWallet(value.tokenList.totalFiatBalance),
                pullToRefreshConfig = if (value.isRefreshing) {
                    state.pullToRefreshConfig.copy(isRefreshing = state.getRefreshingStatus())
                } else {
                    state.pullToRefreshConfig
                },
            )
    }

    private fun WalletStateHolder.updateWithTokenList(tokenList: TokenList): MultiCurrencyContent {
        return MultiCurrencyContent(
            onBackClick = onBackClick,
            topBarConfig = topBarConfig,
            walletsListConfig = walletsListConfig,
            pullToRefreshConfig = pullToRefreshConfig,
            notifications = notifications,
            bottomSheetConfig = bottomSheetConfig,
            tokensListState = tokenListToContentConverter.convert(value = tokenList),
        )
    }

    private fun WalletStateHolder.updateSelectedWallet(fiatBalance: TokenList.FiatBalance): WalletsListConfig {
        val selectedWalletIndex = walletsListConfig.selectedWalletIndex
        val selectedWalletCard = walletsListConfig.wallets[selectedWalletIndex]
        val converter = FiatBalanceToWalletCardConverter(
            currentState = selectedWalletCard,
            isLockedState = this is WalletStateHolder.UnlockWalletContent,
            cardTypeResolverProvider = cardTypeResolverProvider,
            isWalletContentHidden = isWalletContentHidden,
            fiatCurrencyCode = fiatCurrencyCode,
            fiatCurrencySymbol = fiatCurrencySymbol,
        )

        return walletsListConfig.copy(
            wallets = walletsListConfig.wallets
                .toPersistentList()
                .set(index = selectedWalletIndex, element = converter.convert(fiatBalance)),
        )
    }

    private fun WalletStateHolder.getRefreshingStatus(): Boolean {
        return if (this is MultiCurrencyContent) {
            tokensListState.items.any { tokensListItemState ->
                tokensListItemState is WalletTokensListState.TokensListItemState.Token &&
                    tokensListItemState.state is TokenItemState.Loading
            }
        } else {
            false
        }
    }

    data class TokensListModel(val tokenList: TokenList, val isRefreshing: Boolean)
}
