package com.tangem.test.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class DelayedReadDataStore<T>(
    private val delegate: DataStore<T>,
    private val readDelayMillis: Long,
) : DataStore<T> {

    override val data: Flow<T> = delegate.data.onEach { delay(readDelayMillis) }

    override suspend fun updateData(transform: suspend (t: T) -> T): T = delegate.updateData(transform)
}