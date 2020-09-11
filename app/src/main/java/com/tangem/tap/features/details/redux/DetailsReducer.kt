package com.tangem.tap.features.details.redux

import com.tangem.commands.Card
import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

class DetailsReducer {
    companion object {
        fun reduce(action: Action, state: AppState): DetailsState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): DetailsState {

    if (action !is DetailsAction) return state.detailsState

    var detailsState = state.detailsState
    when (action) {
        is DetailsAction.SetCard -> {
            detailsState = DetailsState(card = action.card, cardInfo = action.card.toCardInfo())
        }
    }
    return detailsState
}

private fun Card.toCardInfo(): CardInfo? {
    val cardId = this.cardId.chunked(4).joinToString(separator = " ")
    val issuer = this.cardData?.issuerName ?: return null
    val signedHashes = this.walletSignedHashes ?: return null
    return CardInfo(cardId, issuer, signedHashes)
}