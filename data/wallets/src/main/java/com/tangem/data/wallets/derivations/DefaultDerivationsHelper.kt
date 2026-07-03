package com.tangem.data.wallets.derivations

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.derivations.DerivationStyleProvider

class DefaultDerivationsHelper(private val demoConfig: DemoConfig) {

    fun getDefaultDerivations(
        derivationStyleProvider: DerivationStyleProvider,
        cardId: String,
        wallets: List<CardDTO.Wallet>,
    ): Map<ByteArrayKey, List<DerivationPath>> {
        val result = mutableMapOf<ByteArrayKey, List<DerivationPath>>()
        wallets.forEach { wallet ->
            val blockchainsForCurve = getBlockchains(cardId).filter {
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

    fun getDefaultDerivationsWithCurves(
        derivationStyleProvider: DerivationStyleProvider,
        cardId: String,
        curves: List<EllipticCurve>,
    ): Map<EllipticCurve, List<DerivationPath>> {
        val result = mutableMapOf<EllipticCurve, List<DerivationPath>>()
        curves.forEach { curve ->
            val blockchainsForCurve = getBlockchains(cardId).filter {
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

    private fun getBlockchains(cardId: String): List<Blockchain> {
        return when {
            demoConfig.isDemoCardId(cardId) -> demoConfig.getDemoBlockchains(cardId).toList()
            else -> listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        }
    }
}