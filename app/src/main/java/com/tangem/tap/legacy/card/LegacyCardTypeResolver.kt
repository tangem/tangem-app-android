package com.tangem.tap.legacy.card

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.card.WalletData
import com.tangem.domain.card.CardTypeResolver
import com.tangem.domain.common.Provider
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType

internal class LegacyCardTypeResolver(
    private val cardProvider: Provider<CardDTO>,
    private val productTypeProvider: Provider<ProductType>,
    private val walletDataProvider: Provider<WalletData?>,
) : CardTypeResolver {

    private val card: CardDTO get() = cardProvider.invoke()
    private val productType: ProductType get() = productTypeProvider.invoke()
    private val walletData: WalletData? get() = walletDataProvider.invoke()

    override fun isTangemNote(): Boolean = productType == ProductType.Note

    override fun isTangemWallet(): Boolean {
        return with(card.settings) {
            isBackupAllowed && isHDWalletAllowed && card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable
        }
    }

    override fun isWallet2(): Boolean {
        return with(card) {
            firmwareVersion >= FirmwareVersion.KeysImportAvailable && settings.isKeysImportAllowed
        }
    }

    override fun isTangemTwins(): Boolean = productType == ProductType.Twins

    override fun isStart2Coin(): Boolean = card.isStart2Coin

    override fun isMultiwalletAllowed(): Boolean {
        return (multiWalletAvailable() || card.wallets.firstOrNull()?.curve == EllipticCurve.Secp256k1) &&
            !isTangemTwins() && !card.isStart2Coin && !isTangemNote()
    }

    private fun multiWalletAvailable() = card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable

    override fun getBlockchain(): Blockchain {
        if (productType == ProductType.Start2Coin) {
            return if (card.isTestCard) Blockchain.BitcoinTestnet else Blockchain.Bitcoin
        }

        val blockchainName = walletData?.blockchain
            ?: return if (productType == ProductType.Note) {
                TANGEM_NOTE_BATCHES[card.batchId] ?: Blockchain.Unknown
            } else {
                Blockchain.Unknown
            }

        return Blockchain.fromBlockchainName(blockchainName)
    }

    override fun getPrimaryToken(): Token? {
        val cardToken = walletData?.token ?: return null
        return Token(
            name = cardToken.name,
            symbol = cardToken.symbol,
            contractAddress = cardToken.contractAddress,
            decimals = cardToken.decimals,
        )
    }

    private fun Blockchain.Companion.fromBlockchainName(name: String): Blockchain {
        // workaround for BSC (BNB) notes cards
        return when (name) {
            "BINANCE" -> Blockchain.BSC
            "BINANCE/test" -> Blockchain.BSCTestnet
            else -> Blockchain.fromId(name)
        }
    }

    private companion object {
        val TANGEM_NOTE_BATCHES = mapOf(
            "AB01" to Blockchain.Bitcoin,
            "AB02" to Blockchain.Ethereum,
            "AB03" to Blockchain.CardanoShelley,
            "AB04" to Blockchain.Dogecoin,
            "AB05" to Blockchain.BSC,
            "AB06" to Blockchain.XRP,
            "AB07" to Blockchain.Bitcoin,
            "AB08" to Blockchain.Ethereum,
            "AB09" to Blockchain.Bitcoin, // new batches for 3.34
            "AB10" to Blockchain.Ethereum,
            "AB11" to Blockchain.Bitcoin,
            "AB12" to Blockchain.Ethereum,
        )
    }
}