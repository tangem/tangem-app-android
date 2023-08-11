package com.tangem.domain.common.configs

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.models.scan.CardDTO

sealed interface CardConfig {

    val mandatoryCurves: List<EllipticCurve>

    fun primaryCurve(blockchain: Blockchain): EllipticCurve?

    companion object {

        fun createConfig(cardDTO: CardDTO): CardConfig {
            if (cardDTO.firmwareVersion >= FirmwareVersion.Ed25519Slip0010Available) {
                return Wallet2CardConfig
            }
            if (cardDTO.settings.isBackupAllowed && cardDTO.settings.isHDWalletAllowed &&
                cardDTO.firmwareVersion >= FirmwareVersion.MultiWalletAvailable
            ) {
                return TangemWalletCardConfig
            }
            error("This card is not supported by this configs")
        }
    }
}
