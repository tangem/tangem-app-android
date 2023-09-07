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
        )
    }

    private fun getIconStateForToken(token: CryptoCurrency.Token, isErrorStatus: Boolean): TokenItemState.IconState {
        val background = token.tryGetBackgroundForTokenIcon()
        val tint = getTintForTokenIcon(background)

        return if (token.isCustom) {
            TokenItemState.IconState.CustomTokenIcon(
                tint = tint,
                background = background,
                networkBadgeIconResId = token.networkIconResId,
                isGrayscale = token.network.isTestnet || isErrorStatus,
            )
        } else {
            TokenItemState.IconState.TokenIcon(
                url = token.iconUrl,
                networkBadgeIconResId = token.networkIconResId,
                isGrayscale = token.network.isTestnet || isErrorStatus,
                fallbackTint = tint,
                fallbackBackground = background,
            )
        }
    }
}