package com.tangem.datasource.local.swap

/**
 * Stores flag indicating whether should show best rate animation in current session.
 * Animation should appear once per session
 *
 * If true, reset flag to false
 */
interface SwapBestRateAnimationStore {
    suspend fun getSyncOrNull(): Boolean
}