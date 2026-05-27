package com.tangem.data.settings

import com.tangem.domain.settings.HotWalletRestrictionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Production implementation of [HotWalletRestrictionManager].
 *
 * Hot wallet creation restriction is always enabled in production builds —
 * users must scan a physical Tangem card to add a wallet.
 * [toggleCreationEnabled] is a no-op since the restriction cannot be changed at runtime.
 */
internal class ProdHotWalletRestrictionManager : HotWalletRestrictionManager {

    private val state: StateFlow<Boolean> = MutableStateFlow(true)

    override fun isCreationEnabled(): StateFlow<Boolean> = state
    override fun isCreationEnabledSync(): Boolean = true
    override suspend fun toggleCreationEnabled() = Unit
}