package com.tangem.datasource.local.datastore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Runtime store
 *
[REDACTED_AUTHOR]
 */
interface RuntimeStateStore<T> {

    /** Get flow of elements [T] */
    fun get(): StateFlow<T>

    /** Store [value] */
    suspend fun store(value: T)

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

            override suspend fun store(value: T) {
                flow.value = value
            }
        }
    }
}