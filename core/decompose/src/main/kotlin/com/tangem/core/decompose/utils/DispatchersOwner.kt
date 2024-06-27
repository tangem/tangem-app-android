package com.tangem.core.decompose.utils

import com.tangem.utils.coroutines.CoroutineDispatcherProvider

/**
 * Interface for owning a [CoroutineDispatcherProvider].
 */
interface DispatchersOwner {

    /**
     * Provides access to the [CoroutineDispatcherProvider] instance.
     */
    val dispatchers: CoroutineDispatcherProvider
}