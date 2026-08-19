package com.tangem.common.ui.components.currency.icon.converter

import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.utils.converter.Converter

/**
 * Converts [CryptoCurrencyStatus] to [CurrencyIconState]
 *
 * @property isAvailable flag that indicates if the currency is available (affects on icon's grayscale)
 * @property shouldGrayscaleTestnet whether a currency on a testnet network is greyed out. Turn it off where the
 * network is dictated by a backend environment rather than picked by the user (e.g. a Tangem Pay payment
 * account), so that the same screen looks identical on a testnet and a production environment.
 */
class CryptoCurrencyToIconStateConverter(
    private val isAvailable: Boolean = true,
    private val shouldGrayscaleTestnet: Boolean = true,
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
            isGrayscale = forceGrayscale || coin.network.isGrayscaleTestnet() || isUnreachable || !isAvailable,
            shouldShowCustomBadge = coin.isCustom && showCustomBadge,
        )
    }

    private fun getIconStateForToken(
        token: CryptoCurrency.Token,
        isErrorStatus: Boolean,
        showCustomBadge: Boolean = true,
        forceGrayscale: Boolean = false,
    ): CurrencyIconState {
        val isGrayscale = forceGrayscale || token.network.isGrayscaleTestnet() || isErrorStatus || !isAvailable
        val background = token.tryGetBackgroundForTokenIcon(isGrayscale)
        val tint = getTintForTokenIcon(background)

        return if (token.isCustom && token.iconUrl == null) {
            CurrencyIconState.CustomTokenIcon(
                tint = tint,
                background = background,
                topBadgeIconResId = token.networkIconResId,
                isGrayscale = isGrayscale,
                shouldShowCustomBadge = showCustomBadge,
            )
        } else {
            CurrencyIconState.TokenIcon(
                url = token.iconUrl,
                topBadgeIconResId = token.networkIconResId,
                isGrayscale = isGrayscale,
                fallbackTint = tint,
                fallbackBackground = background,
                shouldShowCustomBadge = token.isCustom && showCustomBadge,
            )
        }
    }

    private fun Network.isGrayscaleTestnet(): Boolean = isTestnet && shouldGrayscaleTestnet
}