package com.tangem.features.send.feeselector.component.token.model

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference

// A product name, not translated in any locale
private const val SPONSORED_FEE_TITLE = "SmartGas"

/**
 * The sponsored fee is paid in the same coin as the regular one, so it is the coin's row under its own title and id:
 * rows are keyed by id.
 */
internal fun TokenItemState.asSponsoredFee(symbol: String): TokenItemState {
    val sponsoredId = "${id}_sponsored"
    return when (this) {
        is TokenItemState.Content -> copy(
            id = sponsoredId,
            titleState = TokenItemState.TitleState.Content(text = stringReference("$SPONSORED_FEE_TITLE $symbol")),
        )
        is TokenItemState.Loading -> copy(id = sponsoredId)
        else -> this
    }
}