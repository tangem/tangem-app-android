package com.tangem.data.dynamicaddresses

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.dynamicaddresses.DynamicAddressesFeatureToggles
import com.tangem.domain.dynamicaddresses.DynamicAddressesSupportedBlockchains
import com.tangem.domain.dynamicaddresses.GetDerivedXpubUseCase
import com.tangem.domain.dynamicaddresses.model.DynamicAddressesStatus
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
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
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    suspend fun getXpubs(userWalletId: UserWalletId, networks: Set<Network>): Map<Network, String> {
        if (!dynamicAddressesFeatureToggles.isDynamicAddressesEnabled) return emptyMap()

        /*
         * Dynamic addresses rely on the server-side wallet accounts list, which is populated only for
         * multi-currency wallets. Single-currency wallets (Note, s2c, etc.) never populate it, so
         * DynamicAddressesRepository.getStatus() — backed by WalletAccountsFetcher.get() — would never
         * emit and firstOrNull() below would suspend forever, hanging the whole balance fetch and leaving
         * the currency stuck in Loading. Skip such wallets entirely. ([REDACTED_TASK_KEY])
         */
        val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId)
        if (userWallet == null || !userWallet.isMultiCurrency) return emptyMap()

        val result = mutableMapOf<Network, String>()
        for (network in networks) {
            if (!DynamicAddressesSupportedBlockchains.isSupportedByNetworkId(network.rawId)) continue

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