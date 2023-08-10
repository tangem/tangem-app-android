package com.tangem.domain.common.util

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.TangemCardTypesResolver
import com.tangem.domain.common.TangemDerivationStyleProvider
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.models.scan.ScanResponse

val ScanResponse.cardTypesResolver: CardTypesResolver
    get() = TangemCardTypesResolver(
        card = card,
        productType = productType,
        walletData = walletData,
    )

val ScanResponse.derivationStyleProvider: DerivationStyleProvider
    get() = TangemDerivationStyleProvider(
        cardTypesResolver,
        card,
    )

fun ScanResponse.twinsIsTwinned(): Boolean = card.isTangemTwins && walletData != null && secondTwinPublicKey != null
fun ScanResponse.supportsHdWallet(): Boolean = card.settings.isHDWalletAllowed
fun ScanResponse.supportsBackup(): Boolean = card.settings.isBackupAllowed

fun ScanResponse.hasDerivation(blockchain: Blockchain, rawDerivationPath: String): Boolean {
    return hasDerivation(blockchain, DerivationPath(rawDerivationPath))
}

private fun ScanResponse.hasDerivation(blockchain: Blockchain, derivationPath: DerivationPath): Boolean {
    val isTestnet = card.isTestCard || blockchain.isTestnet()
    return when {
        Blockchain.secp256k1Blockchains(isTestnet).contains(blockchain) -> {
            hasDerivation(EllipticCurve.Secp256k1, derivationPath)
        }
        Blockchain.ed25519OnlyBlockchains(isTestnet).contains(blockchain) -> {
            hasDerivation(EllipticCurve.Ed25519, derivationPath)
        }
        else -> false
    }
}

private fun ScanResponse.hasDerivation(curve: EllipticCurve, derivationPath: DerivationPath): Boolean {
    val foundWallet = card.wallets.firstOrNull { it.curve == curve }
        ?: return false
    val extendedPublicKeysMap = derivedKeys[foundWallet.publicKey.toMapKey()] ?: return false
    val extendedPublicKey = extendedPublicKeysMap[derivationPath]
    return extendedPublicKey != null
}
