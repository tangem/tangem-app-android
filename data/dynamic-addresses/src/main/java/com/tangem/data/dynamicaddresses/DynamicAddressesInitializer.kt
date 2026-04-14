package com.tangem.data.dynamicaddresses

import com.tangem.domain.dynamicaddresses.DynamicAddressesFeatureToggles
import com.tangem.domain.dynamicaddresses.GetDerivedXpubUseCase
import com.tangem.domain.dynamicaddresses.model.DynamicAddressesStatus
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides XPUB strings for networks that need dynamic addresses restore (ENABLED_REQUIRES_SETUP).
 * Uses only already-derived keys — no card scan triggered.
 */
@Singleton
class DynamicAddressesInitializer @Inject constructor(
    private val dynamicAddressesRepository: DynamicAddressesRepository,
    private val dynamicAddressesFeatureToggles: DynamicAddressesFeatureToggles,
    private val getDerivedXpubUseCase: GetDerivedXpubUseCase,
) {

    suspend fun getXpubs(userWalletId: UserWalletId, networks: Set<Network>): Map<Network, String> {
        if (!dynamicAddressesFeatureToggles.isDynamicAddressesEnabled) return emptyMap()

        val result = mutableMapOf<Network, String>()
        for (network in networks) {
            val status = dynamicAddressesRepository.getStatus(userWalletId, network).firstOrNull()
            if (status != DynamicAddressesStatus.ENABLED_REQUIRES_SETUP) continue

            val xpub = getDerivedXpubUseCase(userWalletId, network)
            if (xpub != null) {
                result[network] = xpub
            } else {
                TangemLogger.w("Dynamic addresses enabled but XPUB not available for ${network.id}")
            }
        }
        return result
    }
}