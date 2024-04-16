package com.tangem.blockchainsdk.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Default implementation of RuntimeStore
 *
 * @param defaultValue default value
 */
internal class DefaultRuntimeStore<T>(defaultValue: T) : RuntimeStore<T> {

    private val flow = MutableStateFlow(value = defaultValue)

    override fun get(): StateFlow<T> = flow

    override suspend fun store(value: T) {
        flow.value = value
    }
}