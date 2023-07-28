package com.tangem.feature.wallet.presentation.wallet.state.factory

import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletLoadedTokensListConverter.LoadedTokensListModel
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListErrorToWalletStateConverter
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListToWalletStateConverter
import com.tangem.utils.converter.Converter

/**
 * Converter from loaded [TokenListError] or [TokenList] to [WalletStateHolder]
 *
 * @property currentStateProvider current ui state provider
 *
[REDACTED_AUTHOR]
 */
internal class WalletLoadedTokensListConverter(
    private val currentStateProvider: Provider<WalletStateHolder>,
) : Converter<LoadedTokensListModel, WalletStateHolder> {

    private val tokenListStateConverter = TokenListToWalletStateConverter(
        currentState = currentStateProvider(),
        isWalletContentHidden = false, // TODO: [REDACTED_JIRA]
        fiatCurrencyCode = "USD", // TODO: [REDACTED_JIRA]
        fiatCurrencySymbol = "$", // TODO: [REDACTED_JIRA]
    )

    private val tokenListErrorStateConverter = TokenListErrorToWalletStateConverter(
        currentState = currentStateProvider(),
    )

    override fun convert(value: LoadedTokensListModel): WalletStateHolder {
        return value.tokenListEither.fold(
            ifLeft = tokenListErrorStateConverter::convert,
            ifRight = {
                tokenListStateConverter.convert(
                    value = TokenListToWalletStateConverter.TokensListModel(
                        tokenList = it,
                        isRefreshing = value.isRefreshing,
                    ),
                )
            },
        )
    }

    data class LoadedTokensListModel(
        val tokenListEither: Either<TokenListError, TokenList>,
        val isRefreshing: Boolean,
    )
}