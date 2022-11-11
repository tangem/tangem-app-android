package com.tangem.domain.common.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.isTestCard

/**
 * Created by Anton Zhilenkov on 28/04/2022.
 */
val FirmwareVersion.Companion.SolanaTokensAvailable
    get() = FirmwareVersion(4, 52)

fun CardDTO.supportedBlockchains(): List<Blockchain> {
    val supportedBlockchains = when {
        firmwareVersion < FirmwareVersion.MultiWalletAvailable -> {
            Blockchain.fromCurve(EllipticCurve.Secp256k1)
        }

        else -> {
            (Blockchain.fromCurve(EllipticCurve.Secp256k1) + Blockchain.fromCurve(EllipticCurve.Ed25519)).distinct()
        }
    }
    return supportedBlockchains
        .filter { isTestCard == it.isTestnet() }
        .filter { it.isSupportedInApp() }
}

fun CardDTO.supportedTokens(): List<Blockchain> {
    val tokensSupportedByBlockchain = supportedBlockchains().filter { it.canHandleTokens() }.toMutableList()
    val tokensSupportedByCard = when {
        firmwareVersion >= FirmwareVersion.SolanaTokensAvailable -> tokensSupportedByBlockchain
        else -> {
            tokensSupportedByBlockchain.apply {
                remove(Blockchain.Solana)
                remove(Blockchain.SolanaTestnet)
            }
        }
    }
    val filtered = tokensSupportedByCard.filter { isTestCard == it.isTestnet() }
    return filtered
}

fun CardDTO.canHandleBlockchain(blockchain: Blockchain): Boolean {
    return this.supportedBlockchains().contains(blockchain)
}

fun CardDTO.canHandleToken(blockchain: Blockchain): Boolean {
    return this.supportedTokens().contains(blockchain)
}
