package com.tangem.features.onramp.swap.entity

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.onramp.impl.R

/**
 * Exchange card UI model
 *
 * @author Andrew Khokhlov on 30/10/2024
 */
internal sealed interface ExchangeCardUM {

    /** Title reference */
    val titleReference: TextReference

    /** Remove button UI model */
    val removeButtonUM: RemoveButtonUM?

    /** Token item state */
    val tokenItemState: TokenItemState

    /**
     * Empty state
     *
     * @property titleReference title reference
     */
    data class Empty(override val titleReference: TextReference) : ExchangeCardUM {

        override val removeButtonUM: RemoveButtonUM? = null

        // TODO: https://tangem.atlassian.net/browse/AND-8936
        override val tokenItemState: TokenItemState = TokenItemState.Content(
            id = "empty",
            iconState = CurrencyIconState.Empty(R.drawable.ic_empty_64),
            titleState = TokenItemState.TitleState.Content(text = "Choose the Token"),
            subtitleState = TokenItemState.SubtitleState.TextContent(value = "You want to Swap"),
            fiatAmountState = TokenItemState.FiatAmountState.Content(text = ""),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(text = ""),
            onItemClick = null,
            onItemLongClick = null,
        )
    }

    /**
     * Filled
     *
     * @property titleReference title reference
     * @property removeButtonUM remove button UI model
     * @property tokenItemState token item state
     */
    data class Filled(
        override val titleReference: TextReference,
        override val removeButtonUM: RemoveButtonUM?,
        override val tokenItemState: TokenItemState,
    ) : ExchangeCardUM

    data class RemoveButtonUM(val onClick: () -> Unit)
}
