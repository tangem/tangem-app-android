package com.tangem.domain.dynamicaddresses

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId

/**
 * Whitelist of blockchains eligible for Dynamic Addresses (XPUB-based multi-address mode).
 * Mirrors [Blockchain.isBip44DerivationStyleXPUB] from blockchain-sdk minus Kaspa (deferred).
 */
object DynamicAddressesSupportedBlockchains {

    private val supported = setOf(
        Blockchain.Bitcoin,
        Blockchain.BitcoinTestnet,
        Blockchain.BitcoinCash,
        Blockchain.BitcoinCashTestnet,
        Blockchain.Litecoin,
        Blockchain.Dogecoin,
        Blockchain.Dash,
        Blockchain.Ravencoin,
        Blockchain.RavencoinTestnet,
    )

    private val supportedNetworkIds = supported.map { it.toNetworkId() }.toSet()

    fun isSupported(blockchain: Blockchain): Boolean = blockchain in supported

    fun isSupportedByNetworkId(networkId: String): Boolean = networkId in supportedNetworkIds
}