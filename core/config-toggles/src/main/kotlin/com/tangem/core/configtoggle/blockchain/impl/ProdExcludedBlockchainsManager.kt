package com.tangem.core.configtoggle.blockchain.impl

import com.tangem.core.configtoggle.ExcludedBlockchainToggles
import com.tangem.core.configtoggle.blockchain.ExcludedBlockchainsManager
import com.tangem.core.configtoggle.utils.defineTogglesAvailability
import com.tangem.core.configtoggle.version.VersionProvider

/**
 * [ExcludedBlockchainsManager] implementation in PROD build
 *
 * @property versionProvider application version provider
 */
internal class ProdExcludedBlockchainsManager(
    private val versionProvider: VersionProvider,
) : ExcludedBlockchainsManager {

    override val excludedBlockchainsIds: Set<String> = getBlockchainToggles()

    private fun getBlockchainToggles(): Set<String> {
        val appVersion = versionProvider.get()

        return ExcludedBlockchainToggles.values
            .defineTogglesAvailability(appVersion = appVersion)
            .filterValues { !it }
            .keys
    }
}