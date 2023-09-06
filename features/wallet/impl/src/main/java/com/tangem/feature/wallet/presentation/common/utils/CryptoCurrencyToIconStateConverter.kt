package com.tangem.feature.wallet.presentation.common.utils

import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.utils.converter.Converter

internal class CryptoCurrencyToIconStateConverter : Converter<CryptoCurrencyStatus, TokenItemState.IconState> {

    override fun convert(value: CryptoCurrencyStatus): TokenItemState.IconState {
        val isUnreachable = when (value.value) {
            is CryptoCurrencyStatus.Loading,
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.NoQuote,
            -> false
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            -> true
        }

        return when (val currency = value.currency) {
            is CryptoCurrency.Coin -> getIconStateForCoin(currency, isUnreachable)
            is CryptoCurrency.Token -> getIconStateForToken(currency, isUnreachable)
        }
    }

    private fun getIconStateForCoin(
        coin: CryptoCurrency.Coin,
        isUnreachable: Boolean,
    ): TokenItemState.IconState.CoinIcon {
        return TokenItemState.IconState.CoinIcon(
            url = coin.iconUrl,
            fallbackResId = coin.networkIconResId,
            isGrayscale = coin.network.isTestnet || isUnreachable,
        )
    }

    private fun getIconStateForToken(token: CryptoCurrency.Token, isUnreachable: Boolean): TokenItemState.IconState {
        val background = token.tryGetBackgroundForTokenIcon()
        val tint = getTintForTokenIcon(background)

        return if (token.isCustom) {
            TokenItemState.IconState.CustomTokenIcon(
                tint = tint,
                background = background,
                networkBadgeIconResId = token.networkIconResId,
                isGrayscale = token.network.isTestnet || isUnreachable,
            )
        } else {
            TokenItemState.IconState.TokenIcon(
                url = token.iconUrl,
                networkBadgeIconResId = token.networkIconResId,
                isGrayscale = token.network.isTestnet || isUnreachable,
                fallbackTint = tint,
                fallbackBackground = background,
            )
        }
    }
}
