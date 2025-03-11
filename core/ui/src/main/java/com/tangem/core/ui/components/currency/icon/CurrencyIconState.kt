package com.tangem.core.ui.components.currency.icon

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.R

/**
 * Represents the various states an icon can be in.
 *
 * [REDACTED_TODO_COMMENT]
 */
@Immutable
sealed class CurrencyIconState {

    abstract val isGrayscale: Boolean
    abstract val showCustomBadge: Boolean
    abstract val topBadgeIconResId: Int?

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
    ) : CurrencyIconState() {

        override val topBadgeIconResId: Int? = null
    }

    /**
     * Represents a token icon.
     *
     * @property url The URL where the token icon can be fetched from. May be `null` if not found.
     * @property topBadgeIconResId The drawable resource ID for the network badge. May be `null`.
     * @property isGrayscale Specifies whether to show the icon in grayscale.
     * @property showCustomBadge Specifies whether to show the custom token badge.
     * @property fallbackTint The color to be used for tinting the fallback icon.
     * @property fallbackBackground The background color to be used for the fallback icon.
     */
    data class TokenIcon(
        val url: String?,
        @DrawableRes override val topBadgeIconResId: Int?,
        override val isGrayscale: Boolean,
        override val showCustomBadge: Boolean,
        val fallbackTint: Color,
        val fallbackBackground: Color,
    ) : CurrencyIconState()

    /**
     * Represents a custom token icon.
     *
     * @property tint The color to be used for tinting the icon.
     * @property background The background color to be used for the icon.
     * @property topBadgeIconResId The drawable resource ID for the network badge.
     * @property isGrayscale Specifies whether to show the icon in grayscale.
     * @property showCustomBadge Specifies whether to show the custom token badge.
     */
    data class CustomTokenIcon(
        val tint: Color,
        val background: Color,
        @DrawableRes override val topBadgeIconResId: Int,
        override val isGrayscale: Boolean,
        override val showCustomBadge: Boolean = true,
    ) : CurrencyIconState()

    /**
     * Represents a fiat icon.
     *
     * @property url The URL where the fiat icon can be fetched from. May be `null` if not found.
     * @property fallbackResId The drawable resource ID to be used as a fallback if the URL is not available.
     */
    data class FiatIcon(
        val url: String?,
        @DrawableRes val fallbackResId: Int,
    ) : CurrencyIconState() {
        override val isGrayscale: Boolean = false
        override val showCustomBadge: Boolean = false
        override val topBadgeIconResId: Int? = null
    }

    data object Loading : CurrencyIconState() {
        override val isGrayscale: Boolean = false
        override val showCustomBadge: Boolean = false
        override val topBadgeIconResId: Int? = null
    }

    data object Locked : CurrencyIconState() {
        override val isGrayscale: Boolean = false
        override val showCustomBadge: Boolean = false
        override val topBadgeIconResId: Int? = null
    }

    data class Empty(
        @DrawableRes val resId: Int = R.drawable.ic_empty_64,
    ) : CurrencyIconState() {
        override val isGrayscale: Boolean = true
        override val showCustomBadge: Boolean = false
        override val topBadgeIconResId: Int? = null
    }

    fun copySealed(
        isGrayscale: Boolean = this.isGrayscale,
        showCustomBadge: Boolean = this.showCustomBadge,
        topBadgeIconResId: Int? = this.topBadgeIconResId,
    ): CurrencyIconState = when (this) {
        is CoinIcon -> copy(
            isGrayscale = isGrayscale,
            showCustomBadge = showCustomBadge,
        )
        is CustomTokenIcon -> copy(
            isGrayscale = isGrayscale,
            showCustomBadge = showCustomBadge,
            topBadgeIconResId = topBadgeIconResId ?: this.topBadgeIconResId,
        )
        is TokenIcon -> copy(
            isGrayscale = isGrayscale,
            showCustomBadge = showCustomBadge,
            topBadgeIconResId = topBadgeIconResId,
        )
        is Loading,
        is Locked,
        is Empty,
        is FiatIcon,
        -> this
    }
}