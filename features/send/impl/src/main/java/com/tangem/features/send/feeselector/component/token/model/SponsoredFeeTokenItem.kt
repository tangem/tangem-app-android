package com.tangem.features.send.feeselector.component.token.model

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.stringReference

// A product name, not translated in any locale
private const val SPONSORED_FEE_TITLE = "SmartGas"

/**
 * The sponsored fee is paid in the same coin as the regular one, so it is the coin's row under its own title and id:
 * rows are keyed by id.
 */
internal fun TokenItemState.asSponsoredFee(symbol: String): TokenItemState {
    val sponsoredId = "${id}_sponsored"
    val sponsoredTitle = TokenItemState.TitleState.Content(text = stringReference("$SPONSORED_FEE_TITLE $symbol"))
    return when (this) {
        is TokenItemState.Content -> copy(id = sponsoredId, titleState = sponsoredTitle)
        is TokenItemState.Unreachable -> copy(id = sponsoredId, titleState = sponsoredTitle)
        is TokenItemState.NoAddress -> copy(id = sponsoredId, titleState = sponsoredTitle)
        is TokenItemState.Loading -> copy(id = sponsoredId)
        is TokenItemState.Locked,
        is TokenItemState.Draggable,
        -> this
    }
}

internal fun TangemTokenRow.State.asSponsoredFee(symbol: String): TangemTokenRow.State {
    val sponsoredId = "${id}_sponsored"
    val sponsoredTitle = stringReference("$SPONSORED_FEE_TITLE $symbol")
    return when (this) {
        is TangemTokenRow.State.Content -> copy(id = sponsoredId, title = sponsoredTitle)
        is TangemTokenRow.State.Unreachable -> copy(id = sponsoredId, title = sponsoredTitle)
        is TangemTokenRow.State.NoAddress -> copy(id = sponsoredId, title = sponsoredTitle)
        is TangemTokenRow.State.Organize -> copy(id = sponsoredId, title = sponsoredTitle)
        is TangemTokenRow.State.Shimmer -> copy(id = sponsoredId)
    }
}