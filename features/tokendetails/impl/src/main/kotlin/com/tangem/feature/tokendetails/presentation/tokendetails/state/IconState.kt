package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal sealed class IconState {

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