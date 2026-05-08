package com.tangem.domain.dynamicaddresses

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId

/**
 * List of blockchains that support Dynamic Addresses (XPUB-based multi-address mode).
 * Must match [Blockchain.isBip44DerivationStyleXPUB] from blockchain-sdk minus Kaspa (deferred).
 *
 * The allowed derivation per blockchain is the wallet's default for its derivation style:
 * Wallet 1 (V2) uses BIP-44 for BTC/LTC, Wallet 2 / Hot (V3) uses BIP-84. The check itself
 * lives in the caller (TokenDetailsModel) — this object only exposes the supported set.
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