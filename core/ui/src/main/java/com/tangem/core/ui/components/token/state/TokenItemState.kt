package com.tangem.core.ui.components.token.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.audits.AuditLabelUM
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

    /** Token subtitle state (under [titleState] and in one row with [subtitle2State]) */
    abstract val subtitleState: SubtitleState?

    /** Token fiat amount state (in one row with [titleState]) */
    abstract val fiatAmountState: FiatAmountState?

    /**
     * Second subtitle state (under [fiatAmountState] and in one row with [subtitleState]).
     * Example, token crypto amount.
     */
    abstract val subtitle2State: Subtitle2State?

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
        override val iconState: CurrencyIconState = CurrencyIconState.Loading,
        override val titleState: TitleState = TitleState.Loading,
        override val subtitleState: SubtitleState = SubtitleState.Loading,
    ) : TokenItemState() {
        override val fiatAmountState: FiatAmountState = FiatAmountState.Loading
        override val subtitle2State: Subtitle2State = Subtitle2State.Loading
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
        override val subtitle2State: Subtitle2State = Subtitle2State.Locked
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
     * @property subtitle2State token crypto amount
     * @property onItemClick       callback which will be called when an item is clicked
     * @property onItemLongClick   callback which will be called when an item is long clicked
     */
    data class Content(
        override val id: String,
        override val iconState: CurrencyIconState,
        override val titleState: TitleState,
        override val subtitleState: SubtitleState,
        override val fiatAmountState: FiatAmountState,
        override val subtitle2State: Subtitle2State,
        override val onItemClick: (() -> Unit)?,
        override val onItemLongClick: (() -> Unit)?,
    ) : TokenItemState()

    /**
     * Draggable token state
     *
     * @property id                unique id
     * @property iconState         token icon state
     * @property titleState        token title
     * @property subtitle2State token crypto amount
     */
    data class Draggable(
        override val id: String,
        override val iconState: CurrencyIconState,
        override val titleState: TitleState,
        override val subtitle2State: Subtitle2State,
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
        override val subtitle2State: Subtitle2State? = null
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
        override val subtitle2State: Subtitle2State? = null
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
    sealed class Subtitle2State {

        data class TextContent(val text: String) : Subtitle2State()

        data class LabelContent(val auditLabelUM: AuditLabelUM) : Subtitle2State()

        data object Unreachable : Subtitle2State()

        data object Loading : Subtitle2State()

        data object Locked : Subtitle2State()
    }
}
