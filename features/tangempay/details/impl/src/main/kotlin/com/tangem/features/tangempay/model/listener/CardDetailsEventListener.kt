package com.tangem.features.tangempay.model.listener

import kotlinx.coroutines.flow.Flow

/**
 * Card details must be hidden after some UI actions from the external component
 */
internal interface CardDetailsEventListener {

    val event: Flow<CardDetailsEvent>

    suspend fun send(event: CardDetailsEvent)
}

internal enum class CardDetailsEvent {
    Hide, Show
}