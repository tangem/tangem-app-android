package com.tangem.tap.domain.card

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.KeyWalletPublicKey
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.operations.derivation.ExtendedPublicKeysMap

private typealias DerivationData = Pair<ByteArrayKey, List<DerivationPath>>

/**
 * Finder of missed derivations
 *
 * @property scanResponse scanning response
 *
 * @author Andrew Khokhlov on 01/11/2023
 */
internal class MissedDerivationsFinder(private val scanResponse: ScanResponse) {

    /** Find missed derivations for given currencies [currencies] */
    fun find(currencies: List<CryptoCurrency>): Derivations {
        return buildMap<ByteArrayKey, MutableList<DerivationPath>> {
            currencies
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

    private fun List<CryptoCurrency>.mapToNewDerivations(): List<DerivationData> {
        val config = CardConfig.createConfig(scanResponse.card)
        return mapNotNull { currency ->
            val blockchain = Blockchain.fromId(id = currency.network.id.value)
            val curve = config.primaryCurve(blockchain) ?: return@mapNotNull null

            findNewDerivations(curve = curve, scanResponse = scanResponse, currency = currency)
        }
    }

    private fun findNewDerivations(
        curve: EllipticCurve,
        scanResponse: ScanResponse,
        currency: CryptoCurrency,
    ): DerivationData? {
        val wallet = scanResponse.card.wallets.firstOrNull { it.curve == curve } ?: return null
        val publicKey = wallet.publicKey.toMapKey()

        val derivationCandidates = currency
            .getDerivationCandidates(curve)
            .ifEmpty { return null }
            .filterAlreadyDerivedKeys(publicKey)
            .ifEmpty { return null }

        return publicKey to derivationCandidates
    }

    private fun CryptoCurrency.getDerivationCandidates(curve: EllipticCurve): List<DerivationPath> {
        val blockchain = Blockchain.fromId(id = network.id.value)

        return buildList {
            add(blockchain.getDerivationPath(curve = curve))
            add(blockchain.getCustomDerivationPath(curve = curve, currency = this@getDerivationCandidates))
            add(blockchain.getCardanoDerivationPathIfNeeded(currency = this@getDerivationCandidates))
        }
            .filterNotNull()
            .distinct()
    }

    private fun Blockchain.getDerivationPath(curve: EllipticCurve): DerivationPath? {
        return if (getSupportedCurves().contains(curve)) {
            derivationPath(style = scanResponse.derivationStyleProvider.getDerivationStyle())
        } else {
            null
        }
    }

    private fun Blockchain.getCustomDerivationPath(curve: EllipticCurve, currency: CryptoCurrency): DerivationPath? {
        return if (getSupportedCurves().contains(curve)) {
            currency.network.derivationPath.value?.let(::DerivationPath)
        } else {
            null
        }
    }

    private fun Blockchain.getCardanoDerivationPathIfNeeded(currency: CryptoCurrency): DerivationPath? {
        return if (currency is CryptoCurrency.Coin && this == Blockchain.Cardano) {
            currency.network.derivationPath.value?.let {
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
        val extendedPublicKeysMap = scanResponse.derivedKeys[publicKey] ?: ExtendedPublicKeysMap(emptyMap())
        return extendedPublicKeysMap.keys.toList()
    }
}
