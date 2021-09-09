package com.tangem.tap.domain.extensions

import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet

fun Card.getSingleWallet(): CardWallet? {
    return wallets.firstOrNull()
}

fun Card.hasWallets(): Boolean = wallets.isNotEmpty()

fun Card.hasSignedHashes(): Boolean {
    return wallets.any { it.totalSignedHashes ?: 0 > 0 }
}

fun Card.signedHashesCount(): Int {
    return wallets.map { it.totalSignedHashes ?: 0 }.sum()
}

val Card.remainingSignatures: Int?
    get() = this.getSingleWallet()?.remainingSignatures

val Card.isWalletDataSupported: Boolean
    get() = this.firmwareVersion.major >= 4