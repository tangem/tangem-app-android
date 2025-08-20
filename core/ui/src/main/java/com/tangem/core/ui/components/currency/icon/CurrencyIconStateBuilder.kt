package com.tangem.core.ui.components.currency.icon

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.domain.models.currency.CryptoCurrency

object CurrencyIconStateBuilder {

    fun build(
        cryptoCurrency: CryptoCurrency,
        isGrayscale: Boolean = false,
        showCustomBadge: Boolean = true,
    ): CurrencyIconState = when (cryptoCurrency) {
        is CryptoCurrency.Coin -> fromCoin(cryptoCurrency, isGrayscale, showCustomBadge)
        is CryptoCurrency.Token -> fromToken(cryptoCurrency, isGrayscale, showCustomBadge)
    }

    private fun createCoinIcon(
        url: String? = null,
        @DrawableRes fallbackResId: Int = R.drawable.ic_empty_64,
        isGrayscale: Boolean = false,
        showCustomBadge: Boolean = false,
    ): CurrencyIconState.CoinIcon = CurrencyIconState.CoinIcon(
        url = url,
        fallbackResId = fallbackResId,
        isGrayscale = isGrayscale,
        showCustomBadge = showCustomBadge,
    )

    private fun createTokenIcon(
        url: String? = null,
        @DrawableRes topBadgeIconResId: Int? = null,
        isGrayscale: Boolean = false,
        showCustomBadge: Boolean = false,
        fallbackTint: Color = Color.Black,
        fallbackBackground: Color = Color.White,
    ): CurrencyIconState.TokenIcon = CurrencyIconState.TokenIcon(
        url = url,
        topBadgeIconResId = topBadgeIconResId,
        isGrayscale = isGrayscale,
        fallbackTint = fallbackTint,
        fallbackBackground = fallbackBackground,
        showCustomBadge = showCustomBadge,
    )

    private fun createCustomTokenIcon(
        tint: Color,
        background: Color,
        @DrawableRes topBadgeIconResId: Int,
        isGrayscale: Boolean = false,
        showCustomBadge: Boolean = true,
    ): CurrencyIconState.CustomTokenIcon = CurrencyIconState.CustomTokenIcon(
        tint = tint,
        background = background,
        topBadgeIconResId = topBadgeIconResId,
        isGrayscale = isGrayscale,
        showCustomBadge = showCustomBadge,
    )

    private fun fromCoin(
        coin: CryptoCurrency.Coin,
        isGrayscale: Boolean = false,
        showCustomBadge: Boolean = true,
    ): CurrencyIconState.CoinIcon = createCoinIcon(
        url = coin.iconUrl,
        fallbackResId = coin.networkIconResId,
        isGrayscale = isGrayscale || coin.network.isTestnet,
        showCustomBadge = coin.isCustom && showCustomBadge,
    )

    private fun fromToken(
        token: CryptoCurrency.Token,
        isGrayscale: Boolean = false,
        showCustomBadge: Boolean = true,
    ): CurrencyIconState {
        val grayScale = isGrayscale || token.network.isTestnet
        val background = token.tryGetBackgroundForTokenIcon(grayScale)
        val tint = getTintForTokenIcon(background)

        return if (token.isCustom && token.iconUrl == null) {
            createCustomTokenIcon(
                tint = tint,
                background = background,
                topBadgeIconResId = token.networkIconResId,
                isGrayscale = grayScale,
                showCustomBadge = showCustomBadge,
            )
        } else {
            createTokenIcon(
                url = token.iconUrl,
                topBadgeIconResId = token.networkIconResId,
                isGrayscale = grayScale,
                fallbackTint = tint,
                fallbackBackground = background,
                showCustomBadge = token.isCustom && showCustomBadge,
            )
        }
    }
}