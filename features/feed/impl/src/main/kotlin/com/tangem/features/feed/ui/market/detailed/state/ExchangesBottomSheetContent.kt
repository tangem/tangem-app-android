package com.tangem.features.feed.ui.market.detailed.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.feed.impl.R
import com.tangem.utils.StringsSigns.DOT
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Exchanges bottom sheet content
 *
[REDACTED_AUTHOR]
 */
@Immutable
internal sealed interface ExchangesBottomSheetContent : TangemBottomSheetConfigContent {

    /** Title of bottom sheet. Like, app bar. */
    @get:StringRes
    val titleResId: Int
        get() = R.string.markets_token_details_exchanges_title

    /** Subtitle */
    @get:StringRes
    val subtitleResId: Int
        get() = R.string.markets_token_details_exchange

    /** Volume info */
    val volumeReference: TextReference
        get() = resourceReference(id = R.string.markets_token_details_volume) +
            stringReference(value = " $DOT ") +
            resourceReference(id = R.string.markets_selector_interval_24h_title)

    /** Exchange items */
    val exchangeItems: ImmutableList<TokenItemState>

    val exchangeItemsV2: ImmutableList<ExchangeItemUM>

    /**
     * Loading state
     *
     * @property exchangesCount count of exchanges
     */
    data class Loading(val exchangesCount: Int) : ExchangesBottomSheetContent {

        override val exchangeItems: ImmutableList<TokenItemState>
            get() = List(size = exchangesCount) { index -> TokenItemState.Loading(id = "loading#$index") }
                .toImmutableList()

        override val exchangeItemsV2: ImmutableList<ExchangeItemUM>
            get() = List(size = exchangesCount) { index -> ExchangeItemUM.Loading(id = "loading#$index") }
                .toImmutableList()
    }

    /**
     * Content state
     *
     * @property exchangeItems exchanges
     */
    data class ContentV1(
        override val exchangeItems: ImmutableList<TokenItemState>,
        override val exchangeItemsV2: ImmutableList<ExchangeItemUM> = persistentListOf(),
    ) : ExchangesBottomSheetContent

    data class ContentV2(
        override val exchangeItemsV2: ImmutableList<ExchangeItemUM>,
        override val exchangeItems: ImmutableList<TokenItemState> = persistentListOf(),
    ) : ExchangesBottomSheetContent

    /** Error state */
    data class Error(
        val onRetryClick: () -> Unit,
    ) : ExchangesBottomSheetContent {
        override val exchangeItems: ImmutableList<TokenItemState> = persistentListOf()
        override val exchangeItemsV2: ImmutableList<ExchangeItemUM> = persistentListOf()

        @StringRes
        val message: Int = R.string.markets_loading_error_title
    }
}