package com.tangem.domain.card.repository

interface CardRepository {

    suspend fun wasCardScanned(cardId: String): Boolean
}
