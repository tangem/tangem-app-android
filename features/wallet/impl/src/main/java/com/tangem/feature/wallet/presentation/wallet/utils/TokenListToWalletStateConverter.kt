package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder.MultiCurrencyContent
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder.SingleCurrencyContent
import com.tangem.feature.wallet.presentation.wallet.state.WalletsListConfig
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class TokenListToWalletStateConverter(
    private val currentState: WalletStateHolder,
    private val isWalletContentHidden: Boolean,
    private val fiatCurrencyCode: String,
    private val fiatCurrencySymbol: String,
) : Converter<TokenList, WalletStateHolder> {

    override fun convert(value: TokenList): WalletStateHolder {
        return when (currentState) {
            is MultiCurrencyContent -> currentState.updateWithTokenList(value)
            is SingleCurrencyContent -> currentState.updateWithTokenList(value)
        }
    }

    private fun MultiCurrencyContent.updateWithTokenList(tokenList: TokenList): MultiCurrencyContent {
        val converter = TokenListToContentItemsConverter(isWalletContentHidden, fiatCurrencyCode, fiatCurrencySymbol)

        return this.copy(
            walletsListConfig = updateSelectedWallet(tokenList.totalFiatBalance),
            contentItems = converter.convert(tokenList),
        )
    }

    private fun SingleCurrencyContent.updateWithTokenList(tokenList: TokenList): SingleCurrencyContent {
        return this.copy(
            walletsListConfig = updateSelectedWallet(tokenList.totalFiatBalance),
        )
    }

    private fun WalletStateHolder.updateSelectedWallet(fiatBalance: TokenList.FiatBalance): WalletsListConfig {
        val selectedWalletIndex = walletsListConfig.selectedWalletIndex
        val selectedWalletCard = walletsListConfig.wallets[selectedWalletIndex]
        val converter = FiatBalanceToWalletCardConverter(
            selectedWalletCard,
            isWalletContentHidden,
            fiatCurrencyCode,
            fiatCurrencySymbol,
        )

        return walletsListConfig.copy(
            wallets = walletsListConfig.wallets
                .toPersistentList()
                .set(selectedWalletIndex, converter.convert(fiatBalance)),
        )
    }
}