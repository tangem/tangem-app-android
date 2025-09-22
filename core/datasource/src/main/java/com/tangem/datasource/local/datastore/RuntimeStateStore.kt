package com.tangem.datasource.local.datastore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Runtime store
 *
[REDACTED_AUTHOR]
 */
interface RuntimeStateStore<T> {

    /** Get flow of elements [T] */
    fun get(): StateFlow<T>

    /** Get element [T] synchronously or null */
    suspend fun getSyncOrNull(): T?

    /** Store [value] */
    suspend fun store(value: T)

    /** Update current value by [function] */
    suspend fun update(function: (T) -> T)

    /** Clear stored value */
    fun clear()

    companion object {

        /**
         * Create [RuntimeStateStore]
         *
         * @param T            type of stored value
         * @param defaultValue default value
         */
        operator fun <T> invoke(defaultValue: T): RuntimeStateStore<T> = object : RuntimeStateStore<T> {
            private val flow = MutableStateFlow(value = defaultValue)

            override fun get(): StateFlow<T> = flow

            override suspend fun getSyncOrNull(): T? = flow.value

            override suspend fun store(value: T) {
                flow.value = value
            }

            override suspend fun update(function: (T) -> T) {
                flow.update(function)
            }

            override fun clear() {
                flow.value = defaultValue
            }
        }
    }
}