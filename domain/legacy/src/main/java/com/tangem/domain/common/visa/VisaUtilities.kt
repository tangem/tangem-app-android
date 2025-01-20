package com.tangem.domain.common.visa

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.models.scan.CardDTO

private const val VISA_BATCH_START = "AE"

object VisaUtilities {

    val mandatoryCurve = EllipticCurve.Secp256k1

    const val tokenId = "tether"

    val visaBlockchain = Blockchain.Polygon

    val visaDefaultDerivationPath = visaBlockchain.derivationPath(DerivationStyle.V3)

    fun isVisaCard(card: CardDTO): Boolean {
        return isVisaCard(card.firmwareVersion.doubleValue, card.batchId)
    }

    fun isVisaCard(firmwareVersion: Double, batchId: String): Boolean {
        return FirmwareVersion.visaRange.contains(firmwareVersion) &&
            batchId.startsWith(VISA_BATCH_START)
    }
}
