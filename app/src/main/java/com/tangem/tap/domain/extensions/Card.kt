package com.tangem.tap.domain.extensions

import com.tangem.FirmwareConstraints
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.CardStatus
import com.tangem.commands.wallet.CardWallet
import com.tangem.commands.wallet.WalletStatus
import com.tangem.common.TangemSdkConstants

fun Card.getToken(): Token? {
    val symbol = cardData?.tokenSymbol ?: return null
    val contractAddress = cardData?.tokenContractAddress ?: return null
    val decimals = cardData?.tokenDecimal ?: return null
    if (symbol.isBlank() || contractAddress.isBlank()) return null
    return Token(symbol, contractAddress, decimals)
}

fun Card.getBlockchain(): Blockchain? {
    val blockchainName: String = cardData?.blockchainName ?: return null
    return Blockchain.fromId(blockchainName)
}

fun Card.getSingleWallet(): CardWallet? {
    return wallet(TangemSdkConstants.getDefaultWalletIndex())
}

fun Card.getStatus(): CardStatus {
    if (firmwareVersion < FirmwareConstraints.AvailabilityVersions.walletData) return status!!

    return if (wallets.any { it.status == WalletStatus.Loaded }) {
        CardStatus.Loaded
    } else {
        CardStatus.Empty
    }
}

fun Card.hasSignedHashes(): Boolean {
    return wallets.any { it.status == WalletStatus.Loaded && it.signedHashes ?: 0 > 0 }
}

fun Card.signedHashesCount(): Int {
    return wallets.map { it.signedHashes ?: 0 }.sum()
}

val Card.remainingSignatures: Int?
    get() = this.getSingleWallet()?.remainingSignatures

val Card.isWalletDataSupported: Boolean
    get() = this.firmwareVersion.major >= 4