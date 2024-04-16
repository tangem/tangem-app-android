package com.tangem.blockchainsdk.store

import kotlinx.coroutines.flow.StateFlow

/**
 * Runtime store
 *
 * @author Andrew Khokhlov on 15/04/2024
 */
internal interface RuntimeStore<T> {

    /** Get flow of elements [T] */
    fun get(): StateFlow<T>

    /** Store [value] */
    suspend fun store(value: T)
}
