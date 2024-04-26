package com.tangem.core.ui.components.currency.tokenicon

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Represents the various states an icon can be in.
 *
* [REDACTED_TODO_COMMENT]
 */
@Immutable
sealed class TokenIconState {

    abstract val isGrayscale: Boolean
    abstract val showCustomBadge: Boolean
    abstract val networkBadgeIconResId: Int?

    /**
     * Represents a coin icon.
     *
     * @property url The URL where the coin icon can be fetched from. May be `null` if not found.
     * @property fallbackResId The drawable resource ID to be used as a fallback if the URL is not available.
     * @property isGrayscale Specifies whether to show the icon in grayscale.
     * @property showCustomBadge Specifies whether to show the custom token badge.
     */
    data class CoinIcon(
        val url: String?,
        @DrawableRes val fallbackResId: Int,
        override val isGrayscale: Boolean,
        override val showCustomBadge: Boolean,
    ) : TokenIconState() {

        override val networkBadgeIconResId: Int? = null
    }

    /**
     * Represents a token icon.
     *
     * @property url The URL where the token icon can be fetched from. May be `null` if not found.
     * @property networkBadgeIconResId The drawable resource ID for the network badge.
     * @property isGrayscale Specifies whether to show the icon in grayscale.
     * @property showCustomBadge Specifies whether to show the custom token badge.
     * @property fallbackTint The color to be used for tinting the fallback icon.
     * @property fallbackBackground The background color to be used for the fallback icon.
     */
    data class TokenIcon(
        val url: String?,
        @DrawableRes override val networkBadgeIconResId: Int,
        override val isGrayscale: Boolean,
        override val showCustomBadge: Boolean,
        val fallbackTint: Color,
        val fallbackBackground: Color,
    ) : TokenIconState()

    /**
     * Represents a custom token icon.
     *
     * @property tint The color to be used for tinting the icon.
     * @property background The background color to be used for the icon.
     * @property networkBadgeIconResId The drawable resource ID for the network badge.
     * @property isGrayscale Specifies whether to show the icon in grayscale.
     * @property showCustomBadge Specifies whether to show the custom token badge.
     */
    data class CustomTokenIcon(
        val tint: Color,
        val background: Color,
        @DrawableRes override val networkBadgeIconResId: Int,
        override val isGrayscale: Boolean,
        override val showCustomBadge: Boolean = true,
    ) : TokenIconState()

    data object Loading : TokenIconState() {
        override val isGrayscale: Boolean = false
        override val showCustomBadge: Boolean = false
        override val networkBadgeIconResId: Int? = null
    }

    data object Locked : TokenIconState() {
        override val isGrayscale: Boolean = false
        override val showCustomBadge: Boolean = false
        override val networkBadgeIconResId: Int? = null
    }
}
