package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.*
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.TapWorkarounds.useOldStyleDerivation
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.features.wallet.models.Currency

fun WalletManagerFactory.makeWalletManagerForApp(
    scanResponse: ScanResponse,
    blockchain: Blockchain,
    derivationParams: DerivationParams?,
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
        scanResponse.cardTypesResolver.isTangemTwins() && scanResponse.secondTwinPublicKey != null -> {
            createTwinWalletManager(
                walletPublicKey = wallet.publicKey,
                pairPublicKey = scanResponse.secondTwinPublicKey!!.hexToBytes(),
                blockchain = environmentBlockchain,
                curve = wallet.curve,
            )
        }
        scanResponse.card.isHdWalletAllowedByApp && seedKey != null && derivationParams != null -> {
            val derivedKeys = scanResponse.derivedKeys[wallet.publicKey.toMapKey()]
            val derivationPath = when (derivationParams) {
                is DerivationParams.Default -> blockchain.derivationPath(derivationParams.style)
                is DerivationParams.Custom -> derivationParams.path
            }
            val derivedKey = derivedKeys?.get(derivationPath)
                ?: return null

            createWalletManager(
                blockchain = environmentBlockchain,
                seedKey = wallet.publicKey,
                derivedKey = derivedKey,
                derivation = derivationParams,
            )
        }
        else -> {
            createLegacyWalletManager(
                blockchain = environmentBlockchain,
                walletPublicKey = wallet.publicKey,
                curve = wallet.curve,
            )
        }
    }
}

fun WalletManagerFactory.makeWalletManagerForApp(
    scanResponse: ScanResponse,
    blockchainNetwork: BlockchainNetwork,
): WalletManager? {
    return makeWalletManagerForApp(
        scanResponse,
        blockchain = blockchainNetwork.blockchain,
        derivationParams = getDerivationParams(blockchainNetwork.derivationPath, scanResponse.card),
    )
}

private fun getDerivationParams(derivationPath: String?, card: CardDTO): DerivationParams? {
    return derivationPath?.let {
        DerivationParams.Custom(
            DerivationPath(it),
        )
    } ?: if (!card.settings.isHDWalletAllowed) {
        null
    } else if (card.useOldStyleDerivation) {
        DerivationParams.Default(DerivationStyle.LEGACY)
    } else {
        DerivationParams.Default(DerivationStyle.NEW)
    }
}

fun WalletManagerFactory.makeWalletManagerForApp(scanResponse: ScanResponse, currency: Currency): WalletManager? {
    return makeWalletManagerForApp(
        scanResponse,
        blockchain = currency.blockchain,
        derivationParams = getDerivationParams(currency.derivationPath, scanResponse.card),
    )
}

fun WalletManagerFactory.makeWalletManagersForApp(
    scanResponse: ScanResponse,
    blockchains: List<Currency>,
): List<WalletManager> {
    return blockchains
        .filter { it.isBlockchain() }
        .mapNotNull { this.makeWalletManagerForApp(scanResponse, it) }
}

fun WalletManagerFactory.makePrimaryWalletManager(scanResponse: ScanResponse): WalletManager? {
    val blockchain = if (scanResponse.card.isTestCard) {
        scanResponse.cardTypesResolver.getBlockchain().getTestnetVersion() ?: return null
    } else {
        scanResponse.cardTypesResolver.getBlockchain()
    }
    val derivationParams = getDerivationParams(null, scanResponse.card)
    return makeWalletManagerForApp(
        scanResponse = scanResponse,
        blockchain = blockchain,
        derivationParams = derivationParams,
    )
}

private fun selectWallet(wallets: List<CardDTO.Wallet>): CardDTO.Wallet? {
    return when (wallets.size) {
        0 -> null
        1 -> wallets[0]
        else -> wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: wallets[0]
    }
}
