package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.EllipticCurve

fun WalletManagerFactory.makeWalletManagerForApp(card: Card, blockchain: Blockchain): WalletManager? {
    val curve = blockchain.getCurve() ?: return null
    val publicKey = card.getWallets().firstOrNull { it.curve == curve }?.publicKey ?: return null
    return makeWalletManager(card.cardId, publicKey, blockchain, curve)
}

fun WalletManagerFactory.makeWalletManagersForApp(
        card: Card, blockchains: List<Blockchain>
): List<WalletManager> {
    return makeWalletManagersForCurve(card, blockchains, EllipticCurve.Secp256k1) +
            makeWalletManagersForCurve(card, blockchains, EllipticCurve.Ed25519)
}

fun WalletManagerFactory.makeWalletManagersForCurve(
        card: Card, blockchains: List<Blockchain>, curve: EllipticCurve
): List<WalletManager> {
    val blockchainsForCurve = blockchains.filter { it.getCurve() == curve }
    val walletPublicKey = card.getWallets().firstOrNull { it.curve == curve }?.publicKey

    return if (walletPublicKey != null) {
        makeWalletManagers(card.cardId, walletPublicKey, blockchainsForCurve, curve)
    } else {
        emptyList()
    }
}
