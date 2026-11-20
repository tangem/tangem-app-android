package com.tangem.spend.datasource.tangempay.store

import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import kotlinx.coroutines.flow.Flow

interface TangemPayCardFrozenStateStore {

    suspend fun getSyncOrNull(key: String): TangemPayCardFrozenState?

    fun get(key: String): Flow<TangemPayCardFrozenState>

    suspend fun store(key: String, value: TangemPayCardFrozenState)
}