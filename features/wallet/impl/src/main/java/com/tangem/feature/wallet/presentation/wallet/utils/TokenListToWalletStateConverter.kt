package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.core.ui.components.transactions.TransactionState
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletContentItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder.MultiCurrencyContent
import com.tangem.feature.wallet.presentation.wallet.state.WalletsListConfig
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class TokenListToWalletStateConverter(
    private val currentState: WalletStateHolder,
    private val isWalletContentHidden: Boolean,
    private val fiatCurrencyCode: String,
    private val fiatCurrencySymbol: String,
    private val isRefreshing: Boolean,
) : Converter<TokenList, WalletStateHolder> {

    override fun convert(value: TokenList): WalletStateHolder {
        val state = if (currentState is MultiCurrencyContent) currentState.updateWithTokenList(value) else currentState

        return state.copySealed(
            walletsListConfig = state.updateSelectedWallet(value.totalFiatBalance),
            pullToRefreshConfig = if (isRefreshing) {
                state.pullToRefreshConfig.copy(isRefreshing = state.getRefreshingStatus())
            } else {
                state.pullToRefreshConfig
            },
        )
    }

    private fun MultiCurrencyContent.updateWithTokenList(tokenList: TokenList): MultiCurrencyContent {
        return this.copy(
            contentItems = TokenListToContentItemsConverter(
                isWalletContentHidden = isWalletContentHidden,
                fiatCurrencyCode = fiatCurrencyCode,
                fiatCurrencySymbol = fiatCurrencySymbol,
            ).convert(value = tokenList),
        )
    }

    private fun WalletStateHolder.updateSelectedWallet(fiatBalance: TokenList.FiatBalance): WalletsListConfig {
        val selectedWalletIndex = walletsListConfig.selectedWalletIndex
        val selectedWalletCard = walletsListConfig.wallets[selectedWalletIndex]
        val converter = FiatBalanceToWalletCardConverter(
            currentState = selectedWalletCard,
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
        return contentItems.any { state ->
            val isMultiCurrencyItem = state as? WalletContentItemState.MultiCurrencyItem.Token
            val isSingleCurrencyItem = state as? WalletContentItemState.SingleCurrencyItem.Transaction

            isMultiCurrencyItem?.state is TokenItemState.Loading ||
                isSingleCurrencyItem?.state is TransactionState.Loading ||
                state is WalletContentItemState.Loading
        }
    }
}
