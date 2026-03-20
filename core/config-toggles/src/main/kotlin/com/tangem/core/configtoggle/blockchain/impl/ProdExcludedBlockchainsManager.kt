package com.tangem.core.configtoggle.blockchain.impl

import com.tangem.core.configtoggle.blockchain.ExcludedBlockchainsManager
import com.tangem.core.configtoggle.blockchain.provider.ExcludedBlockchainTogglesProvider
import com.tangem.core.configtoggle.utils.defineTogglesAvailability
import com.tangem.core.configtoggle.version.VersionProvider

/**
 * [ExcludedBlockchainsManager] implementation in PROD build
 *
 * @property versionProvider                    application version provider
 * @property excludedBlockchainTogglesProvider  provider for excluded blockchain toggle entries
 */
internal class ProdExcludedBlockchainsManager(
    private val versionProvider: VersionProvider,
    private val excludedBlockchainTogglesProvider: ExcludedBlockchainTogglesProvider,
) : ExcludedBlockchainsManager {

    override val excludedBlockchainsIds: Set<String> = getBlockchainToggles()

    private fun getBlockchainToggles(): Set<String> {
        val appVersion = versionProvider.get()

        return excludedBlockchainTogglesProvider.getToggles()
            .defineTogglesAvailability(appVersion = appVersion)
            .filterValues { !it }
            .keys
    }
}