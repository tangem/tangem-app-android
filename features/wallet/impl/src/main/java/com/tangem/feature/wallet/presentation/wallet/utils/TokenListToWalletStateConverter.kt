package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Provider
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

@Suppress("LongParameterList")
internal class TokenListToWalletStateConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val cardTypeResolverProvider: Provider<CardTypesResolver>,
    private val currentWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val isWalletContentHidden: Boolean,
    clickIntents: WalletClickIntents,
) : Converter<TokenList, WalletMultiCurrencyState.Content> {

    private val tokenListToContentConverter = TokenListToContentItemsConverter(
        isWalletContentHidden = isWalletContentHidden,
        appCurrencyProvider = appCurrencyProvider,
        clickIntents = clickIntents,
    )

    override fun convert(value: TokenList): WalletMultiCurrencyState.Content {
        val state = requireNotNull(currentStateProvider() as? WalletMultiCurrencyState.Content)
        return state.copy(
            walletsListConfig = state.updateSelectedWallet(fiatBalance = value.totalFiatBalance),
            tokensListState = tokenListToContentConverter.convert(value = value),
        )
    }

    private fun WalletMultiCurrencyState.updateSelectedWallet(fiatBalance: TokenList.FiatBalance): WalletsListConfig {
        val selectedWalletIndex = walletsListConfig.selectedWalletIndex
        val selectedWalletCard = walletsListConfig.wallets[selectedWalletIndex]
        val converter = FiatBalanceToWalletCardConverter(
            currentState = selectedWalletCard,
            currentWalletProvider = currentWalletProvider,
            cardTypeResolverProvider = cardTypeResolverProvider,
            appCurrencyProvider = appCurrencyProvider,
            isWalletContentHidden = isWalletContentHidden,
        )

        return walletsListConfig.copy(
            wallets = walletsListConfig.wallets.toPersistentList()
                .set(index = selectedWalletIndex, element = converter.convert(fiatBalance)),
        )
    }
}