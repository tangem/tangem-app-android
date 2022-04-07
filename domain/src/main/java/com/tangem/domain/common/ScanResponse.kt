package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.card.Card
import com.tangem.common.card.WalletData
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.domain.common.TapWorkarounds.getTangemNoteBlockchain
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
            Blockchain.fromId(walletData.blockchain)
        )
    }

    fun isTangemNote(): Boolean = productType == ProductType.Note
    fun isTangemWallet(): Boolean = productType == ProductType.Wallet
    fun isTangemTwins(): Boolean = productType == ProductType.Twins

    fun supportsHdWallet(): Boolean = card.settings.isHDWalletAllowed
    fun supportsBackup(): Boolean = card.settings.isBackupAllowed

    fun twinsIsTwinned(): Boolean =
        card.isTangemTwins() && walletData != null && secondTwinPublicKey != null
}

enum class ProductType {
    Note, Twins, Wallet
}

typealias KeyWalletPublicKey = ByteArrayKey

fun Card.isTangemTwins(): Boolean = TwinsHelper.getTwinCardNumber(cardId) != null