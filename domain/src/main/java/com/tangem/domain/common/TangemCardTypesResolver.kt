package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.card.WalletData
import com.tangem.common.extensions.guard
import com.tangem.domain.common.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.domain.common.TapWorkarounds.isSaltPay
import com.tangem.domain.common.TapWorkarounds.isSaltPayVisa
import com.tangem.domain.common.TapWorkarounds.isSaltPayWallet
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TapWorkarounds.isTestCard

class TangemCardTypesResolver(
    private val card: CardDTO,
    private val productType: ProductType,
    private val walletData: WalletData?,
) : CardTypesResolver {

    override fun isTangemNote(): Boolean = productType == ProductType.Note
    override fun isTangemWallet(): Boolean = card.settings.isBackupAllowed &&
        card.settings.isHDWalletAllowed &&
        card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable &&
        !card.isSaltPay

    override fun isSaltPay(): Boolean = productType == ProductType.SaltPay
    override fun isSaltPayVisa(): Boolean = card.isSaltPayVisa
    override fun isSaltPayWallet(): Boolean = card.isSaltPayWallet
    override fun isTangemTwins(): Boolean = productType == ProductType.Twins
    override fun isStart2Coin(): Boolean = card.isStart2Coin

    override fun isMultiwalletAllowed(): Boolean =
        !isTangemTwins() && !card.isStart2Coin && !isTangemNote() && !isSaltPay() &&
            (card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable ||
                card.wallets.firstOrNull()?.curve == EllipticCurve.Secp256k1)

    override fun getBlockchain(): Blockchain {
        return when (productType) {
            ProductType.Start2Coin -> if (card.isTestCard) Blockchain.BitcoinTestnet else Blockchain.Bitcoin
            ProductType.SaltPay -> Blockchain.SaltPay
            else -> {
                val blockchainName: String = walletData?.blockchain.guard {
                    if (productType == ProductType.Note) {
                        return card.getTangemNoteBlockchain() ?: Blockchain.Unknown
                    } else {
                        return Blockchain.Unknown
                    }
                }
                Blockchain.fromId(blockchainName)
            }
        }
    }

    override fun getPrimaryToken(): Token? {
        if (isSaltPay()) return SaltPayWorkaround.tokenFrom(getBlockchain())

        val cardToken = walletData?.token ?: return null
        return Token(
            cardToken.name,
            cardToken.symbol,
            cardToken.contractAddress,
            cardToken.decimals,
        )
    }
}

enum class ProductType {
    Note, Twins, Wallet, SaltPay, Start2Coin
}
