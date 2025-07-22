package com.tangem.domain.common.util

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.*
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.configs.Wallet2CardConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet

val ScanResponse.cardTypesResolver: CardTypesResolver
    get() = TangemCardTypesResolver(
        card = card,
        productType = productType,
        walletData = walletData,
    )

val UserWallet.derivationStyleProvider: DerivationStyleProvider
    get() = when (this) {
        is UserWallet.Cold -> this.scanResponse.derivationStyleProvider
        is UserWallet.Hot -> TangemHotDerivationStyleProvider()
    }

val ScanResponse.derivationStyleProvider: DerivationStyleProvider
    get() = card.derivationStyleProvider

val CardDTO.derivationStyleProvider: DerivationStyleProvider
    get() = TangemDerivationStyleProvider(this)

val UserWallet.Cold.cardTypesResolver: CardTypesResolver
    get() = scanResponse.cardTypesResolver

fun ScanResponse.twinsIsTwinned(): Boolean = card.isTangemTwins && walletData != null && secondTwinPublicKey != null

fun ScanResponse.hasDerivation(blockchain: Blockchain, rawDerivationPath: String): Boolean {
    return hasDerivation(blockchain, DerivationPath(rawDerivationPath))
}

private fun ScanResponse.hasDerivation(blockchain: Blockchain, derivationPath: DerivationPath): Boolean {
    val isTestnet = card.isTestCard || blockchain.isTestnet()
    val config = CardConfig.createConfig(card)
    return if (config is Wallet2CardConfig) {
        // new logic for wallet2
        val primaryCurve = config.primaryCurve(blockchain)
        primaryCurve?.let { hasDerivation(it, derivationPath) } == true
    } else {
        // leave logic for legacy wallets
        when {
            Blockchain.secp256k1Blockchains(isTestnet).contains(blockchain) -> {
                hasDerivation(EllipticCurve.Secp256k1, derivationPath)
            }
            Blockchain.ed25519Blockchains(isTestnet).contains(blockchain) -> {
                hasDerivation(EllipticCurve.Ed25519, derivationPath)
            }
            else -> false
        }
    }
}

private fun ScanResponse.hasDerivation(curve: EllipticCurve, derivationPath: DerivationPath): Boolean {
    val foundWallet = card.wallets.firstOrNull { it.curve == curve }
        ?: return false
    val extendedPublicKeysMap = derivedKeys[foundWallet.publicKey.toMapKey()] ?: return false
    val extendedPublicKey = extendedPublicKeysMap[derivationPath]
    return extendedPublicKey != null
}

/**
 * Get total cards count in wallets set for this [ScanResponse] card
 *
 * @return null if wallet is not multi-currency or total cards count
 */
fun ScanResponse.getCardsCount(): Int? {
    if (cardTypesResolver.isTangemTwins()) return 2
    if (!cardTypesResolver.isMultiwalletAllowed()) return null

    return when (val status = card.backupStatus) {
        is CardDTO.BackupStatus.Active -> status.cardCount + 1
        is CardDTO.BackupStatus.NoBackup,
        is CardDTO.BackupStatus.CardLinked,
        null, // Multi-currency wallet without backup function. Example, 4.12
        -> 1
    }
}

/**
 * Get backup cards count for this [ScanResponse] card
 *
 * @return null if wallet is not multi-currency or total cards count
 */
fun ScanResponse.getBackupCardsCount(): Int? {
    return if (cardTypesResolver.isMultiwalletAllowed()) {
        when (val status = card.backupStatus) {
            is CardDTO.BackupStatus.Active -> status.cardCount
            else -> 0
        }
    } else {
        null
    }
}