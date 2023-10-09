package com.tangem.feature.wallet.presentation.common.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.marketprice.PriceChangeType

/** Token item state */
@Immutable
internal sealed class TokenItemState {

    abstract val id: String

    abstract val iconState: IconState

    abstract val titleState: TitleState

    abstract val fiatAmountState: FiatAmountState?

    abstract val cryptoAmountState: CryptoAmountState?

    abstract val priceChangeState: PriceChangeState?

    /** Loading token state */
    data class Loading(override val id: String) : TokenItemState() {
        override val iconState: IconState = IconState.Loading
        override val titleState: TitleState = TitleState.Loading
        override val fiatAmountState: FiatAmountState = FiatAmountState.Loading
        override val cryptoAmountState: CryptoAmountState = CryptoAmountState.Loading
        override val priceChangeState: PriceChangeState = PriceChangeState.Loading
    }

    /** Locked token state */
    data class Locked(override val id: String) : TokenItemState() {
        override val iconState: IconState = IconState.Locked
        override val titleState: TitleState = TitleState.Locked
        override val fiatAmountState: FiatAmountState = FiatAmountState.Locked
        override val cryptoAmountState: CryptoAmountState = CryptoAmountState.Locked
        override val priceChangeState: PriceChangeState = PriceChangeState.Locked
    }

    /**
     * Content token state
     *
     * @property id                    unique id
     * @property iconState             token icon state
     * @property titleState            token name
     * @property onItemClick           callback which will be called when an item is clicked
     * @property onItemLongClick       callback which will be called when an item is long clicked
     */
    data class Content(
        override val id: String,
        override val iconState: IconState,
        override val titleState: TitleState,
        override val fiatAmountState: FiatAmountState,
        override val cryptoAmountState: CryptoAmountState.Content,
        override val priceChangeState: PriceChangeState?,
        val isBalanceHidden: Boolean,
        val onItemClick: () -> Unit,
        val onItemLongClick: () -> Unit,
    ) : TokenItemState()

    /**
     * Draggable token state
     *
     * @property id                    unique id
     * @property iconState             token icon state
     * @property titleState            token name
     */
    data class Draggable(
        override val id: String,
        override val iconState: IconState,
        override val titleState: TitleState,
        override val cryptoAmountState: CryptoAmountState,
        val isBalanceHidden: Boolean,
    ) : TokenItemState() {
        override val fiatAmountState: FiatAmountState? = null
        override val priceChangeState: PriceChangeState? = null
    }

    /**
     * Unreachable token state
     *
     * @property id                    token id
     * @property iconState             token icon state
     * @property titleState            token name
     * @property onItemClick           callback which will be called when an item is clicked
     * @property onItemLongClick       callback which will be called when an item is long clicked
     */
    data class Unreachable(
        override val id: String,
        override val iconState: IconState,
        override val titleState: TitleState,
        val onItemClick: () -> Unit,
        val onItemLongClick: () -> Unit,
    ) : TokenItemState() {
        override val fiatAmountState: FiatAmountState? = null
        override val cryptoAmountState: CryptoAmountState? = null
        override val priceChangeState: PriceChangeState? = null
    }

    /**
     * No derivation address state
     *
     * @property id                     token id
     * @property iconState              token icon state
     * @property titleState             token name
     * @property onItemLongClick        callback which will be called when an item is long clicked
     */
    data class NoAddress(
        override val id: String,
        override val iconState: IconState,
        override val titleState: TitleState,
        val onItemLongClick: () -> Unit,
    ) : TokenItemState() {
        override val fiatAmountState: FiatAmountState? = null
        override val cryptoAmountState: CryptoAmountState? = null
        override val priceChangeState: PriceChangeState? = null
    }

    /**
     * Represents the various states an icon can be in.
     */
    @Immutable
    sealed class IconState {

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
        ) : IconState() {

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
        ) : IconState() {

            override val showCustomBadge: Boolean = true
        }

        object Loading : IconState() {
            override val isGrayscale: Boolean = false
            override val showCustomBadge: Boolean = false
            override val networkBadgeIconResId: Int? = null
        }

        object Locked : IconState() {
            override val isGrayscale: Boolean = false
            override val showCustomBadge: Boolean = false
            override val networkBadgeIconResId: Int? = null
        }
    }

    @Immutable
    sealed class TitleState {

        data class Content(val text: String, val hasPending: Boolean = false) : TitleState()

        object Loading : TitleState()

        object Locked : TitleState()
    }

    @Immutable
    sealed class FiatAmountState {
        data class Content(val text: String) : FiatAmountState()

        object Loading : FiatAmountState()

        object Locked : FiatAmountState()
    }

    @Immutable
    sealed class CryptoAmountState {
        data class Content(val text: String) : CryptoAmountState()

        object Unreachable : CryptoAmountState()

        object Loading : CryptoAmountState()

        object Locked : CryptoAmountState()
    }

    sealed class PriceChangeState {

        data class Content(val valueInPercent: String, val type: PriceChangeType) : PriceChangeState()

        object Unknown : PriceChangeState()

        object Loading : PriceChangeState()

        object Locked : PriceChangeState()
    }

    companion object {
        const val UNKNOWN_AMOUNT_SIGN = "—"
    }
}