package com.tangem.domain.collectibles.repository

import kotlinx.coroutines.flow.Flow

interface CollectiblesDismissedTxRepository {

    fun getDismissedTxIds(): Flow<Set<String>>

    suspend fun saveAsDismissed(txId: String)
}