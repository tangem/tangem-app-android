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
 * @author Andrew Khokhlov on 25/07/2023
 */
internal class WalletLoadedTokensListConverter(
    private val currentStateProvider: Provider<WalletStateHolder>,
) : Converter<LoadedTokensListModel, WalletStateHolder> {

    override fun convert(value: LoadedTokensListModel): WalletStateHolder {
        val updateStateWithError = TokenListErrorToWalletStateConverter(currentState = currentStateProvider())::convert
        val updateState = TokenListToWalletStateConverter(
            currentState = currentStateProvider(),
            isRefreshing = value.isRefreshing,
            isWalletContentHidden = false, // TODO: https://tangem.atlassian.net/browse/AND-4007
            fiatCurrencyCode = "USD", // TODO: https://tangem.atlassian.net/browse/AND-4006
            fiatCurrencySymbol = "$", // TODO: https://tangem.atlassian.net/browse/AND-4006
        )::convert

        return value.tokenListEither.fold(ifLeft = updateStateWithError, ifRight = updateState)
    }

    data class LoadedTokensListModel(
        val tokenListEither: Either<TokenListError, TokenList>,
        val isRefreshing: Boolean,
    )
}
