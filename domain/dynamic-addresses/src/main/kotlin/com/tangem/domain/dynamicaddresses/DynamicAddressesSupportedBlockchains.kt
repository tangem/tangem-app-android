package com.tangem.domain.dynamicaddresses

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId

/**
 * List of blockchains that support Dynamic Addresses (XPUB-based multi-address mode).
 * Must match [Blockchain.isBip44DerivationStyleXPUB] from blockchain-sdk minus Kaspa (deferred).
 *
 * Dynamic addresses are NOT used for Legacy (m/44' for BTC/LTC) or Taproot (m/86') addresses.
 * Only the default derivation style per blockchain is supported.
 */
object DynamicAddressesSupportedBlockchains {

    private const val BIP44_PURPOSE = 44L
    private const val BIP84_PURPOSE = 84L

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

    /**
     * Allowed BIP purpose nodes per network ID.
     * BTC/LTC use BIP-84 (SegWit), others use BIP-44 (Legacy P2PKH).
     */
    private val allowedPurposeByNetworkId: Map<String, Long> = buildMap {
        put(Blockchain.Bitcoin.toNetworkId(), BIP84_PURPOSE)
        put(Blockchain.BitcoinTestnet.toNetworkId(), BIP84_PURPOSE)
        put(Blockchain.Litecoin.toNetworkId(), BIP84_PURPOSE)
        put(Blockchain.BitcoinCash.toNetworkId(), BIP44_PURPOSE)
        put(Blockchain.BitcoinCashTestnet.toNetworkId(), BIP44_PURPOSE)
        put(Blockchain.Dogecoin.toNetworkId(), BIP44_PURPOSE)
        put(Blockchain.Dash.toNetworkId(), BIP44_PURPOSE)
        put(Blockchain.Ravencoin.toNetworkId(), BIP44_PURPOSE)
        put(Blockchain.RavencoinTestnet.toNetworkId(), BIP44_PURPOSE)
    }

    fun isSupported(blockchain: Blockchain): Boolean = blockchain in supported

    fun isSupportedByNetworkId(networkId: String): Boolean = networkId in supportedNetworkIds

    /** Returns the allowed BIP purpose node for the given network, or null if not supported */
    fun getAllowedPurpose(networkId: String): Long? = allowedPurposeByNetworkId[networkId]
}