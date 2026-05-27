package com.tangem.features.managetokens.utils

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.dynamicaddresses.DynamicAddressesDerivationChecker
import com.tangem.domain.dynamicaddresses.DynamicAddressesFeatureToggles
import com.tangem.domain.dynamicaddresses.DynamicAddressesSupportedBlockchains
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.firstOrNull

/**
 * Validates that custom derivation paths don't conflict with Dynamic Addresses.
 *
 * When DA is enabled for an account, custom derivation paths with non-zero
 * change (node 3) or address_index (node 4) are forbidden, because DA
 * manages those nodes automatically.
 */
internal class DynamicAddressesDerivationValidator(
    private val dynamicAddressesRepository: DynamicAddressesRepository,
    private val dynamicAddressesFeatureToggles: DynamicAddressesFeatureToggles,
) {

    /**
     * @return true if the path is invalid (DA is enabled for the same account and change/index ≠ 0)
     */
    suspend fun isInvalidForDynamicAddresses(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        path: String?,
    ): Boolean {
        if (!dynamicAddressesFeatureToggles.isDynamicAddressesEnabled) return false
        if (path == null) return false
        if (!isSupportedBlockchain(networkId)) return false

        val basePath = networkId.derivationPath.value ?: return false
        val isEnabled = dynamicAddressesRepository
            .isDynamicAddressesEnabledForNetwork(userWalletId, networkId)
            .firstOrNull() == true
        if (!isEnabled) return false

        return DynamicAddressesDerivationChecker.hasSameAccountWithNonZeroChangeOrIndex(
            customPath = path,
            basePath = basePath,
        )
    }

    private fun isSupportedBlockchain(networkId: Network.ID): Boolean {
        return DynamicAddressesSupportedBlockchains.isSupported(networkId.toBlockchain())
    }
}