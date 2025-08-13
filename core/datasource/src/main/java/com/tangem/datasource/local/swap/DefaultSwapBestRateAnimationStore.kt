package com.tangem.datasource.local.swap

import com.tangem.datasource.local.datastore.RuntimeSharedStore

internal class DefaultSwapBestRateAnimationStore(
    private val dataStore: RuntimeSharedStore<Boolean>,
) : SwapBestRateAnimationStore, RuntimeSharedStore<Boolean> by dataStore {
    /**
     * Returns flag indicating whether should show best rate animation in current session.
     * Animation should appear once per session
     *
     * If true, reset flag to false
     */
    override suspend fun getSyncOrNull(): Boolean {
        val value = dataStore.getSyncOrNull() ?: true
        if (value) {
            dataStore.store(false)
        }
        return value
    }
}