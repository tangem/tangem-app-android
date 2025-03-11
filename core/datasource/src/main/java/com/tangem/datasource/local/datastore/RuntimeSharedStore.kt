package com.tangem.datasource.local.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull

/**
 * Runtime shared store
 *
[REDACTED_AUTHOR]
 */
interface RuntimeSharedStore<T> {

    /** Get flow of elements [T] */
    fun get(): Flow<T>

    /** Get element [T] synchronously or null */
    suspend fun getSyncOrNull(): T?

    /** Get element [T] synchronously or default value */
    suspend fun getSyncOrDefault(default: T): T

    /** Store [value] */
    suspend fun store(value: T)

    /**
     * Update
     *
     * @param default  default value if store is empty
     * @param function update function
     */
    suspend fun update(default: T, function: (T) -> T)

    companion object {

        /**
         * Create [RuntimeSharedStore]
         *
         * @param T type of stored value
         */
        operator fun <T> invoke(): RuntimeSharedStore<T> = object : RuntimeSharedStore<T> {

            private val flow = MutableSharedFlow<T?>(replay = 1)

            init {
                flow.tryEmit(value = null)
            }

            override fun get(): Flow<T> = flow.mapNotNull { it ?: return@mapNotNull null }

            override suspend fun getSyncOrNull(): T? = flow.firstOrNull()

            override suspend fun getSyncOrDefault(default: T): T = getSyncOrNull() ?: default

            override suspend fun store(value: T) {
                flow.emit(value = value)
            }

            override suspend fun update(default: T, function: (T) -> T) {
                val storedData = flow.firstOrNull() ?: default
                val updatedData = function(storedData)

                flow.emit(value = updatedData)
            }
        }
    }
}