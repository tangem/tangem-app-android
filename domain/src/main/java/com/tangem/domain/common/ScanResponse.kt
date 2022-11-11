package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.WalletData
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.domain.common.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.domain.common.TapWorkarounds.isSaltPay
import com.tangem.domain.common.TapWorkarounds.isSaltPayVisa
import com.tangem.domain.common.TapWorkarounds.isSaltPayWallet
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
    fun getBlockchain(): Blockchain {
        return when (productType) {
            ProductType.SaltPay -> if (card.isTestCard) Blockchain.SaltPayTestnet else Blockchain.SaltPay
            ProductType.Note -> card.getTangemNoteBlockchain() ?: Blockchain.Unknown
            else -> {
                val blockchainName: String = walletData?.blockchain ?: return Blockchain.Unknown
                Blockchain.fromId(blockchainName)
            }
        }
    }

    fun getPrimaryToken(): Token? {
        if (card.isSaltPay) return SaltPayWorkaround.tokenFrom(getBlockchain())

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
    fun isSaltPay(): Boolean = productType == ProductType.SaltPay
    fun isSaltPayVisa(): Boolean = card.isSaltPayVisa
    fun isSaltPayWallet(): Boolean = card.isSaltPayWallet
    fun isTangemTwins(): Boolean = productType == ProductType.Twins
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

enum class ProductType {
    Note, Twins, Wallet, SaltPay
}

typealias KeyWalletPublicKey = ByteArrayKey
