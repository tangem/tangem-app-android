package com.tangem.domain.common

import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.domain.models.scan.CardDTO

interface DerivationStyleProvider {
    fun getDerivationStyle(): DerivationStyle?
}

internal class TangemDerivationStyleProvider(
    private val cardTypesResolver: CardTypesResolver,
    private val card: CardDTO,
) : DerivationStyleProvider {
    override fun getDerivationStyle(): DerivationStyle? {
        return when {
            !card.settings.isHDWalletAllowed -> null
            firstBatchesOfWallet1(card) -> DerivationStyle.V1
            cardTypesResolver.isWallet2() -> DerivationStyle.V3
            else -> DerivationStyle.V2
        }
    }

    private fun firstBatchesOfWallet1(card: CardDTO): Boolean {
        return card.batchId == "AC01" || card.batchId == "AC02" || card.batchId == "CB95"
    }
}
