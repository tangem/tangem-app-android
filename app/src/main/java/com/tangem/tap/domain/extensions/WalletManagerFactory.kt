package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toMapKey
import com.tangem.tap.domain.TapWorkarounds.isTestCard
import com.tangem.tap.domain.tasks.product.ScanResponse

fun WalletManagerFactory.makeWalletManagerForApp(
    scanResponse: ScanResponse, blockchain: Blockchain
): WalletManager? {
    val card = scanResponse.card
    if (card.isTestCard && blockchain.getTestnetVersion() == null) return null
    val supportedCurves = blockchain.getSupportedCurves() ?: return null

    val wallets = card.wallets.filter { wallet -> supportedCurves.contains(wallet.curve) }
    val wallet = selectWallet(wallets) ?: return null

    val environmentBlockchain = if (card.isTestCard) blockchain.getTestnetVersion()!! else blockchain

    val seedKey = wallet.extendedPublicKey
    return when {
        scanResponse.isTangemTwins() && scanResponse.secondTwinPublicKey != null -> {
            makeTwinWalletManager(
                card.cardId,
                wallet.publicKey, scanResponse.secondTwinPublicKey.hexToBytes(),
                environmentBlockchain, wallet.curve
            )
        }
        seedKey != null -> {
            val derivedKeys = scanResponse.derivedKeys[wallet.publicKey.toMapKey()]
            val derivedKey = derivedKeys?.get(blockchain.derivationPath())
                ?: return null

            makeWalletManager(
                cardId = card.cardId,
                blockchain = environmentBlockchain,
                seedKey = wallet.publicKey,
                derivedKey = derivedKey
            )
        }
        else -> {
            makeWalletManager(
                cardId = card.cardId,
                blockchain = environmentBlockchain,
                walletPublicKey = wallet.publicKey,
                curve = wallet.curve
            )
        }
    }
}

fun WalletManagerFactory.makeWalletManagersForApp(
    scanResponse: ScanResponse, blockchains: List<Blockchain>,
): List<WalletManager> {
    val isTestCard = scanResponse.card.isTestCard
    val filteredBlockchains = blockchains.mapNotNull { if (isTestCard) it.getTestnetVersion() else it }
    return filteredBlockchains.mapNotNull { makeWalletManagerForApp(scanResponse, it) }
}

fun WalletManagerFactory.makePrimaryWalletManager(
    scanResponse: ScanResponse,
): WalletManager? {
    val blockchain = if (scanResponse.card.isTestCard) {
        scanResponse.getBlockchain().getTestnetVersion() ?: return null
    } else {
        scanResponse.getBlockchain()
    }
    return makeWalletManagerForApp(scanResponse, blockchain)
}

private fun selectWallet(wallets: List<CardWallet>): CardWallet? {
    return when (wallets.size) {
        0 -> null
        1 -> wallets[0]
        else -> wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: wallets[0]
    }
}