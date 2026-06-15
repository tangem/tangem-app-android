package com.tangem.features.tangempay.model.listener

import kotlinx.coroutines.flow.Flow

/**
 * Coordinates reveal/hide of card details across card-detail blocks that may be shown simultaneously
 * (e.g. several cards in a pager, or the same card on the card page and the edit-name screen).
 *
 * Events are card-scoped so that one block does not flip the others; [CardDetailsEvent.HideAll] is
 * the only broadcast event.
 */
internal interface CardDetailsEventListener {

    val event: Flow<CardDetailsEvent>

    fun send(event: CardDetailsEvent)
}

internal sealed interface CardDetailsEvent {

    /** Reveal the details of [cardId]. Other blocks treat it as a hint to hide themselves. */
    data class Show(val cardId: String) : CardDetailsEvent

    /** Hide the details of [cardId]. */
    data class Hide(val cardId: String) : CardDetailsEvent

    /** Hide details of every block (e.g. leaving the card screen). */
    data object HideAll : CardDetailsEvent
}