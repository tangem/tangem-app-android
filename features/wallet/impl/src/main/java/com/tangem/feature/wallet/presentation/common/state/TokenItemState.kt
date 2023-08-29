package com.tangem.feature.wallet.presentation.common.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.marketprice.PriceChangeConfig

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
    sealed class ContentState(
        override val id: String,
        open val tokenIconUrl: String?,
        @DrawableRes open val tokenIconResId: Int,
        @DrawableRes open val networkBadgeIconResId: Int?,
        open val name: String,
    ) : TokenItemState

    /**
     * Content token state
     *
     * @property id                    unique id
     * @property tokenIconUrl          token icon url
     * @property tokenIconResId        token icon resource id
     * @property networkBadgeIconResId network badge icon resource id, may be null if it is a coin
     * @property name                  token name
     * @property amount                amount of token
     * @property hasPending            pending tx in blockchain
     * @property tokenOptions          state for token options
     * @property isTestnet             indicates whether the token is from test network or not
     * @property onItemClick           callback which will be called when an item is clicked
     * @property onItemLongClick       callback which will be called when an item is long clicked
     */
    data class Content(
        override val id: String,
        override val tokenIconUrl: String?,
        @DrawableRes override val tokenIconResId: Int,
        @DrawableRes override val networkBadgeIconResId: Int?,
        override val name: String,
        val amount: String,
        val hasPending: Boolean,
        val tokenOptions: TokenOptionsState,
        val isTestnet: Boolean,
        val onItemClick: () -> Unit,
        val onItemLongClick: () -> Unit,
    ) : ContentState(id, tokenIconUrl, tokenIconResId, networkBadgeIconResId, name)

    /**
     * Draggable token state
     *
     * @property id                    unique id
     * @property tokenIconUrl          token icon url
     * @property tokenIconResId        token icon resource id
     * @property networkBadgeIconResId network badge icon resource id, may be null if it is a coin
     * @property name                  token name
     * @property fiatAmount            fiat amount of token
     * @property isTestnet             indicates whether the token is from test network or not
     */
    data class Draggable(
        override val id: String,
        override val tokenIconUrl: String?,
        @DrawableRes override val tokenIconResId: Int,
        @DrawableRes override val networkBadgeIconResId: Int?,
        override val name: String,
        val fiatAmount: String,
        val isTestnet: Boolean,
    ) : ContentState(id, tokenIconUrl, tokenIconResId, networkBadgeIconResId, name)

    /**
     * Unreachable token state
     *
     * @property id                    token id
     * @property tokenIconUrl          token icon url
     * @property tokenIconResId        token icon resource id
     * @property networkBadgeIconResId network badge icon resource id, may be null if it is a coin
     * @property name                  token name
     */
    data class Unreachable(
        override val id: String,
        override val tokenIconUrl: String?,
        @DrawableRes override val tokenIconResId: Int,
        @DrawableRes override val networkBadgeIconResId: Int?,
        override val name: String,
    ) : ContentState(id, tokenIconUrl, tokenIconResId, networkBadgeIconResId, name)

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