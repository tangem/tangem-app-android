package com.tangem.features.addressbook.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import javax.inject.Inject

/**
 * Finds every supported mainnet network whose address format matches a given address.
 *
 * The match runs over the whole SDK blockchain set (minus testnets and excluded chains), not just the networks already
 * added to the wallet — entering/scanning an address must surface every network it could belong to.
 */
internal class SupportedNetworksMatcher @Inject constructor(
    excludedBlockchains: ExcludedBlockchains,
) {

    private val supportedBlockchains: List<Blockchain> = Blockchain.entries
        .filter { !it.isTestnet() && it !in excludedBlockchains }

    fun match(address: String): List<Blockchain> {
        if (address.isBlank()) return emptyList()
        return supportedBlockchains.filter { blockchain ->
            runCatching { blockchain.validateAddress(address) }.getOrDefault(false)
        }
    }
}