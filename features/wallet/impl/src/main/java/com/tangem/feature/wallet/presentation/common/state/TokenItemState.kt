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

    /**
     * Content token state
     *
     * @property id                  unique id
     * @property tokenIconUrl        token icon url
     * @property tokenIconResId      token icon resource id
     * @property networkIconResId    network icon resource id, may be null if it is a coin
     * @property name                token name
     * @property amount              amount of token
     * @property hasPending          pending tx in blockchain
     * @property tokenOptions        state for token options
     */
    data class Content(
        override val id: String,
        val tokenIconUrl: String?,
        @DrawableRes val tokenIconResId: Int,
        @DrawableRes val networkIconResId: Int?,
        val name: String,
        val amount: String,
        val hasPending: Boolean,
        val tokenOptions: TokenOptionsState,
        val onClick: () -> Unit,
    ) : TokenItemState

    /**
     * Draggable token state
     *
     * @property id                  unique id
     * @property tokenIconUrl        token icon url
     * @property tokenIconResId      token icon resource id
     * @property networkIconResId    network icon resource id, may be null if it is a coin
     * @property name                token name
     * @property fiatAmount          fiat amount of token
     */
    data class Draggable(
        override val id: String,
        val tokenIconUrl: String?,
        @DrawableRes val tokenIconResId: Int,
        @DrawableRes val networkIconResId: Int?,
        val name: String,
        val fiatAmount: String,
    ) : TokenItemState

    /**
     * Unreachable token state
     *
     * @property id                  token id
     * @property tokenIconUrl        token icon url
     * @property tokenIconResId      token icon resource id
     * @property networkIconResId    network icon resource id, may be null if it is a coin
     * @property name                token name
     */
    data class Unreachable(
        override val id: String,
        val tokenIconUrl: String?,
        @DrawableRes val tokenIconResId: Int,
        @DrawableRes val networkIconResId: Int?,
        val name: String,
    ) : TokenItemState

    /** Token options state */
    sealed interface TokenOptionsState {

        /**
         * Visible token options state
         *
         * @property fiatAmount fiat amount of token
         * @property priceChange value of price changing
         */
        data class Visible(val fiatAmount: String, val priceChange: PriceChangeConfig) : TokenOptionsState

        /**
         * Hidden token options state
         *
         * @property priceChange value of price changing
         */
        data class Hidden(val priceChange: PriceChangeConfig) : TokenOptionsState
    }
}
