package com.tangem.features.swap.v2.impl.amount.store

/**
 * Stores flag indicating whether should show best rate animation in current session.
 * Animation should appear once per session
 *
 * If true, reset flag to false
 */
internal interface SwapBestRateAnimationStore {
    suspend fun getSyncOrNull(): Boolean
}