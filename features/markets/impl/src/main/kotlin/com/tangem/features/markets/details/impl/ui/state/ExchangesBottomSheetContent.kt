package com.tangem.features.markets.details.impl.ui.state

import androidx.annotation.StringRes
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.markets.impl.R
import com.tangem.utils.StringsSigns.DOT
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Exchanges bottom sheet content
 *
[REDACTED_AUTHOR]
 */
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
    @get:StringRes
    val volumeReference: TextReference
        get() = resourceReference(id = R.string.markets_token_details_volume) +
            stringReference(value = " $DOT ") +
            resourceReference(id = R.string.markets_selector_interval_24h_title)

    /** Exchange items */
    val exchangeItems: ImmutableList<TokenItemState>

    /**
     * Loading state
     *
     * @property exchangesCount count of exchanges
     */
    data class Loading(val exchangesCount: Int) : ExchangesBottomSheetContent {

        override val exchangeItems: ImmutableList<TokenItemState>
            get() = List(size = exchangesCount) { index -> TokenItemState.Loading(id = "loading#$index") }
                .toImmutableList()
    }

    /**
     * Content state
     *
     * @property exchangeItems exchanges
     */
    data class Content(
        override val exchangeItems: ImmutableList<TokenItemState>,
    ) : ExchangesBottomSheetContent

    /** Error state */
    data class Error(
        val onRetryClick: () -> Unit,
    ) : ExchangesBottomSheetContent {
        override val exchangeItems: ImmutableList<TokenItemState> = persistentListOf()

        @StringRes
        val message: Int = R.string.markets_loading_error_title
    }
}