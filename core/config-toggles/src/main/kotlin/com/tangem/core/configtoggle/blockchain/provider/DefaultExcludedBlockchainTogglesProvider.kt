package com.tangem.core.configtoggle.blockchain.provider

import com.tangem.core.configtoggle.ExcludedBlockchainToggles
import javax.inject.Inject

/**
 * Default implementation of [ExcludedBlockchainTogglesProvider] that reads from the generated
 * [ExcludedBlockchainToggles] enum.
 */
internal class DefaultExcludedBlockchainTogglesProvider @Inject constructor() : ExcludedBlockchainTogglesProvider {

    override fun getToggles(): Map<String, String> {
        return ExcludedBlockchainToggles.entries.associate { it.rawName to it.version }
    }
}