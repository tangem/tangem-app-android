package com.tangem.data.wallets.derivations

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.KeyWalletPublicKey
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet

private typealias DerivationData = Pair<ByteArrayKey, List<DerivationPath>>
internal typealias Derivations = Map<ByteArrayKey, List<DerivationPath>>

/**
 * Data class representing a blockchain with its derivation path
 */
data class BlockchainToDerive(
    val blockchain: Blockchain,
    val derivationPath: DerivationPath,
)

/**
 * Finder of missed derivations
 *
 * @property source Source of derivations data (UserWallet or ScanResponse)
 *
[REDACTED_AUTHOR]
 */
class MissedDerivationsFinder private constructor(
    private val source: DerivationsSource,
    private val isDynamicAddressesEnabled: Boolean,
) {

    constructor(userWallet: UserWallet, isDynamicAddressesEnabled: Boolean) : this(
        source = DerivationsSource.FromUserWallet(userWallet),
        isDynamicAddressesEnabled = isDynamicAddressesEnabled,
    )

    constructor(scanResponse: ScanResponse, isDynamicAddressesEnabled: Boolean) : this(
        source = DerivationsSource.FromScanResponse(scanResponse),
        isDynamicAddressesEnabled = isDynamicAddressesEnabled,
    )

    /** Find missed derivations for given currencies [currencies] */
    fun find(currencies: List<CryptoCurrency>): Derivations {
        return currencies.map { it.network }.let(::findByNetworks)
    }

    /** Find missed derivations for given [Network] list */
    fun findByNetworks(networks: List<Network>): Derivations {
        val blockchainsToDerive = networks.mapNotNull { network ->
            val blockchain = network.toBlockchain()
            val derivationPath = network.derivationPath.value?.let(::DerivationPath)
                ?: return@mapNotNull null

            BlockchainToDerive(blockchain, derivationPath)
        }
        return findByBlockchainsToDerive(blockchainsToDerive)
    }

    /** Find missed derivations for given [BlockchainToDerive] list */
    fun findByBlockchainsToDerive(blockchainsToDerive: Collection<BlockchainToDerive>): Derivations {
        val enrichedBlockchains = blockchainsToDerive.enrichBlockchains()
        return findDerivationsInternal(enrichedBlockchains)
    }

    /**
     * Common implementation for finding derivations
     */
    private fun findDerivationsInternal(items: Collection<BlockchainToDerive>): Derivations {
        return buildMap<ByteArrayKey, MutableList<DerivationPath>> {
            items
                .mapNotNull(::mapToNewDerivation)
                .forEach { data ->
                    val current = this[data.first]
                    if (current != null) {
                        current.addAll(data.second)
                        this[data.first] = current.distinct().toMutableList()
                    } else {
                        this[data.first] = data.second.toMutableList()
                    }
                }
        }
    }

    /**
     * Maps a single BlockchainToDerive to derivation data (public key -> derivation paths)
     */
    private fun mapToNewDerivation(input: BlockchainToDerive): DerivationData? {
        val curve = source.curvesConfig.primaryCurve(input.blockchain) ?: return null
        if (!input.blockchain.getSupportedCurves().contains(curve)) return null

        val publicKey = source.getWalletPublicKey(curve) ?: return null

        val derivationCandidates = input.blockchain
            .getDerivationCandidates(input.derivationPath)
            .ifEmpty { return null }
            .filterAlreadyDerivedKeys(publicKey.toMapKey())
            .ifEmpty { return null }

        return publicKey.toMapKey() to derivationCandidates
    }

    /**
     * Gets all possible derivation paths for a blockchain
     */
    private fun Blockchain.getDerivationCandidates(derivationPath: DerivationPath): List<DerivationPath> {
        return buildList {
            // Default derivation path for blockchain
            add(getDerivationPath())

            // The specified derivation path (can be either default or custom)
            add(derivationPath)

            // Extended Cardano derivation path if needed
            add(getCardanoExtendedDerivationPath(derivationPath))

            // Account-level and parent paths for XPUB generation (Dynamic Addresses)
            addAll(getXpubDerivationPaths(derivationPath))
        }
            .filterNotNull()
            .distinct()
    }

    private fun Blockchain.getDerivationPath(): DerivationPath? {
        return derivationPath(style = source.derivationStyleProvider.getDerivationStyle())
    }

    private fun Blockchain.getCardanoExtendedDerivationPath(customDerivationPath: DerivationPath): DerivationPath? {
        if (this != Blockchain.Cardano) return null
        return CardanoUtils.extendedDerivationPath(derivationPath = customDerivationPath)
    }

    /**
     * Returns account-level and parent derivation paths needed for XPUB generation.
     * - Account-level (e.g. m/84'/0'/0') — the XPUB itself
     * - Parent (e.g. m/84'/0') — needed for parent fingerprint in XPUB serialization
     *
     * Only applicable for BIP44-style XPUB blockchains (BTC, BCH, LTC, DOGE, DASH, RVN).
     */
    private fun Blockchain.getXpubDerivationPaths(derivationPath: DerivationPath): List<DerivationPath> {
        if (!isDynamicAddressesEnabled) return emptyList()
        if (!isBip44DerivationStyleXPUB()) return emptyList()

        val nodes = derivationPath.nodes
        if (nodes.size < XPUB_MIN_NODES) return emptyList()

        val accountPath = DerivationPath(nodes.take(XPUB_ACCOUNT_NODE_COUNT))
        val parentPath = DerivationPath(nodes.take(XPUB_PARENT_NODE_COUNT))

        return listOf(accountPath, parentPath)
    }

    private fun List<DerivationPath>.filterAlreadyDerivedKeys(publicKey: KeyWalletPublicKey): List<DerivationPath> {
        val alreadyDerivedPaths = source.getDerivedKeys(publicKey).keys.toList()
        return filterNot(alreadyDerivedPaths::contains)
    }

    // region Blockchain enrichment logic

    /**
     * Enriches blockchains collection:
     * - Adds Ethereum if HD wallet is allowed
     * - Removes unnecessary blockchains that share derivation path with Ethereum (for cards without old style derivation)
     */
    private fun Collection<BlockchainToDerive>.enrichBlockchains(): Collection<BlockchainToDerive> {
        if (!source.isHDWalletAllowed) return this

        val derivationStyle = source.derivationStyleProvider.getDerivationStyle()
        val ethereumDerivationPath = Blockchain.Ethereum.derivationPath(derivationStyle) ?: return this

        val withEthereum = this + BlockchainToDerive(Blockchain.Ethereum, ethereumDerivationPath)

        // For cards with old style derivation, keep all blockchains
        if (source.hasOldStyleDerivation) {
            return withEthereum.distinct()
        }

        // For new cards: filter out blockchains with same derivation path as Ethereum (except Ethereum itself)
        return withEthereum
            .filter { it.derivationPath != ethereumDerivationPath || it.blockchain == Blockchain.Ethereum }
            .distinct()
    }

    // endregion

    private companion object {
        const val XPUB_MIN_NODES = 3
        const val XPUB_ACCOUNT_NODE_COUNT = 3
        const val XPUB_PARENT_NODE_COUNT = 2
    }
}