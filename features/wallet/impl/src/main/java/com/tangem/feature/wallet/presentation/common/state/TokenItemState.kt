package com.tangem.feature.wallet.presentation.common.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.marketprice.PriceChangeConfig
import com.tangem.core.ui.extensions.TextReference

/** Token item state */
@Immutable
internal sealed interface TokenItemState {

    /** Unique id */
    val id: String

    /** Loading token state */
    data class Loading(override val id: String) : TokenItemState

    /** Locked token state */
    data class Locked(override val id: String) : TokenItemState

    /** Content state */
    @Immutable
    sealed class ContentState : TokenItemState {

        abstract val icon: IconState
        abstract val name: String
    }

    /**
     * Content token state
     *
     * @property id                    unique id
     * @property icon                  token icon state
     * @property name                  token name
     * @property amount                amount of token
     * @property hasPending            pending tx in blockchain
     * @property tokenOptions          state for token options
     * @property onItemClick           callback which will be called when an item is clicked
     * @property onItemLongClick       callback which will be called when an item is long clicked
     */
    data class Content(
        override val id: String,
        override val icon: IconState,
        override val name: String,
        val amount: String,
        val hasPending: Boolean,
        val tokenOptions: TokenOptionsState,
        val onItemClick: () -> Unit,
        val onItemLongClick: () -> Unit,
    ) : ContentState()

    /**
     * Draggable token state
     *
     * @property id                    unique id
     * @property icon                  token icon state
     * @property name                  token name
     * @property info                  token info (e.g. fiat balance or status)
     */
    data class Draggable(
        override val id: String,
        override val icon: IconState,
        override val name: String,
        val info: TextReference,
    ) : ContentState()

    /**
     * Unreachable token state
     *
     * @property id                    token id
     * @property icon                  token icon state
     * @property name                  token name
     */
    data class Unreachable(
        override val id: String,
        override val icon: IconState,
        override val name: String,
    ) : ContentState()

    /**
     * Represents the various states an icon can be in.
     */
    @Immutable
    sealed class IconState {

        abstract val networkBadgeIconResId: Int?
        abstract val isGrayscale: Boolean

        /**
         * Represents a coin icon.
         *
         * @property url The URL where the coin icon can be fetched from. May be `null` if not found.
         * @property fallbackResId The drawable resource ID to be used as a fallback if the URL is not available.
         * @property isGrayscale Specifies whether to show the icon in grayscale.
         */
        data class CoinIcon(
            val url: String?,
            @DrawableRes val fallbackResId: Int,
            override val isGrayscale: Boolean,
        ) : IconState() {

            override val networkBadgeIconResId: Int? = null
        }

        /**
         * Represents a token icon.
         *
         * @property url The URL where the token icon can be fetched from. May be `null` if not found.
         * @property networkBadgeIconResId The drawable resource ID for the network badge.
         * @property isGrayscale Specifies whether to show the icon in grayscale.
         * @property fallbackTint The color to be used for tinting the fallback icon.
         * @property fallbackBackground The background color to be used for the fallback icon.
         */
        data class TokenIcon(
            val url: String?,
            @DrawableRes override val networkBadgeIconResId: Int,
            override val isGrayscale: Boolean,
            val fallbackTint: Color,
            val fallbackBackground: Color,
        ) : IconState()

        /**
         * Represents a custom token icon.
         *
         * @property tint The color to be used for tinting the icon.
         * @property background The background color to be used for the icon.
         * @property networkBadgeIconResId The drawable resource ID for the network badge.
         * @property isGrayscale Specifies whether to show the icon in grayscale.
         */
        data class CustomTokenIcon(
            val tint: Color,
            val background: Color,
            @DrawableRes override val networkBadgeIconResId: Int,
            override val isGrayscale: Boolean,
        ) : IconState()
    }

    /** Token options state */
    @Immutable
    sealed interface TokenOptionsState {

        val config: PriceChangeConfig

        /**
         * Visible token options state
         *
         * @property fiatAmount fiat amount of token
         * @property config     value of price changing
         */
        data class Visible(
            override val config: PriceChangeConfig,
            val fiatAmount: String,
        ) : TokenOptionsState

        /**
         * Hidden token options state
         *
         * @property config value of price changing
         */
        data class Hidden(override val config: PriceChangeConfig) : TokenOptionsState
    }

    companion object {
        const val DOTS = "•••"
    }
}