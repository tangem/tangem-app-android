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

    /** Flag that indicates if remove button should be shown  */
    val hasRemoveButton: Boolean

    /** Token item state */
    val tokenItemState: TokenItemState

    /**
     * Empty state
     *
     * @property titleReference title reference
     */
    data class Empty(
        override val titleReference: TextReference,
    ) : ExchangeCardUM {

        override val hasRemoveButton: Boolean = false

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
     * @property tokenItemState token item state
     */
    data class Filled(
        override val titleReference: TextReference,
        override val tokenItemState: TokenItemState,
    ) : ExchangeCardUM {

        override val hasRemoveButton: Boolean = true
    }
}
