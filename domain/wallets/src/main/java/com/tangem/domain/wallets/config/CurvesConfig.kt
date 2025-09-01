package com.tangem.domain.wallets.config

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.models.wallet.UserWallet

interface CurvesConfig {

    val mandatoryCurves: List<EllipticCurve>

    fun primaryCurve(blockchain: Blockchain): EllipticCurve?
}

val UserWallet.curvesConfig: CurvesConfig
    get() = when (this) {
        is UserWallet.Cold -> ColdCurvesConfig(this.scanResponse.card)
        is UserWallet.Hot -> HotCurvesConfig
    }