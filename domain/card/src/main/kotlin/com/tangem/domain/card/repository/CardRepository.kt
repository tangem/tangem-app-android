package com.tangem.domain.card.repository

import kotlinx.coroutines.flow.Flow

interface CardRepository {

    fun wasCardScanned(cardId: String): Flow<Boolean>

    suspend fun setCardWasScanned(cardId: String)
}