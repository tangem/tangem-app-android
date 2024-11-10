package com.tangem.features.onramp.swap.entity

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.impl.R

/**
 * Exchange card UI model
 *
[REDACTED_AUTHOR]
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
     * @property titleReference    title reference
     * @property subtitleReference empty token subtitle reference
     */
    data class Empty(
        override val titleReference: TextReference,
        val subtitleReference: TextReference,
    ) : ExchangeCardUM {

        override val removeButtonUM: RemoveButtonUM? = null

        override val tokenItemState: TokenItemState = TokenItemState.Content(
            id = "empty",
            iconState = CurrencyIconState.Empty(R.drawable.ic_empty_64),
            titleState = TokenItemState.TitleState.Content(
                text = resourceReference(id = R.string.action_buttons_swap_choose_token),
            ),
            subtitleState = TokenItemState.SubtitleState.TextContent(value = subtitleReference),
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