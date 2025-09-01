package com.tangem.domain.wallets.config

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.card.configs.Wallet2CardConfig

data object HotCurvesConfig : CurvesConfig {

    override val mandatoryCurves: List<EllipticCurve>
        get() = Wallet2CardConfig.mandatoryCurves

    override fun primaryCurve(blockchain: Blockchain): EllipticCurve? {
        return Wallet2CardConfig.primaryCurve(blockchain)
    }
}