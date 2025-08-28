package com.tangem.domain.wallets.derivations

import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.domain.card.common.TapWorkarounds.isWallet2
import com.tangem.domain.models.scan.CardDTO

interface DerivationStyleProvider {
    fun getDerivationStyle(): DerivationStyle?
}

internal class TangemDerivationStyleProvider(
    private val card: CardDTO,
) : DerivationStyleProvider {
    override fun getDerivationStyle(): DerivationStyle? {
        return when {
            !card.settings.isHDWalletAllowed -> null
            firstBatchesOfWallet1(card) -> DerivationStyle.V1
            card.isWallet2 -> DerivationStyle.V3
            else -> DerivationStyle.V2
        }
    }

    private fun firstBatchesOfWallet1(card: CardDTO): Boolean {
        return card.batchId == "AC01" || card.batchId == "AC02" || card.batchId == "CB95"
    }
}

internal class TangemHotDerivationStyleProvider : DerivationStyleProvider {
    override fun getDerivationStyle(): DerivationStyle? = DerivationStyle.V3
}