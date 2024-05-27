package com.tangem.tap.common.feedback

import android.os.Build
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.crypto.NetworkType
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.tap.common.extensions.stripZeroPlainString
import java.util.concurrent.CopyOnWriteArrayList

class AdditionalFeedbackInfo {

    class EmailWalletInfo(
        val blockchain: Blockchain = Blockchain.Unknown,
        val derivationPath: String = "",
        val outputsCount: String? = null,
        val host: String = "",
        val addresses: String = "",
        val explorerLink: String = "",
        val tokens: List<EmailTokenInfo> = emptyList(),
    )

    class EmailTokenInfo(
        val id: String?,
        val name: String,
        val contractAddress: String,
    )

    var appVersion: String = ""

    // card
    var cardId: String = ""
    var cardFirmwareVersion: String = ""
    var cardIssuer: String = ""
    var cardBlockchain: String = ""
    var userWalletId: String = ""
    var extendedPublicKey: String = ""

    // wallets
    val walletsInfo = CopyOnWriteArrayList<EmailWalletInfo>()
    var onSendErrorWalletInfo: EmailWalletInfo? = null
        private set
    var signedHashesCount: String = ""
        private set

    // device
    var phoneModel: String = Build.MODEL
        private set
    var osVersion: String = Build.VERSION.SDK_INT.toString()
        private set

    // send error
    var destinationAddress: String = ""
        private set
    var amount: String = ""
        private set
    var fee: String = ""
        private set
    var token: String = ""
        private set

    private val Address.name: String
        get() = type.javaClass.simpleName

    @Deprecated("Don't use it directly")
    fun setCardInfo(data: ScanResponse) {
        cardId = data.card.cardId
        cardBlockchain = data.walletData?.blockchain ?: ""
        cardFirmwareVersion = data.card.firmwareVersion.stringValue
        cardIssuer = data.card.issuer.name
        signedHashesCount = formatSignedHashes(data.card.wallets)
        userWalletId = UserWalletIdBuilder.scanResponse(data).build()?.stringValue ?: ""
        extendedPublicKey = runCatching {
            data.card.wallets.firstOrNull { it.extendedPublicKey != null }
                ?.extendedPublicKey
                ?.serialize(networkType = NetworkType.Mainnet).orEmpty()
        }.getOrDefault("couldn't retrieve extended public key")
    }

    @Deprecated("Don't use it directly")
    fun setWalletsInfo(walletManagers: List<WalletManager>) {
        synchronized(walletsInfo) {
            walletsInfo.clear()
            walletsInfo.addAll(elements = walletManagers.map(::createEmailWalletInfo))
        }
    }

    fun updateOnSendError(
        walletManager: WalletManager,
        amountToSend: Amount,
        feeAmount: Amount?,
        destinationAddress: String,
    ) {
        onSendErrorWalletInfo = createEmailWalletInfo(walletManager)
        this.destinationAddress = destinationAddress
        amount = amountToSend.value?.stripZeroPlainString() ?: "0"
        fee = if (feeAmount != null) {
            feeAmount.value?.stripZeroPlainString() ?: "0"
        } else {
            "Unable to receive"
        }
        token = if (amountToSend.type is AmountType.Token) amountToSend.currencySymbol else ""
    }

    private fun createEmailWalletInfo(walletManager: WalletManager): EmailWalletInfo {
        return EmailWalletInfo(
            blockchain = walletManager.wallet.blockchain,
            derivationPath = walletManager.wallet.publicKey.derivationPath?.rawPath ?: "",
            outputsCount = walletManager.outputsCount?.toString(),
            host = walletManager.currentHost,
            addresses = formatAddresses(walletManager.wallet),
            explorerLink = formatExploreUrls(walletManager.wallet),
            tokens = walletManager.cardTokens.map { token ->
                EmailTokenInfo(token.id, token.name, token.contractAddress)
            },
        )
    }

    private fun formatSignedHashes(wallets: List<CardDTO.Wallet>): String {
        return wallets.joinToString("\n") { "Signed hashes: ${it.curve.curve} - ${it.totalSignedHashes}" }
    }

    private fun formatAddresses(wallet: Wallet): String {
        return wallet.formatAddressWith("Multiple address:") {
            "${it.name} - ${it.value}"
        }
    }

    private fun formatExploreUrls(wallet: Wallet): String {
        return wallet.formatAddressWith("Multiple explorers links:") {
            "${it.name} - ${wallet.getExploreUrl(it.value)}"
        }
    }

    @Suppress("MagicNumber")
    private fun Wallet.formatAddressWith(with: String, mapAddress: (Address) -> String): String {
        return if (addresses.size == 1) {
            getExploreUrl(address)
        } else {
            addresses.map { mapAddress(it) }.toMutableList()
                .apply { add(0, with) }
                .joinToString("\n")
        }
    }
}
