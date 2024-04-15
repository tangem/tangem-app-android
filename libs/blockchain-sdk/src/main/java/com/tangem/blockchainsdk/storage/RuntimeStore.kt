package com.tangem.blockchainsdk.storage

import kotlinx.coroutines.flow.Flow

/**
 * Runtime store
 *
 * @author Andrew Khokhlov on 15/04/2024
 */
internal interface RuntimeStore<T> {

    /** Get flow of elements [T] */
    fun get(): Flow<T>

    /** Store [value] */
    suspend fun store(value: T)
}
