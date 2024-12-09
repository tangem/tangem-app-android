package com.tangem.features.onramp.swap.entity

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference

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
        val tokenItemState: TokenItemState,
    ) : ExchangeCardUM

    data class RemoveButtonUM(val onClick: () -> Unit)
}