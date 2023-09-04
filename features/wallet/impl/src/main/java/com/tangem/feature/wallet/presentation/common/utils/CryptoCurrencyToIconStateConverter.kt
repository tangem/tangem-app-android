package com.tangem.feature.wallet.presentation.common.utils

import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.utils.converter.Converter

internal class CryptoCurrencyToIconStateConverter : Converter<CryptoCurrency, TokenItemState.IconState> {

    override fun convert(value: CryptoCurrency): TokenItemState.IconState {
        return when (value) {
            is CryptoCurrency.Coin -> getIconStateForCoin(value)
            is CryptoCurrency.Token -> getIconStateForToken(value)
        }
    }

    private fun getIconStateForCoin(coin: CryptoCurrency.Coin): TokenItemState.IconState.CoinIcon {
        return TokenItemState.IconState.CoinIcon(
            url = coin.iconUrl,
            fallbackResId = coin.networkIconResId,
            isGrayscale = coin.network.isTestnet,
        )
    }

    private fun getIconStateForToken(token: CryptoCurrency.Token): TokenItemState.IconState {
        val background = token.tryGetBackgroundForTokenIcon()
        val tint = getTintForTokenIcon(background)

        return if (token.isCustom) {
            TokenItemState.IconState.CustomTokenIcon(
                tint = tint,
                background = background,
                networkBadgeIconResId = token.networkIconResId,
                isGrayscale = token.network.isTestnet,
            )
        } else {
            TokenItemState.IconState.TokenIcon(
                url = token.iconUrl,
                networkBadgeIconResId = token.networkIconResId,
                isGrayscale = token.network.isTestnet,
                fallbackTint = tint,
                fallbackBackground = background,
            )
        }
    }
}