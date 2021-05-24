package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.EllipticCurve
import com.tangem.commands.wallet.CardWallet

fun WalletManagerFactory.makeWalletManagerForApp(
    card: Card,
    blockchain: Blockchain,
): WalletManager? {
    val supportedCurves = blockchain.getSupportedCurves() ?: return null
    val wallets = card.getWallets().filter { wallet -> supportedCurves.contains(wallet.curve) }
    val wallet = selectWallet(wallets)
    val publicKey = wallet?.publicKey ?: return null
    val curveToUse = wallet.curve ?: return null
    return makeWalletManager(card.cardId, publicKey, blockchain, curveToUse)
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
    return blockchains.mapNotNull { blockchain ->  makeWalletManagerForApp(card, blockchain) }
}

