package com.tangem.domain.card.common.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.TapWorkarounds.isTestCard
import com.tangem.domain.card.configs.CardConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet

// TODO: refactor [REDACTED_JIRA]\

/**
[REDACTED_AUTHOR]
 */
val FirmwareVersion.Companion.SolanaTokensAvailable
    get() = FirmwareVersion(4, 52)

val hotWalletExcludedBlockchains = setOf(
    Blockchain.Hedera,
    Blockchain.HederaTestnet,
)

fun UserWallet.supportedBlockchains(excludedBlockchains: ExcludedBlockchains): List<Blockchain> {
    return when (this) {
        is UserWallet.Cold -> {
            scanResponse.card.supportedBlockchains(
                excludedBlockchains = excludedBlockchains,
                cardTypesResolver = this.cardTypesResolver,
            )
        }
        is UserWallet.Hot -> {
            Blockchain.entries.filter {
                it.isTestnet().not() && it !in excludedBlockchains && it !in hotWalletExcludedBlockchains
            }
        }
    }
}

fun CardDTO.supportedBlockchains(
    cardTypesResolver: CardTypesResolver,
    excludedBlockchains: ExcludedBlockchains,
): List<Blockchain> {
    val supportedBlockchains = if (firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
        Blockchain.fromCurve(EllipticCurve.Secp256k1).toMutableList()
    } else if (!cardTypesResolver.isWallet2() && !cardTypesResolver.isTangemWallet()) {
        // need for old multiwallet that supports only secp256k1
        wallets.flatMap { Blockchain.fromCurve(it.curve) }.distinct().toMutableList()
    } else {
        // multiwallet supports all blockchains, move this logic to config
        Blockchain.entries.toMutableList()
    }
    return supportedBlockchains
        .filter { isTestCard == it.isTestnet() }
        .filter { it !in excludedBlockchains }
}

fun CardDTO.supportedTokens(
    cardTypesResolver: CardTypesResolver,
    excludedBlockchains: ExcludedBlockchains,
): List<Blockchain> {
    val tokensSupportedByBlockchain = supportedBlockchains(cardTypesResolver, excludedBlockchains)
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

fun UserWallet.canHandleToken(supportedTokens: List<Blockchain>, blockchain: Blockchain): Boolean {
    return when (this) {
        is UserWallet.Cold -> {
            scanResponse.card.canHandleToken(
                supportedTokens = supportedTokens,
                blockchain = blockchain,
                cardTypesResolver = scanResponse.cardTypesResolver,
            )
        }
        is UserWallet.Hot -> blockchain in supportedTokens && blockchain !in hotWalletExcludedBlockchains
    }
}

fun UserWallet.canHandleToken(blockchain: Blockchain, excludedBlockchains: ExcludedBlockchains): Boolean {
    return when (this) {
        is UserWallet.Cold -> {
            scanResponse.card.canHandleToken(
                blockchain = blockchain,
                excludedBlockchains = excludedBlockchains,
                cardTypesResolver = scanResponse.cardTypesResolver,
            )
        }

        is UserWallet.Hot -> blockchain.isTestnet().not() &&
            blockchain !in excludedBlockchains &&
            blockchain !in hotWalletExcludedBlockchains &&
            blockchain.canHandleTokens()
    }
}

fun UserWallet.canHandleBlockchain(blockchain: Blockchain, excludedBlockchains: ExcludedBlockchains): Boolean {
    return when (this) {
        is UserWallet.Cold -> {
            scanResponse.card.canHandleBlockchain(
                blockchain = blockchain,
                excludedBlockchains = excludedBlockchains,
                cardTypesResolver = scanResponse.cardTypesResolver,
            )
        }
        is UserWallet.Hot -> blockchain.isTestnet().not() &&
            blockchain !in excludedBlockchains &&
            blockchain !in hotWalletExcludedBlockchains
    }
}

/**
 * The same as [CardDTO.supportedTokens] but with supportedTokens input, if previously calculated
 */
fun CardDTO.canHandleToken(
    supportedTokens: List<Blockchain>,
    blockchain: Blockchain,
    cardTypesResolver: CardTypesResolver,
): Boolean {
    val cardConfig = CardConfig.createConfig(this)
    val primaryCurveForBlockchain = cardConfig.primaryCurve(blockchain)
    val isContainsBlockchain = supportedTokens.contains(blockchain)
    val isWalletForCurveExists = wallets.any { it.curve == primaryCurveForBlockchain }
    // fixme: check for first wallets with 1 curve and remove condition
    return if (cardTypesResolver.isTangemWallet() || cardTypesResolver.isWallet2()) {
        // if there's no wallet on card for blockchain with given curve
        isContainsBlockchain && isWalletForCurveExists
    } else {
        isContainsBlockchain
    }
}

fun CardDTO.canHandleToken(
    blockchain: Blockchain,
    cardTypesResolver: CardTypesResolver,
    excludedBlockchains: ExcludedBlockchains,
): Boolean {
    val cardConfig = CardConfig.createConfig(this)
    val primaryCurveForBlockchain = cardConfig.primaryCurve(blockchain)
    val isContainsBlockchain = blockchain in supportedTokens(cardTypesResolver, excludedBlockchains)
    val isWalletForCurveExists = wallets.any { it.curve == primaryCurveForBlockchain }
    // fixme: check for first wallets with 1 curve and remove condition
    return if (cardTypesResolver.isTangemWallet() || cardTypesResolver.isWallet2()) {
        // if there's no wallet on card for blockchain with given curve
        isContainsBlockchain && isWalletForCurveExists
    } else {
        isContainsBlockchain
    }
}

fun CardDTO.canHandleBlockchain(
    blockchain: Blockchain,
    cardTypesResolver: CardTypesResolver,
    excludedBlockchains: ExcludedBlockchains,
): Boolean {
    val cardConfig = CardConfig.createConfig(this)
    val primaryCurveForBlockchain = cardConfig.primaryCurve(blockchain)
    val isContainsBlockchain =
        blockchain in supportedBlockchains(cardTypesResolver, excludedBlockchains)
    val isWalletForCurveExists = wallets.any { it.curve == primaryCurveForBlockchain }
    // fixme: check for first wallets with 1 curve and remove condition
    return if (cardTypesResolver.isTangemWallet() || cardTypesResolver.isWallet2()) {
        // if there's no wallet on card for blockchain with given curve
        isContainsBlockchain && isWalletForCurveExists
    } else {
        isContainsBlockchain
    }
}