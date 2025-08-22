package com.tangem.data.wallets.derivations

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.KeyWalletPublicKey
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.config.curvesConfig
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import kotlin.collections.forEach

private typealias DerivationData = Pair<ByteArrayKey, List<DerivationPath>>
internal typealias Derivations = Map<ByteArrayKey, List<DerivationPath>>

/**
 * Finder of missed derivations
 *
 * @property userWallet User wallet to find derivations for
 *
[REDACTED_AUTHOR]
 */
internal class MissedDerivationsFinder(private val userWallet: UserWallet) {

    /** Find missed derivations for given currencies [currencies] */
    fun find(currencies: List<CryptoCurrency>): Derivations {
        return currencies.map { it.network }.let(::findByNetworks)
    }

    fun findByNetworks(networks: List<Network>): Derivations {
        return buildMap<ByteArrayKey, MutableList<DerivationPath>> {
            networks
                .mapToNewDerivations()
                .forEach { data ->
                    val current = this[data.first]
                    if (current != null) {
                        current.addAll(data.second)
                        current.distinct()
                    } else {
                        this[data.first] = data.second.toMutableList()
                    }
                }
        }
    }

    private fun List<Network>.mapToNewDerivations(): List<DerivationData> {
        return mapNotNull { network ->
            val blockchain = network.toBlockchain()
            val curve = userWallet.curvesConfig.primaryCurve(blockchain) ?: return@mapNotNull null

            val walletPublicKey = when (userWallet) {
                is UserWallet.Cold -> {
                    val wallet = userWallet.scanResponse.card.wallets.firstOrNull { it.curve == curve }
                    wallet?.publicKey
                }
                is UserWallet.Hot -> {
                    val wallet = userWallet.wallets?.firstOrNull { it.curve == curve }
                    wallet?.publicKey
                }
            }

            walletPublicKey?.let {
                findNewDerivations(curve = curve, publicKey = it, network = network)
            }
        }
    }

    private fun findNewDerivations(curve: EllipticCurve, publicKey: ByteArray, network: Network): DerivationData? {
        val derivationCandidates = network
            .getDerivationCandidates(curve)
            .ifEmpty { return null }
            .filterAlreadyDerivedKeys(publicKey.toMapKey())
            .ifEmpty { return null }

        return publicKey.toMapKey() to derivationCandidates
    }

    private fun Network.getDerivationCandidates(curve: EllipticCurve): List<DerivationPath> {
        val blockchain = this.toBlockchain()

        return buildList {
            add(blockchain.getDerivationPath(curve = curve))
            add(blockchain.getCustomDerivationPath(curve = curve, network = this@getDerivationCandidates))
            add(blockchain.getCardanoDerivationPathIfNeeded(network = this@getDerivationCandidates))
        }
            .filterNotNull()
            .distinct()
    }

    private fun Blockchain.getDerivationPath(curve: EllipticCurve): DerivationPath? {
        return if (getSupportedCurves().contains(curve)) {
            derivationPath(style = userWallet.derivationStyleProvider.getDerivationStyle())
        } else {
            null
        }
    }

    private fun Blockchain.getCustomDerivationPath(curve: EllipticCurve, network: Network): DerivationPath? {
        return if (getSupportedCurves().contains(curve)) {
            network.derivationPath.value?.let(::DerivationPath)
        } else {
            null
        }
    }

    private fun Blockchain.getCardanoDerivationPathIfNeeded(network: Network): DerivationPath? {
        return if (this == Blockchain.Cardano) {
            network.derivationPath.value?.let {
                CardanoUtils.extendedDerivationPath(derivationPath = DerivationPath(it))
            }
        } else {
            null
        }
    }

    private fun List<DerivationPath>.filterAlreadyDerivedKeys(publicKey: KeyWalletPublicKey): List<DerivationPath> {
        val alreadyDerivedPaths = getAlreadyDerivedKeys(publicKey)
        return filterNot(alreadyDerivedPaths::contains)
    }

    private fun getAlreadyDerivedKeys(publicKey: KeyWalletPublicKey): List<DerivationPath> {
        val extendedPublicKeysMap = when (userWallet) {
            is UserWallet.Cold -> userWallet.scanResponse.derivedKeys[publicKey] ?: ExtendedPublicKeysMap(emptyMap())
            is UserWallet.Hot -> {
                val wallets = userWallet.wallets ?: return emptyList()
                wallets.firstOrNull { it.publicKey.contentEquals(publicKey.bytes) }?.derivedKeys
                    ?: ExtendedPublicKeysMap(emptyMap())
            }
        }

        return extendedPublicKeysMap.keys.toList()
    }
}