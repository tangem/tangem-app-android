package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.*
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toMapKey
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.TapWorkarounds.useOldStyleDerivation
import com.tangem.tap.domain.tokens.BlockchainNetwork
import com.tangem.tap.features.wallet.redux.Currency

fun WalletManagerFactory.makeWalletManagerForApp(
    scanResponse: ScanResponse,
    blockchain: Blockchain,
    derivationParams: DerivationParams?
): WalletManager? {
    val card = scanResponse.card
    if (card.isTestCard && blockchain.getTestnetVersion() == null) return null
    val supportedCurves = blockchain.getSupportedCurves()

    val wallets = card.wallets.filter { wallet -> supportedCurves.contains(wallet.curve) }
    val wallet = selectWallet(wallets) ?: return null

    val environmentBlockchain =
        if (card.isTestCard) blockchain.getTestnetVersion()!! else blockchain

    val seedKey = wallet.extendedPublicKey
    return when {
        scanResponse.isTangemTwins() && scanResponse.secondTwinPublicKey != null -> {
            makeTwinWalletManager(
                card.cardId,
                wallet.publicKey, scanResponse.secondTwinPublicKey!!.hexToBytes(),
                environmentBlockchain, wallet.curve
            )
        }
        seedKey != null && derivationParams != null -> {
            val derivedKeys = scanResponse.derivedKeys[wallet.publicKey.toMapKey()]
            val derivationPath = when (derivationParams) {
                is DerivationParams.Default -> blockchain.derivationPath(derivationParams.style)
                is DerivationParams.Custom -> derivationParams.path
            }
            val derivedKey = derivedKeys?.get(derivationPath)
                ?: return null

            makeWalletManager(
                cardId = card.cardId,
                blockchain = environmentBlockchain,
                seedKey = wallet.publicKey,
                derivedKey = derivedKey,
                derivation = derivationParams
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

fun WalletManagerFactory.makeWalletManagerForApp(
    scanResponse: ScanResponse, blockchainNetwork: BlockchainNetwork
): WalletManager? {
    return makeWalletManagerForApp(
        scanResponse,
        blockchain = blockchainNetwork.blockchain,
        derivationParams = getDerivationParams(blockchainNetwork.derivationPath, scanResponse.card)
    )
}

private fun getDerivationParams(derivationPath: String?, card: Card): DerivationParams? {
    return derivationPath?.let {
        DerivationParams.Custom(
            DerivationPath(it)
        )
    } ?: if (!card.settings.isHDWalletAllowed) {
        null
    } else if (card.useOldStyleDerivation) {
        DerivationParams.Default(DerivationStyle.LEGACY)
    } else {
        DerivationParams.Default(DerivationStyle.NEW)
    }
}

fun WalletManagerFactory.makeWalletManagerForApp(
    scanResponse: ScanResponse, currency: Currency
): WalletManager? {
    return makeWalletManagerForApp(
        scanResponse,
        blockchain = currency.blockchain,
        derivationParams = getDerivationParams(currency.derivationPath, scanResponse.card)
    )
}

fun WalletManagerFactory.makeWalletManagersForApp(
    scanResponse: ScanResponse, blockchains: List<BlockchainNetwork>,
): List<WalletManager> {
    return blockchains.mapNotNull { this.makeWalletManagerForApp(scanResponse, it) }
}

fun WalletManagerFactory.makePrimaryWalletManager(
    scanResponse: ScanResponse,
): WalletManager? {
    val blockchain = if (scanResponse.card.isTestCard) {
        scanResponse.getBlockchain().getTestnetVersion() ?: return null
    } else {
        scanResponse.getBlockchain()
    }
    val derivationParams = getDerivationParams(null, scanResponse.card)
    return makeWalletManagerForApp(
        scanResponse = scanResponse,
        blockchain = blockchain,
        derivationParams = derivationParams
    )
}

private fun selectWallet(wallets: List<CardWallet>): CardWallet? {
    return when (wallets.size) {
        0 -> null
        1 -> wallets[0]
        else -> wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: wallets[0]
    }
}