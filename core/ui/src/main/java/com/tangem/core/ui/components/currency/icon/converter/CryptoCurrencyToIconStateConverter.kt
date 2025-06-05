package com.tangem.core.ui.components.currency.icon.converter

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.converter.Converter

/**
 * Converts [CryptoCurrencyStatus] to [CurrencyIconState]
 *
 * @property isAvailable flag that indicates if the currency is available (affects on icon's grayscale)
 */
class CryptoCurrencyToIconStateConverter(
    private val isAvailable: Boolean = true,
) : Converter<CryptoCurrencyStatus, CurrencyIconState> {

    override fun convert(value: CryptoCurrencyStatus): CurrencyIconState {
        return when (val currency = value.currency) {
            is CryptoCurrency.Coin -> getIconStateForCoin(currency, value.value.isError)
            is CryptoCurrency.Token -> getIconStateForToken(currency, value.value.isError)
        }
    }

    fun convertCustom(
        value: CryptoCurrencyStatus,
        forceGrayscale: Boolean,
        showCustomTokenBadge: Boolean,
    ): CurrencyIconState {
        return when (val currency = value.currency) {
            is CryptoCurrency.Coin -> getIconStateForCoin(
                coin = currency,
                isUnreachable = value.value.isError,
                forceGrayscale = forceGrayscale,
                showCustomBadge = showCustomTokenBadge,
            )
            is CryptoCurrency.Token -> getIconStateForToken(
                token = currency,
                isErrorStatus = value.value.isError,
                forceGrayscale = forceGrayscale,
                showCustomBadge = showCustomTokenBadge,
            )
        }
    }

    fun convert(currency: CryptoCurrency): CurrencyIconState {
        return when (currency) {
            is CryptoCurrency.Coin -> getIconStateForCoin(currency, isUnreachable = false)
            is CryptoCurrency.Token -> getIconStateForToken(currency, isErrorStatus = false)
        }
    }

    private fun getIconStateForCoin(
        coin: CryptoCurrency.Coin,
        isUnreachable: Boolean,
        showCustomBadge: Boolean = true,
        forceGrayscale: Boolean = false,
    ): CurrencyIconState.CoinIcon {
        return CurrencyIconState.CoinIcon(
            url = coin.iconUrl,
            fallbackResId = coin.networkIconResId,
            isGrayscale = forceGrayscale || coin.network.isTestnet || isUnreachable || !isAvailable,
            showCustomBadge = coin.isCustom && showCustomBadge,
        )
    }

    private fun getIconStateForToken(
        token: CryptoCurrency.Token,
        isErrorStatus: Boolean,
        showCustomBadge: Boolean = true,
        forceGrayscale: Boolean = false,
    ): CurrencyIconState {
        val grayScale = forceGrayscale || token.network.isTestnet || isErrorStatus || !isAvailable
        val background = token.tryGetBackgroundForTokenIcon(grayScale)
        val tint = getTintForTokenIcon(background)

        return if (token.isCustom && token.iconUrl == null) {
            CurrencyIconState.CustomTokenIcon(
                tint = tint,
                background = background,
                topBadgeIconResId = token.networkIconResId,
                isGrayscale = grayScale,
                showCustomBadge = showCustomBadge,
            )
        } else {
            CurrencyIconState.TokenIcon(
                url = token.iconUrl,
                topBadgeIconResId = token.networkIconResId,
                isGrayscale = grayScale,
                fallbackTint = tint,
                fallbackBackground = background,
                showCustomBadge = token.isCustom && showCustomBadge, // `true` for tokens with custom derivation
            )
        }
    }
}