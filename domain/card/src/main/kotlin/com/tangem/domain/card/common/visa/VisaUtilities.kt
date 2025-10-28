package com.tangem.domain.card.common.visa

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.card.FirmwareVersion
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.scan.CardDTO

private const val VISA_BATCH_START = "AE"
private const val VISA_BATCH_START_2 = "FFFC"

object VisaUtilities {

    const val tokenId = "tether"

    val visaBlockchain = Blockchain.Polygon

    val visaDefaultDerivationPath
        get() = visaBlockchain.derivationPath(DerivationStyle.V3)
    val customDerivationPath = DerivationPath("m/44'/60'/999999'/0/0")

    fun visaDefaultDerivationPath(style: DerivationStyle) = visaBlockchain.derivationPath(style)

    fun isVisaCard(card: CardDTO): Boolean {
        return isVisaCard(card.firmwareVersion.doubleValue, card.batchId)
    }

    fun isVisaCard(firmwareVersion: Double, batchId: String): Boolean {
        return firmwareVersion in FirmwareVersion.visaRange &&
            (batchId.startsWith(VISA_BATCH_START) || batchId.startsWith(VISA_BATCH_START_2))
    }
}