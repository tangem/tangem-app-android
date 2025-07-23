package com.tangem.domain.common.extensions

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.TapWorkarounds.useOldStyleDerivation
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.configs.Wallet2CardConfig
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse

fun WalletManagerFactory.makeWalletManagerForApp(
    scanResponse: ScanResponse,
    blockchain: Blockchain,
    derivationParams: DerivationParams?,
): WalletManager? {
    val card = scanResponse.card
    val cardConfig = CardConfig.createConfig(card)
    if (card.isTestCard && blockchain.getTestnetVersion() == null) return null
    val supportedCurves = blockchain.getSupportedCurves()

    val wallets = card.wallets.filter { wallet -> supportedCurves.contains(wallet.curve) }
    val wallet = selectWallet(
        wallets = wallets,
        cardConfig = cardConfig,
        blockchain = blockchain,
    ) ?: return null

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
        scanResponse.card.settings.isHDWalletAllowed && seedKey != null && derivationParams != null -> {
            val derivedKeys = scanResponse.derivedKeys[wallet.publicKey.toMapKey()]
            val derivationPath = derivationParams.getPath(blockchain)

            val publicKey = makePublicKey(
                seedKey = wallet.publicKey,
                blockchain = blockchain,
                derivationPath = derivationPath ?: return null,
                derivedWalletKeys = derivedKeys ?: return null,
                isWallet2 = scanResponse.cardTypesResolver.isWallet2(),
            ) ?: return null

            createWalletManager(
                blockchain = environmentBlockchain,
                publicKey = publicKey,
                curve = wallet.curve,
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

fun makePublicKey(
    seedKey: ByteArray,
    blockchain: Blockchain,
    derivationPath: DerivationPath,
    derivedWalletKeys: Map<DerivationPath, ExtendedPublicKey>,
    isWallet2: Boolean,
): Wallet.PublicKey? {
    val derivedKey = derivedWalletKeys[derivationPath] ?: return null

    val derivationKey = Wallet.HDKey(
        path = derivationPath,
        extendedPublicKey = derivedKey,
    )

    // we should generate second key for cardano
    // because cardano address generation for wallet2 requires keys from 2 derivations
    // https://developers.cardano.org/docs/get-started/cardano-serialization-lib/generating-keys/
    if (blockchain == Blockchain.Cardano && isWallet2) {
        val extendedDerivationPath = CardanoUtils.extendedDerivationPath(derivationPath)
        val secondDerivedKey = derivedWalletKeys[extendedDerivationPath] ?: error("No derivation found")

        val secondDerivationKey = Wallet.HDKey(secondDerivedKey, extendedDerivationPath)

        return Wallet.PublicKey(
            seedKey = seedKey,
            derivationType = Wallet.PublicKey.DerivationType.Double(derivationKey, secondDerivationKey),
        )
    }

    return Wallet.PublicKey(
        seedKey = seedKey,
        derivationType = Wallet.PublicKey.DerivationType.Plain(derivationKey),
    )
}

private fun getDerivationParams(card: CardDTO): DerivationParams? {
    return if (!card.settings.isHDWalletAllowed) {
        null
    } else if (card.useOldStyleDerivation) {
        DerivationParams.Default(DerivationStyle.LEGACY)
    } else {
        DerivationParams.Default(DerivationStyle.NEW)
    }
}

fun WalletManagerFactory.makePrimaryWalletManager(scanResponse: ScanResponse): WalletManager? {
    val blockchain = if (scanResponse.card.isTestCard) {
        scanResponse.cardTypesResolver.getBlockchain().getTestnetVersion() ?: return null
    } else {
        scanResponse.cardTypesResolver.getBlockchain()
    }
    val derivationParams = getDerivationParams(scanResponse.card)
    return makeWalletManagerForApp(
        scanResponse = scanResponse,
        blockchain = blockchain,
        derivationParams = derivationParams,
    )
}

private fun selectWallet(
    wallets: List<CardDTO.Wallet>,
    cardConfig: CardConfig,
    blockchain: Blockchain,
): CardDTO.Wallet? {
    return if (cardConfig is Wallet2CardConfig) {
        val primaryCurve = cardConfig.primaryCurve(blockchain)
        wallets.firstOrNull { it.curve == primaryCurve }
    } else {
        when (wallets.size) {
            0 -> null
            1 -> wallets[0]
            else -> wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: wallets[0]
        }
    }
}