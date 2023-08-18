package com.tangem.domain.common.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.models.scan.CardDTO

/**
[REDACTED_AUTHOR]
 */
val FirmwareVersion.Companion.SolanaTokensAvailable
    get() = FirmwareVersion(4, 52)

fun CardDTO.supportedBlockchains(cardTypesResolver: CardTypesResolver): List<Blockchain> {
    val supportedBlockchains = if (firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
        Blockchain.fromCurve(EllipticCurve.Secp256k1).toMutableList()
    } else if (!cardTypesResolver.isWallet2() && !cardTypesResolver.isTangemWallet()) {
        // need for old multiwallet that supports only secp256k1
        wallets.flatMap { Blockchain.fromCurve(it.curve) }.distinct().toMutableList()
    } else {
        Blockchain.values().toMutableList()
    }
    // disabled Cardano for wallet 2 for now, should be enabled after key processed
    // ([REDACTED_JIRA])
    if (cardTypesResolver.isWallet2()) {
        supportedBlockchains.apply {
            remove(Blockchain.Cardano)
        }
    }
    return supportedBlockchains
        .filter { isTestCard == it.isTestnet() }
        .filter { it.isSupportedInApp() }
}

fun CardDTO.supportedTokens(cardTypesResolver: CardTypesResolver): List<Blockchain> {
    val tokensSupportedByBlockchain = supportedBlockchains(cardTypesResolver)
        .filter { it.canHandleTokens() }
        .toMutableList()
    val tokensSupportedByCard = when {
        firmwareVersion >= FirmwareVersion.SolanaTokensAvailable -> tokensSupportedByBlockchain
        else -> {
            tokensSupportedByBlockchain.apply {
                remove(Blockchain.Solana)
                remove(Blockchain.SolanaTestnet)
            }
        }
    }

    return tokensSupportedByCard.filter { isTestCard == it.isTestnet() }
}

fun CardDTO.canHandleToken(blockchain: Blockchain, cardTypesResolver: CardTypesResolver): Boolean {
    return this.supportedTokens(cardTypesResolver).contains(blockchain)
}

fun CardDTO.canHandleBlockchain(blockchain: Blockchain, cardTypesResolver: CardTypesResolver): Boolean {
    return this.supportedBlockchains(cardTypesResolver).contains(blockchain)
}