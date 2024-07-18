package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.card.WalletData
import com.tangem.domain.common.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TapWorkarounds.isTestCard
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

    override fun isWhiteWallet2(): Boolean = card.batchId == WHITE_WALLET2_BATCH_ID

    override fun isAvroraWallet(): Boolean = card.batchId == AVRORA_WALLET_BATCH_ID

    override fun isTraillantWallet(): Boolean = card.batchId == TRILLIANT_WALLET_BATCH_ID

    override fun isShibaWallet(): Boolean {
        return card.firmwareVersion.compareTo(FirmwareVersion.KeysImportAvailable) == 0
    }

    override fun isTronWallet(): Boolean = card.batchId == TRON_WALLET_BATCH_ID

    override fun isKaspaWallet(): Boolean = card.batchId == KASPA_WALLET_BATCH_ID

    override fun isKaspa2Wallet(): Boolean = card.batchId == KASPA2_WALLET_BATCH_ID

    override fun isKaspaResellerWallet(): Boolean = card.batchId == KASPA_RESELLER_WALLET_BATCH_ID

    override fun isBadWallet(): Boolean = card.batchId == BAD_WALLET_BATCH_ID

    override fun isJrWallet(): Boolean = card.batchId == JR_WALLET_BATCH_ID

    override fun isGrimWallet(): Boolean = card.batchId == GRIM_WALLET_BATCH_ID

    override fun isSatoshiFriendsWallet(): Boolean = card.batchId == SATOSHI_WALLET_BATCH_ID

    override fun isBitcoinPizzaDayWallet(): Boolean = card.batchId == BITCOIN_PIZZA_DAY_WALLET_BATCH_ID

    override fun isVeChainWallet(): Boolean = card.batchId == VECHAIN_WALLET_BATCH_ID

    override fun isNewWorldEliteWallet(): Boolean = card.batchId == NEW_WORLD_ELITE_WALLET_BATCH_ID

    override fun isRedPandaWallet(): Boolean = card.batchId == RED_PANDA_WALLET_BATCH_ID

    override fun isCryptoSethWallet(): Boolean = card.batchId == CRYPTO_SETH_WALLET_BATCH_ID

    override fun isKishuInuWallet(): Boolean = card.batchId == KISHU_INU_WALLET_BATCH_ID

    override fun isBabyDogeWallet(): Boolean = card.batchId == BABY_DOGE_WALLET_BATCH_ID

    override fun isCOQWallet(): Boolean = card.batchId == COQ_WALLET_BATCH_ID

    override fun isCoinMetricaWallet(): Boolean = card.batchId == COIN_METRICA_WALLET_BATCH_ID

    override fun isVoltInuWallet(): Boolean = card.batchId == VOLT_INU_WALLET_BATCH_ID

    override fun isVividWallet(): Boolean = card.batchId == VIVID_LEMON_WALLET_BATCH_ID ||
        card.batchId == VIVID_AQUA_WALLET_BATCH_ID ||
        card.batchId == VIVID_GRAPEFRUIT_WALLET_BATCH_ID

    override fun isPastelWallet(): Boolean = card.batchId == PASTEL_PEACH_WALLET_BATCH_ID ||
        card.batchId == PASTEL_GRASS_WALLET_BATCH_ID ||
        card.batchId == PASTEL_AIR_WALLET_BATCH_ID

    override fun isWhiteWallet(): Boolean {
        return walletData == null && card.firmwareVersion <= FirmwareVersion.HDWalletAvailable
    }

    override fun isWallet2(): Boolean {
        return card.firmwareVersion >= FirmwareVersion.Ed25519Slip0010Available &&
            card.settings.isKeysImportAllowed
    }

    override fun isVisaWallet(): Boolean = productType == ProductType.Visa

    override fun isRing(): Boolean {
        return productType == ProductType.Ring
    }

    override fun isTangemTwins(): Boolean = productType == ProductType.Twins

    override fun isStart2Coin(): Boolean = card.isStart2Coin

    override fun isDevKit(): Boolean = card.batchId == DEV_KIT_CARD_BATCH_ID

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
            ProductType.Visa -> Blockchain.PolygonTestnet
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
        const val TRON_WALLET_BATCH_ID = "AF07"
        const val KASPA_WALLET_BATCH_ID = "AF08"
        const val KASPA2_WALLET_BATCH_ID = "AF25"
        const val KASPA_RESELLER_WALLET_BATCH_ID = "AF31"
        const val BAD_WALLET_BATCH_ID = "AF09"
        const val JR_WALLET_BATCH_ID = "AF14"
        const val GRIM_WALLET_BATCH_ID = "AF13"
        const val SATOSHI_WALLET_BATCH_ID = "AF19"
        const val WHITE_WALLET2_BATCH_ID = "AF15"
        const val TRILLIANT_WALLET_BATCH_ID = "AF16"
        const val AVRORA_WALLET_BATCH_ID = "AF18"
        const val BITCOIN_PIZZA_DAY_WALLET_BATCH_ID = "AF33"
        const val VECHAIN_WALLET_BATCH_ID = "AF29"
        const val NEW_WORLD_ELITE_WALLET_BATCH_ID = "AF26"
        const val RED_PANDA_WALLET_BATCH_ID = "AF34"
        const val CRYPTO_SETH_WALLET_BATCH_ID = "AF32"
        const val KISHU_INU_WALLET_BATCH_ID = "AF52"
        const val BABY_DOGE_WALLET_BATCH_ID = "AF51"
        const val COQ_WALLET_BATCH_ID = "AF28"
        const val COIN_METRICA_WALLET_BATCH_ID = "AF27"
        const val VOLT_INU_WALLET_BATCH_ID = "AF35"
        // VIVID WALLETS
        const val VIVID_LEMON_WALLET_BATCH_ID = "AF40"
        const val VIVID_AQUA_WALLET_BATCH_ID = "AF41"
        const val VIVID_GRAPEFRUIT_WALLET_BATCH_ID = "AF42"
        // PASTEL WALLETS
        const val PASTEL_PEACH_WALLET_BATCH_ID = "AF43"
        const val PASTEL_AIR_WALLET_BATCH_ID = "AF44"
        const val PASTEL_GRASS_WALLET_BATCH_ID = "AF45"
    }
}
