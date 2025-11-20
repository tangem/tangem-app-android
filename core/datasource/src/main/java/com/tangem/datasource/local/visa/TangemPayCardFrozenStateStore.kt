package com.tangem.datasource.local.visa

import com.tangem.domain.visa.model.TangemPayCardFrozenState
import kotlinx.coroutines.flow.Flow

interface TangemPayCardFrozenStateStore {

    suspend fun getSyncOrNull(key: String): TangemPayCardFrozenState?

    fun get(key: String): Flow<TangemPayCardFrozenState>

    suspend fun store(key: String, value: TangemPayCardFrozenState)
}