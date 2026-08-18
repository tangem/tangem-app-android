package com.tangem.spend.datasource.pay.store

import com.tangem.domain.models.pay.TangemPayCardFrozenState
import kotlinx.coroutines.flow.Flow

interface TangemPayCardFrozenStateStore {

    suspend fun getSyncOrNull(key: String): TangemPayCardFrozenState?

    fun get(key: String): Flow<TangemPayCardFrozenState>

    suspend fun store(key: String, value: TangemPayCardFrozenState)
}