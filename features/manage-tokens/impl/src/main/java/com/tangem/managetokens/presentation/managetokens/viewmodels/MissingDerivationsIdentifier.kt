package com.tangem.managetokens.presentation.managetokens.viewmodels

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.operations.derivation.ExtendedPublicKeysMap

object MissingDerivationsIdentifier {

    fun getMissingDerivationsForWallets(
        wallets: List<UserWallet>,
        addedCurrenciesByWallet: MutableMap<UserWallet, MutableList<CryptoCurrency>>,
    ): Map<UserWallet, List<DerivationData>> {
        return wallets.associateWith { wallet ->
            val currencies = addedCurrenciesByWallet[wallet] ?: emptyList()
            val config = CardConfig.createConfig(wallet.scanResponse.card)
            val missingDerivationsByCurrency = currencies.mapNotNull { currency ->
                config.primaryCurve(blockchain = Blockchain.fromId(currency.network.id.value))?.let { curve ->
                    getMissingDerivations(curve, wallet.scanResponse, currency)
                }
            }
            collectMissingDerivationsByPublicKey(missingDerivationsByCurrency)
        }.filter { it.value.isNotEmpty() }
    }

    private fun getMissingDerivations(
        curve: EllipticCurve,
        scanResponse: ScanResponse,
        currency: CryptoCurrency,
    ): DerivationData? {
        val wallet = scanResponse.card.wallets.firstOrNull { it.curve == curve } ?: return null

        val blockchain = Blockchain.fromId(currency.network.id.value)
        val supportedCurves = blockchain.getSupportedCurves()
        val path = blockchain.derivationPath(scanResponse.derivationStyleProvider.getDerivationStyle())
            .takeIf { supportedCurves.contains(curve) }

        val customPath = currency.network.derivationPath.value?.let {
            DerivationPath(it)
        }.takeIf { supportedCurves.contains(curve) }

        val bothCandidates = listOfNotNull(path, customPath).distinct().toMutableList()
        if (bothCandidates.isEmpty()) return null

        if (currency is CryptoCurrency.Coin && blockchain == Blockchain.Cardano) {
            currency.network.derivationPath.value?.let {
                bothCandidates.add(CardanoUtils.extendedDerivationPath(DerivationPath(it)))
            }
        }

        val mapKeyOfWalletPublicKey = wallet.publicKey.toMapKey()
        val alreadyDerivedKeys: ExtendedPublicKeysMap =
            scanResponse.derivedKeys[mapKeyOfWalletPublicKey] ?: ExtendedPublicKeysMap(emptyMap())
        val alreadyDerivedPaths = alreadyDerivedKeys.keys.toList()

        val toDerive = bothCandidates.filterNot { alreadyDerivedPaths.contains(it) }
        if (toDerive.isEmpty()) return null

        return DerivationData(derivations = mapKeyOfWalletPublicKey to toDerive)
    }

    private fun collectMissingDerivationsByPublicKey(
        missingDerivationsByCurrency: List<DerivationData>,
    ): List<DerivationData> {
        return buildMap<ByteArrayKey, MutableList<DerivationPath>> {
            missingDerivationsByCurrency.forEach { derivationData ->
                val currentEntry = this[derivationData.derivations.first]
                if (currentEntry != null) {
                    currentEntry.addAll(derivationData.derivations.second)
                    currentEntry.distinct()
                } else {
                    this[derivationData.derivations.first] = derivationData.derivations.second.toMutableList()
                }
            }
        }.map { DerivationData(it.key to it.value) }
    }
}

class DerivationData(val derivations: Pair<ByteArrayKey, List<DerivationPath>>) {
    fun derivationsNeeded(): Int {
        return derivations.second.size
    }
}

internal fun List<DerivationData>.toMapOfDerivations(): Map<ByteArrayKey, List<DerivationPath>> {
    return associate { it.derivations }
}