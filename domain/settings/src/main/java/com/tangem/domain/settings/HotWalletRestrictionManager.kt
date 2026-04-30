package com.tangem.domain.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the hot wallet creation restriction setting.
 *
 * When the restriction is enabled, users are forced to scan a physical Tangem card
 * instead of being able to create a new software (hot) wallet.
 */
interface HotWalletRestrictionManager {

    /** Observes the current restriction state as a [StateFlow]. */
    fun isCreationEnabled(): StateFlow<Boolean>

    /** Returns the latest cached restriction state synchronously. */
    fun isCreationEnabledSync(): Boolean

    /** Toggles the restriction state. No-op in production. */
    suspend fun toggleCreationEnabled()
}