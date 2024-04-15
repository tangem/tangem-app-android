package com.tangem.blockchainsdk.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Default implementation of RuntimeStore
 *
 * @param defaultValue default value
 */
internal class DefaultRuntimeStore<T>(defaultValue: T) : RuntimeStore<T> {

    private val flow = MutableStateFlow(value = defaultValue)

    override fun get(): Flow<T> = flow

    override suspend fun store(value: T) {
        flow.value = value
    }
}
