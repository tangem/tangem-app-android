package com.tangem.tap.common.feedback

import android.os.Build
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.address.Address
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.util.userWalletId
import com.tangem.tap.common.extensions.stripZeroPlainString

class AdditionalFeedbackInfo {
    class EmailWalletInfo(
        var blockchain: Blockchain = Blockchain.Unknown,
        var derivationPath: String = "",
        var outputsCount: String? = null,
        var host: String = "",
        var addresses: String = "",
        var explorerLink: String = "",
    )

    var appVersion: String = ""

    // card
    var cardId: String = ""
    var cardFirmwareVersion: String = ""
    var cardIssuer: String = ""
    var cardBlockchain: String = ""
    var userWalletId: String = ""

    // wallets
    internal val walletsInfo = mutableListOf<EmailWalletInfo>()
    internal val tokens = mutableMapOf<Blockchain, Collection<Token>>()
    internal var onSendErrorWalletInfo: EmailWalletInfo? = null
    var signedHashesCount: String = ""

    // device
    var phoneModel: String = Build.MODEL
    var osVersion: String = Build.VERSION.SDK_INT.toString()

    // send error
    var destinationAddress: String = ""
    var amount: String = ""
    var fee: String = ""
    var token: String = ""

    fun setCardInfo(data: ScanResponse) {
        cardId = data.card.cardId
        cardBlockchain = data.walletData?.blockchain ?: ""
        cardFirmwareVersion = data.card.firmwareVersion.stringValue
        cardIssuer = data.card.issuer.name
        signedHashesCount = formatSignedHashes(data.card.wallets)
        userWalletId = data.card.userWalletId.stringValue
    }

    fun setWalletsInfo(walletManagers: List<WalletManager>) {
        walletsInfo.clear()
        tokens.clear()
        walletManagers.forEach { manager ->
            walletsInfo.add(createEmailWalletInfo(manager))
            if (manager.cardTokens.isNotEmpty()) {
                tokens[manager.wallet.blockchain] = manager.cardTokens
            }
        }
    }

    fun updateOnSendError(
        walletManager: WalletManager,
        amountToSend: Amount,
        feeAmount: Amount,
        destinationAddress: String,
    ) {
        onSendErrorWalletInfo = createEmailWalletInfo(walletManager)
        this.destinationAddress = destinationAddress
        amount = amountToSend.value?.stripZeroPlainString() ?: "0"
        fee = feeAmount.value?.stripZeroPlainString() ?: "0"
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

    private fun Wallet.formatAddressWith(with: String, mapAddress: (Address) -> String): String {
        return if (addresses.size == 1) {
            getExploreUrl(address)
        } else {
            addresses.map { mapAddress(it) }.toMutableList()
                .apply { add(0, with) }
                .joinToString("\n")
        }
    }

    private val Address.name: String
        get() = type.javaClass.simpleName
}
