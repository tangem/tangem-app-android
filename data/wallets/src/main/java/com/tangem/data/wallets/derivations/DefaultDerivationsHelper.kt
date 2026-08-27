package com.tangem.data.wallets.derivations

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.derivations.DerivationStyleProvider
import com.tangem.domain.wallets.derivations.DerivationsHelper

internal class DefaultDerivationsHelper(private val demoConfig: DemoConfig) : DerivationsHelper {

    override fun getDefaultDerivations(
        derivationStyleProvider: DerivationStyleProvider,
        cardId: String,
        isTestCard: Boolean,
        wallets: List<CardDTO.Wallet>,
    ): Map<ByteArrayKey, List<DerivationPath>> {
        val result = mutableMapOf<ByteArrayKey, List<DerivationPath>>()
        wallets.forEach { wallet ->
            val blockchainsForCurve = getBlockchains(cardId, isTestCard).filter {
                it.getSupportedCurves().contains(wallet.curve)
            }
            val derivationPaths = blockchainsForCurve.mapNotNull { blockchain ->
                blockchain.derivationPath(derivationStyleProvider.getDerivationStyle())
            }
            val publicKey = wallet.publicKey ?: return@forEach
            if (derivationPaths.isNotEmpty()) {
                result[publicKey.toMapKey()] = derivationPaths
            }
        }
        return result
    }

    override fun getDefaultDerivationsWithCurves(
        derivationStyleProvider: DerivationStyleProvider,
        cardId: String,
        isTestCard: Boolean,
        curves: List<EllipticCurve>,
    ): Map<EllipticCurve, List<DerivationPath>> {
        val result = mutableMapOf<EllipticCurve, List<DerivationPath>>()
        curves.forEach { curve ->
            val blockchainsForCurve = getBlockchains(cardId, isTestCard).filter {
                it.getSupportedCurves().contains(curve)
            }
            val derivationPaths = blockchainsForCurve.mapNotNull { blockchain ->
                blockchain.derivationPath(derivationStyleProvider.getDerivationStyle())
            }
            if (derivationPaths.isNotEmpty()) {
                result[curve] = derivationPaths
            }
        }
        return result
    }

    private fun getBlockchains(cardId: String, isTestCard: Boolean): List<Blockchain> {
        return when {
            demoConfig.isDemoCardId(cardId) -> demoConfig.getDemoBlockchains(cardId).toList()
            isTestCard -> listOf(Blockchain.BitcoinTestnet, Blockchain.EthereumTestnet)
            else -> listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        }
    }
}