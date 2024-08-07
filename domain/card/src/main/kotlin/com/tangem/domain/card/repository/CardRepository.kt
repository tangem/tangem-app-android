package com.tangem.domain.card.repository

import kotlinx.coroutines.flow.Flow

interface CardRepository {

    fun wasCardScanned(cardId: String): Flow<Boolean>

    suspend fun setCardWasScanned(cardId: String)

    suspend fun startCardActivation(cardId: String)

    suspend fun finishCardActivation(cardId: String)

    suspend fun finishCardsActivation(cardIds: List<String>)

    @Throws
    suspend fun isActivationStarted(cardId: String): Boolean

    @Throws
    suspend fun isActivationFinished(cardId: String): Boolean

    @Throws
    suspend fun isActivationInProgress(cardId: String): Boolean

    @Throws
    suspend fun isTangemTOSAccepted(): Boolean

    suspend fun acceptTangemTOS()
}