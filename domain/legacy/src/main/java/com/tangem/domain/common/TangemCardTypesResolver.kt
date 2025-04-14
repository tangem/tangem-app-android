package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.card.WalletData
import com.tangem.domain.common.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.TapWorkarounds.isWallet2
import com.tangem.domain.common.visa.VisaUtilities
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.operations.attestation.Attestation

@Suppress("TooManyFunctions")
internal class TangemCardTypesResolver(
    private val card: CardDTO,
    private val productType: ProductType,
    private val walletData: WalletData?,
) : CardTypesResolver {

    override fun isTangemNote(): Boolean = productType == ProductType.Note

    override fun isTangemWallet(): Boolean {
        return card.settings.isBackupAllowed && card.settings.isHDWalletAllowed &&
            card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable
    }

    override fun isShibaWallet(): Boolean {
        return card.firmwareVersion.compareTo(FirmwareVersion.KeysImportAvailable) == 0
    }

    override fun isWhiteWallet(): Boolean {
        return walletData == null && card.firmwareVersion <= FirmwareVersion.HDWalletAvailable
    }

    override fun isWallet2(): Boolean = card.isWallet2

    override fun isVisaWallet(): Boolean = productType == ProductType.Visa

    override fun isRing(): Boolean {
        return productType == ProductType.Ring
    }

    override fun isTangemTwins(): Boolean = productType == ProductType.Twins

    override fun isStart2Coin(): Boolean = card.isStart2Coin

    override fun isDevKit(): Boolean = card.batchId == DEV_KIT_CARD_BATCH_ID

    override fun isSingleWallet(): Boolean = !isMultiwalletAllowed() && !isSingleWalletWithToken() && !isVisaWallet()

    override fun isSingleWalletWithToken(): Boolean = walletData?.token != null && !isMultiwalletAllowed()

    override fun isMultiwalletAllowed(): Boolean {
        return !isTangemTwins() &&
            !card.isStart2Coin &&
            !isTangemNote() &&
            !isVisaWallet() &&
            (multiWalletAvailable() || card.wallets.firstOrNull()?.curve == EllipticCurve.Secp256k1)
    }

    private fun multiWalletAvailable() = card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable

    override fun getBlockchain(): Blockchain {
        return when (productType) {
            ProductType.Start2Coin -> if (card.isTestCard) Blockchain.BitcoinTestnet else Blockchain.Bitcoin
            ProductType.Visa -> VisaUtilities.visaBlockchain
            else -> {
                val blockchainName: String = walletData?.blockchain
                    ?: if (productType == ProductType.Note) {
                        return card.getTangemNoteBlockchain() ?: Blockchain.Unknown
                    } else {
                        return Blockchain.Unknown
                    }
                Blockchain.fromBlockchainName(blockchainName)
            }
        }
    }

    override fun getPrimaryToken(): Token? {
        val cardToken = walletData?.token ?: return null
        return Token(
            cardToken.name,
            cardToken.symbol,
            cardToken.contractAddress,
            cardToken.decimals,
        )
    }

    override fun isReleaseFirmwareType(): Boolean = card.firmwareVersion.type == FirmwareVersion.FirmwareType.Release

    override fun getRemainingSignatures(): Int? = card.wallets.firstOrNull()?.remainingSignatures

    override fun getCardId(): String {
        return if (isTangemTwins()) {
            card.getTwinCardIdForUser()
        } else {
            card.cardId
        }
    }

    override fun isTestCard(): Boolean = card.isTestCard

    override fun isAttestationFailed(): Boolean = card.attestation.status == Attestation.Status.Failed

    override fun hasWalletSignedHashes(): Boolean {
        return card.wallets.any {
            val totalSignedHashes = it.totalSignedHashes ?: 0
            totalSignedHashes > 0
        }
    }

    private fun Blockchain.Companion.fromBlockchainName(blockchainName: String): Blockchain {
        // workaround for BSC (BNB) notes cards
        return when (blockchainName) {
            "BINANCE" -> {
                Blockchain.BSC
            }
            "BINANCE/test" -> {
                Blockchain.BSCTestnet
            }
            "CARDANO" -> {
                Blockchain.Cardano
            }
            else -> {
                Blockchain.fromId(blockchainName)
            }
        }
    }

    private companion object {

        const val DEV_KIT_CARD_BATCH_ID = "CB83"
    }
}