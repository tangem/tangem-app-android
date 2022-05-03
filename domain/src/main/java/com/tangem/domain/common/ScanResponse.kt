package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.WalletData
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.domain.common.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.operations.CommandResponse
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
[REDACTED_AUTHOR]
 */
data class ScanResponse(
    val card: Card,
    val productType: ProductType,
    val walletData: WalletData?,
    val secondTwinPublicKey: String? = null,
    val derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
    val primaryCard: PrimaryCard? = null
) : CommandResponse {

    fun getBlockchain(): Blockchain {
        if (productType == ProductType.Note) return card.getTangemNoteBlockchain()
            ?: return Blockchain.Unknown
        val blockchainName: String = walletData?.blockchain ?: return Blockchain.Unknown
        return Blockchain.fromId(blockchainName)
    }

    fun getPrimaryToken(): Token? {
        val cardToken = walletData?.token ?: return null
        return Token(
            cardToken.name,
            cardToken.symbol,
            cardToken.contractAddress,
            cardToken.decimals,
        )
    }

    fun isTangemNote(): Boolean = productType == ProductType.Note
    fun isTangemWallet(): Boolean = productType == ProductType.Wallet
    fun isTangemTwins(): Boolean = productType == ProductType.Twins

    fun supportsHdWallet(): Boolean = card.settings.isHDWalletAllowed
    fun supportsBackup(): Boolean = card.settings.isBackupAllowed

    fun twinsIsTwinned(): Boolean =
        card.isTangemTwins() && walletData != null && secondTwinPublicKey != null

    fun hasDerivation(blockchain: Blockchain, rawDerivationPath: String): Boolean {
        return hasDerivation(blockchain, DerivationPath(rawDerivationPath))
    }

    fun hasDerivation(blockchain: Blockchain, derivationPath: DerivationPath): Boolean {
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

    fun hasDerivation(curve: EllipticCurve, derivationPath: DerivationPath): Boolean {
        val foundWallet = card.wallets.firstOrNull { it.curve == curve }
            ?: return false

        val extendedPublicKeysMap = derivedKeys[foundWallet.publicKey.toMapKey()] ?: return false

        val extendedPublicKey = extendedPublicKeysMap[derivationPath]
        return extendedPublicKey != null
    }
}

enum class ProductType {
    Note, Twins, Wallet
}

typealias KeyWalletPublicKey = ByteArrayKey

fun Card.isTangemTwins(): Boolean = TwinsHelper.getTwinCardNumber(cardId) != null