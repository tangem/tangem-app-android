package com.tangem.domain.wallets.config

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.card.configs.CardConfig
import com.tangem.domain.models.scan.CardDTO

class ColdCurvesConfig(cardDTO: CardDTO) : CurvesConfig {

    val cardConfig = CardConfig.createConfig(cardDTO)

    override val mandatoryCurves: List<EllipticCurve>
        get() = cardConfig.mandatoryCurves

    override fun primaryCurve(blockchain: Blockchain): EllipticCurve? {
        return cardConfig.primaryCurve(blockchain)
    }
}