package com.tangem.feature.wallet.presentation.wallet.state.factory

import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListErrorConverter
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListToWalletStateConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter

/**
 * Converter from loaded [TokenListError] or [TokenList] to [WalletMultiCurrencyState]
 *
 * @property currentStateProvider  current ui state provider
 * @param cardTypeResolverProvider card type resolver
 * @param currentWalletProvider    current wallet provider
 * @param clickIntents             screen click intents
 *
[REDACTED_AUTHOR]
 */
internal class WalletLoadedTokensListConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val tokenListErrorConverter: TokenListErrorConverter,
    appCurrencyProvider: Provider<AppCurrency>,
    cardTypeResolverProvider: Provider<CardTypesResolver>,
    currentWalletProvider: Provider<UserWallet>,
    clickIntents: WalletClickIntents,
) : Converter<Either<TokenListError, TokenList>, WalletState> {

    private val tokenListStateConverter = TokenListToWalletStateConverter(
        currentStateProvider = currentStateProvider,
        cardTypeResolverProvider = cardTypeResolverProvider,
        currentWalletProvider = currentWalletProvider,
        appCurrencyProvider = appCurrencyProvider,
        isWalletContentHidden = false, // TODO: [REDACTED_JIRA]
        clickIntents = clickIntents,
    )

    override fun convert(value: Either<TokenListError, TokenList>): WalletState {
        return value.fold(
            ifLeft = tokenListErrorConverter::convert,
            ifRight = tokenListStateConverter::convert,
        )
    }
}