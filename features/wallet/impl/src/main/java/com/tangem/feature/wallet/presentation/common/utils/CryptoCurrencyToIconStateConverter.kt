package com.tangem.feature.wallet.presentation.common.utils

import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.utils.converter.Converter

internal class CryptoCurrencyToIconStateConverter : Converter<CryptoCurrencyStatus, TokenItemState.IconState> {

    override fun convert(value: CryptoCurrencyStatus): TokenItemState.IconState {
        return when (val currency = value.currency) {
            is CryptoCurrency.Coin -> getIconStateForCoin(currency, value.value.isError)
            is CryptoCurrency.Token -> getIconStateForToken(currency, value.value.isError)
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
            showCustomBadge = coin.isCustom,
        )
    }

    private fun getIconStateForToken(token: CryptoCurrency.Token, isErrorStatus: Boolean): TokenItemState.IconState {
        val isGrayscale = token.network.isTestnet || isErrorStatus
        val background = token.tryGetBackgroundForTokenIcon(isGrayscale)
        val tint = getTintForTokenIcon(background)

        return if (token.isCustom && token.iconUrl == null) {
            TokenItemState.IconState.CustomTokenIcon(
                tint = tint,
                background = background,
                networkBadgeIconResId = token.networkIconResId,
                isGrayscale = isGrayscale,
            )
        } else {
            TokenItemState.IconState.TokenIcon(
                url = token.iconUrl,
                networkBadgeIconResId = token.networkIconResId,
                isGrayscale = isGrayscale,
                fallbackTint = tint,
                fallbackBackground = background,
                showCustomBadge = token.isCustom, // `true` for tokens with custom derivation
            )
        }
    }
}
