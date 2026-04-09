package com.tangem.domain.dynamicaddresses

import com.tangem.blockchain.common.Blockchain

/**
 * List of blockchains that support Dynamic Addresses (XPUB-based multi-address mode).
 * Must match [Blockchain.isBip44DerivationStyleXPUB] from blockchain-sdk minus Kaspa (deferred).
 *
 * Per ASMPT-005: DA is NOT used for Legacy (m/44' for BTC/LTC) or Taproot (m/86') addresses.
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

    private val supportedNetworkIds = supported.map { it.id }.toSet()

    /**
     * Allowed BIP purpose nodes per network ID.
     * BTC/LTC use BIP-84 (SegWit), others use BIP-44 (Legacy P2PKH).
     */
    private val allowedPurposeByNetworkId: Map<String, Long> = buildMap {
        put(Blockchain.Bitcoin.id, BIP84_PURPOSE)
        put(Blockchain.BitcoinTestnet.id, BIP84_PURPOSE)
        put(Blockchain.Litecoin.id, BIP84_PURPOSE)
        put(Blockchain.BitcoinCash.id, BIP44_PURPOSE)
        put(Blockchain.BitcoinCashTestnet.id, BIP44_PURPOSE)
        put(Blockchain.Dogecoin.id, BIP44_PURPOSE)
        put(Blockchain.Dash.id, BIP44_PURPOSE)
        put(Blockchain.Ravencoin.id, BIP44_PURPOSE)
        put(Blockchain.RavencoinTestnet.id, BIP44_PURPOSE)
    }

    fun isSupported(blockchain: Blockchain): Boolean = blockchain in supported

    fun isSupportedByNetworkId(networkId: String): Boolean = networkId in supportedNetworkIds

    /** Returns the allowed BIP purpose node for the given network, or null if not supported */
    fun getAllowedPurpose(networkId: String): Long? = allowedPurposeByNetworkId[networkId]
}