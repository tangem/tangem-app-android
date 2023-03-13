package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.WalletData
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.operations.CommandResponse
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
 * Created by Anton Zhilenkov on 07/04/2022.
 */
data class ScanResponse(
    val card: CardDTO,
    val productType: ProductType,
    val walletData: WalletData?,
    val secondTwinPublicKey: String? = null,
    val derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
    val primaryCard: PrimaryCard? = null,
) : CommandResponse {

    val cardTypesResolver: CardTypesResolver = TangemCardTypesResolver(
        card = card,
        productType = productType,
        walletData = walletData,
    )

    fun twinsIsTwinned(): Boolean = card.isTangemTwins && walletData != null && secondTwinPublicKey != null
    fun supportsHdWallet(): Boolean = card.settings.isHDWalletAllowed
    fun supportsBackup(): Boolean = card.settings.isBackupAllowed

    fun hasDerivation(blockchain: Blockchain, rawDerivationPath: String): Boolean {
        return hasDerivation(blockchain, DerivationPath(rawDerivationPath))
    }

    private fun hasDerivation(blockchain: Blockchain, derivationPath: DerivationPath): Boolean {
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

    private fun hasDerivation(curve: EllipticCurve, derivationPath: DerivationPath): Boolean {
        val foundWallet = card.wallets.firstOrNull { it.curve == curve }
            ?: return false
        val extendedPublicKeysMap = derivedKeys[foundWallet.publicKey.toMapKey()] ?: return false
        val extendedPublicKey = extendedPublicKeysMap[derivationPath]
        return extendedPublicKey != null
    }
}

typealias KeyWalletPublicKey = ByteArrayKey
