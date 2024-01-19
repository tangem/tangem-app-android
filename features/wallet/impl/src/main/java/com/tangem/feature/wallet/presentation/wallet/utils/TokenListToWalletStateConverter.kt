package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.feature.wallet.presentation.wallet.state.factory.TokenListWithWallet
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

@Suppress("LongParameterList")
internal class TokenListToWalletStateConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val currentWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    clickIntents: WalletClickIntents,
) : Converter<TokenListWithWallet, WalletState> {

    private val tokenListToContentConverter = TokenListToContentItemsConverter(
        appCurrencyProvider = appCurrencyProvider,
        clickIntents = clickIntents,
    )

    override fun convert(value: TokenListWithWallet): WalletState {
        val tokenList = value.tokenList
        val isSingleCurrencyWalletWithToken = value.wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
        return when (val state = currentStateProvider()) {
            is WalletMultiCurrencyState.Content -> {
                state.copy(
                    walletsListConfig = state.updateSelectedWallet(fiatBalance = tokenList.totalFiatBalance),
                    tokensListState = tokenListToContentConverter.convert(value = value),
                    isManageTokensAvailable = !isSingleCurrencyWalletWithToken,
                )
            }
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Content,
            is WalletSingleCurrencyState.Locked,
            is WalletState.Initial,
            -> state
        }
    }

    private fun WalletMultiCurrencyState.updateSelectedWallet(fiatBalance: TokenList.FiatBalance): WalletsListConfig {
        val selectedWalletIndex = walletsListConfig.selectedWalletIndex
        val selectedWalletCard = walletsListConfig.wallets[selectedWalletIndex]
        val converter = FiatBalanceToWalletCardConverter(
            currentState = selectedWalletCard,
            currentWalletProvider = currentWalletProvider,
            appCurrencyProvider = appCurrencyProvider,
        )

        return walletsListConfig.copy(
            wallets = walletsListConfig.wallets.toPersistentList()
                .set(index = selectedWalletIndex, element = converter.convert(fiatBalance)),
        )
    }
}