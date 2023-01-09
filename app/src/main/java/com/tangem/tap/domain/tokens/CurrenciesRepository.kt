package com.tangem.tap.domain.tokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.common.CardDTO

object CurrenciesRepository {
    fun getBlockchains(
        cardFirmware: CardDTO.FirmwareVersion,
        isTestNet: Boolean = false,
    ): List<Blockchain> {
        val blockchains = if (cardFirmware < FirmwareVersion.MultiWalletAvailable) {
            Blockchain.secp256k1Blockchains(isTestNet)
        } else {
            Blockchain.secp256k1Blockchains(isTestNet) + Blockchain.ed25519OnlyBlockchains(isTestNet)
        }
        return excludeUnsupportedBlockchains(blockchains)
    }

    // Use this list to temporarily exclude a blockchain from the list of tokens.
    private fun excludeUnsupportedBlockchains(blockchains: List<Blockchain>): List<Blockchain> {
        return blockchains.toMutableList().apply {
            removeAll(
                listOf(
//                Any blockchain
                )
            )
        }
    }
}
