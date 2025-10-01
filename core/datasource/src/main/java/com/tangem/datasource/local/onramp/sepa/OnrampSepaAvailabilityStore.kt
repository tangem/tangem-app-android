package com.tangem.datasource.local.onramp.sepa

import kotlinx.coroutines.flow.Flow

interface OnrampSepaAvailabilityStore {
    suspend fun getSyncOrNull(key: OnrampSepaAvailabilityStoreKey): Boolean?
    fun get(key: OnrampSepaAvailabilityStoreKey): Flow<Boolean>
    suspend fun store(key: OnrampSepaAvailabilityStoreKey, value: Boolean)
    suspend fun clear()
}