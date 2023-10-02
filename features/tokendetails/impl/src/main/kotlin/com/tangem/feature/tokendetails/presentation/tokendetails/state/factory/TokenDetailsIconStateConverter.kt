package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenInfoBlockState
import com.tangem.utils.converter.Converter

internal class TokenDetailsIconStateConverter : Converter<CryptoCurrency, TokenInfoBlockState.IconState> {

    override fun convert(value: CryptoCurrency): TokenInfoBlockState.IconState {
        return when (value) {
            is CryptoCurrency.Coin -> getIconStateForCoin(value)
            is CryptoCurrency.Token -> getIconStateForToken(value)
        }
    }

    private fun getIconStateForCoin(coin: CryptoCurrency.Coin): TokenInfoBlockState.IconState.CoinIcon {
        return TokenInfoBlockState.IconState.CoinIcon(
            url = coin.iconUrl,
            fallbackResId = coin.networkIconResId,
            isGrayscale = coin.network.isTestnet,
        )
    }

    private fun getIconStateForToken(token: CryptoCurrency.Token): TokenInfoBlockState.IconState {
        val isGrayscale = token.network.isTestnet
        val background = token.tryGetBackgroundForTokenIcon(isGrayscale)
        val tint = getTintForTokenIcon(background)

        return if (token.isCustom) {
            TokenInfoBlockState.IconState.CustomTokenIcon(
                tint = tint,
                background = background,
                isGrayscale = isGrayscale,
            )
        } else {
            TokenInfoBlockState.IconState.TokenIcon(
                url = token.iconUrl,
                isGrayscale = isGrayscale,
                fallbackTint = tint,
                fallbackBackground = background,
            )
        }
    }
}