package com.tangem.feature.wallet.presentation.common.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeType

/** Token item state */
@Immutable
internal sealed class TokenItemState {

    abstract val id: String

    abstract val iconState: CurrencyIconState

    abstract val titleState: TitleState

    abstract val fiatAmountState: FiatAmountState?

    abstract val cryptoAmountState: CryptoAmountState?

    abstract val cryptoPriceState: CryptoPriceState?

    /** Loading token state */
    data class Loading(
        override val id: String,
        override val iconState: CurrencyIconState,
        override val titleState: TitleState.Content,
    ) : TokenItemState() {
        override val fiatAmountState: FiatAmountState = FiatAmountState.Loading
        override val cryptoAmountState: CryptoAmountState = CryptoAmountState.Loading
        override val cryptoPriceState: CryptoPriceState = CryptoPriceState.Loading
    }

    /** Locked token state */
    data class Locked(override val id: String) : TokenItemState() {
        override val iconState: CurrencyIconState = CurrencyIconState.Locked
        override val titleState: TitleState = TitleState.Locked
        override val fiatAmountState: FiatAmountState = FiatAmountState.Locked
        override val cryptoAmountState: CryptoAmountState = CryptoAmountState.Locked
        override val cryptoPriceState: CryptoPriceState = CryptoPriceState.Locked
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
        override val iconState: CurrencyIconState,
        override val titleState: TitleState,
        override val fiatAmountState: FiatAmountState,
        override val cryptoAmountState: CryptoAmountState.Content,
        override val cryptoPriceState: CryptoPriceState,
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
        override val iconState: CurrencyIconState,
        override val titleState: TitleState,
        override val cryptoAmountState: CryptoAmountState,
    ) : TokenItemState() {
        override val fiatAmountState: FiatAmountState? = null
        override val cryptoPriceState: CryptoPriceState? = null
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
        override val iconState: CurrencyIconState,
        override val titleState: TitleState,
        val onItemClick: () -> Unit,
        val onItemLongClick: () -> Unit,
    ) : TokenItemState() {
        override val fiatAmountState: FiatAmountState? = null
        override val cryptoAmountState: CryptoAmountState? = null
        override val cryptoPriceState: CryptoPriceState? = null
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
        override val iconState: CurrencyIconState,
        override val titleState: TitleState,
        val onItemLongClick: () -> Unit,
    ) : TokenItemState() {
        override val fiatAmountState: FiatAmountState? = null
        override val cryptoAmountState: CryptoAmountState? = null
        override val cryptoPriceState: CryptoPriceState? = null
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

    sealed class CryptoPriceState {

        data class Content(
            val price: String,
            val priceChangePercent: String?,
            val type: PriceChangeType?,
        ) : CryptoPriceState()

        object Unknown : CryptoPriceState()

        object Loading : CryptoPriceState()

        object Locked : CryptoPriceState()
    }
}
