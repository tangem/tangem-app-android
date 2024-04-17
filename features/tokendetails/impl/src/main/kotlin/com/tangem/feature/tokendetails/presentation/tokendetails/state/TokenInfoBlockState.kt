package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

internal data class TokenInfoBlockState(
    val name: String,
    val iconState: IconState,
    val currency: Currency,
) {
    @Immutable
    sealed class Currency {
        object Native : Currency()

        /**
         * @param standardName - token standard. Samples: ERC20, BEP20, BEP2, TRC20 and etc.
         * @param networkName - token's network name.
         * @param networkIcon - token's network icon.
         */
        data class Token(
            val standardName: String?,
            val networkName: String,
            @DrawableRes val networkIcon: Int,
        ) : Currency()
    }

    @Immutable
    sealed class IconState {

        abstract val isGrayscale: Boolean

        data class CoinIcon(
            val url: String?,
            @DrawableRes val fallbackResId: Int,
            override val isGrayscale: Boolean,
        ) : IconState()

        data class TokenIcon(
            val url: String?,
            val fallbackTint: Color,
            val fallbackBackground: Color,
            override val isGrayscale: Boolean,
        ) : IconState()

        data class CustomTokenIcon(
            val tint: Color,
            val background: Color,
            override val isGrayscale: Boolean,
        ) : IconState()
    }
}