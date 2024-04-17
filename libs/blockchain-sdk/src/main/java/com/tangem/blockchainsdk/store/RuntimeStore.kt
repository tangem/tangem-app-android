package com.tangem.blockchainsdk.store

import kotlinx.coroutines.flow.StateFlow

/**
 * Runtime store
 *
* [REDACTED_AUTHOR]
 */
internal interface RuntimeStore<T> {

    /** Get flow of elements [T] */
    fun get(): StateFlow<T>

    /** Store [value] */
    suspend fun store(value: T)
}
