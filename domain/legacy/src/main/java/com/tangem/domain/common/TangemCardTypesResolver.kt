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

    override fun isWhiteWallet(): Boolean {
        return walletData == null && card.firmwareVersion >= FirmwareVersion.HDWalletAvailable
    }

    override fun isWallet2(): Boolean {
        return card.firmwareVersion >= FirmwareVersion.KeysImportAvailable && card.settings.isKeysImportAllowed
    }

    override fun isTangemTwins(): Boolean = productType == ProductType.Twins

    override fun isStart2Coin(): Boolean = card.isStart2Coin

    override fun isDev(): Boolean = card.isTestCard

    override fun isMultiwalletAllowed(): Boolean {
        return !isTangemTwins() && !card.isStart2Coin && !isTangemNote() &&
            (multiWalletAvailable() || card.wallets.firstOrNull()?.curve == EllipticCurve.Secp256k1)
    }

    private fun multiWalletAvailable() = card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable

    override fun getBlockchain(): Blockchain {
        return when (productType) {
            ProductType.Start2Coin -> if (card.isTestCard) Blockchain.BitcoinTestnet else Blockchain.Bitcoin
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

    override fun getBackupCardsCount(): Int = card.wallets.size

    private fun Blockchain.Companion.fromBlockchainName(blockchainName: String): Blockchain {
        // workaround for BSC (BNB) notes cards
        return when (blockchainName) {
            "BINANCE" -> {
                Blockchain.BSC
            }
            "BINANCE/test" -> {
                Blockchain.BSCTestnet
            }
            else -> {
                Blockchain.fromId(blockchainName)
            }
        }
    }
}