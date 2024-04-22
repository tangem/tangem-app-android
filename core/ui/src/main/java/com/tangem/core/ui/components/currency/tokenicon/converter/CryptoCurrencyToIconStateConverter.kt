package com.tangem.core.ui.components.currency.tokenicon.converter

import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.converter.Converter

/**
 * Converts [CryptoCurrencyStatus] to [TokenIconState]
 */
class CryptoCurrencyToIconStateConverter : Converter<CryptoCurrencyStatus, TokenIconState> {

    override fun convert(value: CryptoCurrencyStatus): TokenIconState {
        return when (val currency = value.currency) {
            is CryptoCurrency.Coin -> getIconStateForCoin(currency, value.value.isError)
            is CryptoCurrency.Token -> getIconStateForToken(currency, value.value.isError)
        }
    }

    fun convertCustom(
        value: CryptoCurrencyStatus,
        forceGrayscale: Boolean,
        showCustomTokenBadge: Boolean,
    ): TokenIconState {
        return when (val currency = value.currency) {
            is CryptoCurrency.Coin -> getIconStateForCoin(
                coin = currency,
                isUnreachable = value.value.isError,
                forceGrayscale = forceGrayscale,
            )
            is CryptoCurrency.Token -> getIconStateForToken(
                token = currency,
                isErrorStatus = value.value.isError,
                forceGrayscale = forceGrayscale,
                showCustomBadge = showCustomTokenBadge,
            )
        }
    }

    fun convert(currency: CryptoCurrency): TokenIconState {
        return when (currency) {
            is CryptoCurrency.Coin -> getIconStateForCoin(currency, isUnreachable = false)
            is CryptoCurrency.Token -> getIconStateForToken(currency, isErrorStatus = false)
        }
    }

    private fun getIconStateForCoin(
        coin: CryptoCurrency.Coin,
        isUnreachable: Boolean,
        forceGrayscale: Boolean = false,
    ): TokenIconState.CoinIcon {
        return TokenIconState.CoinIcon(
            url = coin.iconUrl,
            fallbackResId = coin.networkIconResId,
            isGrayscale = forceGrayscale || coin.network.isTestnet || isUnreachable,
            showCustomBadge = coin.isCustom,
        )
    }

    private fun getIconStateForToken(
        token: CryptoCurrency.Token,
        isErrorStatus: Boolean,
        showCustomBadge: Boolean = true,
        forceGrayscale: Boolean = false,
    ): TokenIconState {
        val grayScale = forceGrayscale || token.network.isTestnet || isErrorStatus
        val background = token.tryGetBackgroundForTokenIcon(grayScale)
        val tint = getTintForTokenIcon(background)

        return if (token.isCustom && token.iconUrl == null) {
            TokenIconState.CustomTokenIcon(
                tint = tint,
                background = background,
                networkBadgeIconResId = token.networkIconResId,
                isGrayscale = grayScale,
            )
        } else {
            TokenIconState.TokenIcon(
                url = token.iconUrl,
                networkBadgeIconResId = token.networkIconResId,
                isGrayscale = grayScale,
                fallbackTint = tint,
                fallbackBackground = background,
                showCustomBadge = token.isCustom && showCustomBadge, // `true` for tokens with custom derivation
            )
        }
    }
}
