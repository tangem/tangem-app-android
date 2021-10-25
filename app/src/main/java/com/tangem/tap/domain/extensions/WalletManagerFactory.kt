package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.tap.domain.TapWorkarounds.isTestCard
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.twins.isTangemTwin

fun WalletManagerFactory.makeWalletManagerForApp(
    card: Card,
    blockchain: Blockchain,
): WalletManager? {
    val supportedCurves = blockchain.getSupportedCurves() ?: return null
    val wallets = card.wallets.filter { wallet -> supportedCurves.contains(wallet.curve) }
    val wallet = selectWallet(wallets)
    val publicKey = wallet?.publicKey ?: return null
    val curveToUse = wallet.curve ?: return null
    return if (card.isTestCard) {
        blockchain.getTestnetVersion()?.let {
            makeWalletManager(card.cardId, publicKey, it, curveToUse)
        }
    } else {
        makeWalletManager(card.cardId, publicKey, blockchain, curveToUse)
    }
}

private fun selectWallet(wallets: List<CardWallet>): CardWallet? {
    return when (wallets.size) {
        0 -> null
        1 -> wallets[0]
        else -> wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: wallets[0]
    }
}

fun WalletManagerFactory.makeWalletManagersForApp(
    card: Card, blockchains: List<Blockchain>,
): List<WalletManager> {
    return if (card.isTestCard) {
        blockchains.mapNotNull { blockchain ->
            blockchain.getTestnetVersion()?.let { makeWalletManagerForApp(card, it) }
        }
    } else {
        blockchains.mapNotNull { blockchain -> makeWalletManagerForApp(card, blockchain) }
    }
}

fun WalletManagerFactory.makePrimaryWalletManager(
    data: ScanResponse,
): WalletManager? {
    val card = data.card
    val blockchain = if (card.isTestCard) {
        data.getBlockchain().getTestnetVersion()
    } else {
        data.getBlockchain()
    }
    val supportedCurves = blockchain?.getSupportedCurves() ?: return null

    val wallets = card.wallets.filter { wallet -> supportedCurves.contains(wallet.curve) }
    val wallet = selectWallet(wallets)
    val publicKey = wallet?.publicKey ?: return null
    val curveToUse = wallet.curve

    return if (card.isTangemTwin() && data.secondTwinPublicKey != null) {
        makeMultisigWalletManager(
            cardId = card.cardId,
            walletPublicKey = publicKey, pairPublicKey = data.secondTwinPublicKey.hexToBytes(),
            blockchain = blockchain, curve = curveToUse
        )
    } else {
        makeWalletManager(card.cardId, publicKey, blockchain, curveToUse)
    }
}