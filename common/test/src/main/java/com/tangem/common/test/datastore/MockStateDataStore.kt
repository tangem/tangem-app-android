package com.tangem.common.test.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

class MockStateDataStore<T>(default: T) : DataStore<T> {

    private val _data = MutableStateFlow(value = default)
    override val data: Flow<T> = _data

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        return _data.updateAndGet { transform(it) }
    }
}