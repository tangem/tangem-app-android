package com.tangem.feature.wallet.presentation.wallet.state.factory

import arrow.core.Either
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListErrorConverter
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListToWalletStateConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

/**
 * Converter from loaded [TokenListError] or [TokenList] to [WalletMultiCurrencyState]
 *
 * @property currentStateProvider    current ui state provider
 * @property tokenListErrorConverter converter of tokens list
 * @param appCurrencyProvider        app currency provider
 * @param currentWalletProvider      current wallet provider
 * @param clickIntents               screen click intents
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class WalletLoadedTokensListConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val tokenListErrorConverter: TokenListErrorConverter,
    appCurrencyProvider: Provider<AppCurrency>,
    currentWalletProvider: Provider<UserWallet>,
    clickIntents: WalletClickIntents,
) : Converter<Either<TokenListError, TokenListWithWallet>, WalletState> {

    private val tokenListStateConverter = TokenListToWalletStateConverter(
        currentStateProvider = currentStateProvider,
        currentWalletProvider = currentWalletProvider,
        appCurrencyProvider = appCurrencyProvider,
        clickIntents = clickIntents,
    )

    override fun convert(value: Either<TokenListError, TokenListWithWallet>): WalletState {
        return value.fold(
            ifLeft = tokenListErrorConverter::convert,
            ifRight = tokenListStateConverter::convert,
        )
    }
}