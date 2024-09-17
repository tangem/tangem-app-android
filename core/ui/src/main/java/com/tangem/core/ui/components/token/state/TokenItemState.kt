package com.tangem.core.ui.components.token.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeType

/** TokenItem component state */
@Immutable
sealed class TokenItemState {

    /** Unique id */
    abstract val id: String

    /** Token icon state */
    abstract val iconState: CurrencyIconState

    /** Token title state (in one row with [fiatAmountState]) */
    abstract val titleState: TitleState

    /** Token subtitle state (under [titleState] and in one row with [cryptoAmountState]) */
    abstract val subtitleState: SubtitleState?

    /** Token fiat amount state (in one row with [titleState]) */
    abstract val fiatAmountState: FiatAmountState?

    /** Token crypto amount state (under [fiatAmountState] and in one row with [subtitleState])  */
    abstract val cryptoAmountState: CryptoAmountState?

    /** Callback which will be called when an item is clicked */
    abstract val onItemClick: (() -> Unit)?

    /** Callback which will be called when an item is long clicked */
    abstract val onItemLongClick: (() -> Unit)?

    /**
     * Loading token state
     *
     * @property id            unique id
     * @property iconState     token icon state
     * @property titleState    token title
     * @property subtitleState token subtitle
     */
    data class Loading(
        override val id: String,
        override val iconState: CurrencyIconState,
        override val titleState: TitleState.Content,
        override val subtitleState: SubtitleState = SubtitleState.Loading,
    ) : TokenItemState() {
        override val fiatAmountState: FiatAmountState = FiatAmountState.Loading
        override val cryptoAmountState: CryptoAmountState = CryptoAmountState.Loading
        override val onItemClick: (() -> Unit)? = null
        override val onItemLongClick: (() -> Unit)? = null
    }

    /**
     * Locked token state
     *
     * @property id unique id
     */
    data class Locked(override val id: String) : TokenItemState() {
        override val iconState: CurrencyIconState = CurrencyIconState.Locked
        override val titleState: TitleState = TitleState.Locked
        override val subtitleState: SubtitleState = SubtitleState.Locked
        override val fiatAmountState: FiatAmountState = FiatAmountState.Locked
        override val cryptoAmountState: CryptoAmountState = CryptoAmountState.Locked
        override val onItemClick: (() -> Unit)? = null
        override val onItemLongClick: (() -> Unit)? = null
    }

    /**
     * Content token state
     *
     * @property id                unique id
     * @property iconState         token icon state
     * @property titleState        token title
     * @property subtitleState     token subtitle
     * @property fiatAmountState   token fiat amount
     * @property cryptoAmountState token crypto amount
     * @property onItemClick       callback which will be called when an item is clicked
     * @property onItemLongClick   callback which will be called when an item is long clicked
     */
    data class Content(
        override val id: String,
        override val iconState: CurrencyIconState,
        override val titleState: TitleState,
        override val subtitleState: SubtitleState,
        override val fiatAmountState: FiatAmountState,
        override val cryptoAmountState: CryptoAmountState.Content,
        override val onItemClick: (() -> Unit)?,
        override val onItemLongClick: (() -> Unit)?,
    ) : TokenItemState()

    /**
     * Draggable token state
     *
     * @property id                unique id
     * @property iconState         token icon state
     * @property titleState        token title
     * @property cryptoAmountState token crypto amount
     */
    data class Draggable(
        override val id: String,
        override val iconState: CurrencyIconState,
        override val titleState: TitleState,
        override val cryptoAmountState: CryptoAmountState,
    ) : TokenItemState() {
        override val subtitleState: SubtitleState? = null
        override val fiatAmountState: FiatAmountState? = null
        override val onItemClick: (() -> Unit)? = null
        override val onItemLongClick: (() -> Unit)? = null
    }

    /**
     * Unreachable token state
     *
     * @property id                unique id
     * @property iconState         token icon state
     * @property titleState        token title
     * @property subtitleState     token subtitle
     * @property onItemClick       callback which will be called when an item is clicked
     * @property onItemLongClick   callback which will be called when an item is long clicked
     */
    data class Unreachable(
        override val id: String,
        override val iconState: CurrencyIconState,
        override val titleState: TitleState,
        override val subtitleState: SubtitleState? = null,
        override val onItemClick: (() -> Unit)?,
        override val onItemLongClick: (() -> Unit)?,
    ) : TokenItemState() {
        override val fiatAmountState: FiatAmountState? = null
        override val cryptoAmountState: CryptoAmountState? = null
    }

    /**
     * No derivation address state
     *
     * @property id              unique id
     * @property iconState       token icon state
     * @property titleState      token title
     * @property subtitleState   token subtitle
     * @property onItemLongClick callback which will be called when an item is long clicked
     */
    data class NoAddress(
        override val id: String,
        override val iconState: CurrencyIconState,
        override val titleState: TitleState,
        override val subtitleState: SubtitleState? = null,
        override val onItemLongClick: (() -> Unit)?,
    ) : TokenItemState() {
        override val fiatAmountState: FiatAmountState? = null
        override val cryptoAmountState: CryptoAmountState? = null
        override val onItemClick: (() -> Unit)? = null
    }

    @Immutable
    sealed class TitleState {

        data class Content(val text: String, val hasPending: Boolean = false) : TitleState()

        data object Loading : TitleState()

        data object Locked : TitleState()
    }

    @Immutable
    sealed class SubtitleState {

        data class CryptoPriceContent(
            val price: String,
            val priceChangePercent: String,
            val type: PriceChangeType,
        ) : SubtitleState()

        data class TextContent(val value: String) : SubtitleState()

        data object Unknown : SubtitleState()

        data object Loading : SubtitleState()

        data object Locked : SubtitleState()
    }

    @Immutable
    sealed class FiatAmountState {
        data class Content(
            val text: String,
            val hasStaked: Boolean = false,
        ) : FiatAmountState()

        data object Loading : FiatAmountState()

        data object Locked : FiatAmountState()
    }

    @Immutable
    sealed class CryptoAmountState {
        data class Content(val text: String) : CryptoAmountState()

        data object Unreachable : CryptoAmountState()

        data object Loading : CryptoAmountState()

        data object Locked : CryptoAmountState()
    }
}