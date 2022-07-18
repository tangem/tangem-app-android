package com.tangem.tap.common.feedback

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.extensions.stripZeroPlainString

class AdditionalFeedbackInfo {
    class EmailWalletInfo(
        var blockchain: Blockchain = Blockchain.Unknown,
        var address: String = "",
        var explorerLink: String = "",
        var host: String = "",
        var derivationPath: String = "",
    )

    var appVersion: String = ""

    // card
    var cardId: String = ""
    var cardFirmwareVersion: String = ""
    var cardIssuer: String = ""
    var cardBlockchain: String = ""

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

    fun setAppVersion(context: Context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            appVersion = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    fun setCardInfo(data: ScanResponse) {
        cardId = data.card.cardId
        cardBlockchain = data.walletData?.blockchain ?: ""
        cardFirmwareVersion = data.card.firmwareVersion.stringValue
        cardIssuer = data.card.issuer.name
        signedHashesCount = data.card.wallets
            .joinToString("; ") { "${it.curve.curve} - ${it.totalSignedHashes}" }
    }

    fun setWalletsInfo(walletManagers: List<WalletManager>) {
        walletsInfo.clear()
        tokens.clear()
        walletManagers.forEach { manager ->
            walletsInfo.add(
                EmailWalletInfo(
                    blockchain = manager.wallet.blockchain,
                    address = getAddress(manager.wallet),
                    explorerLink = getExploreUri(manager.wallet),
                    host = manager.currentHost,
                    derivationPath = manager.wallet.publicKey.derivationPath?.rawPath ?: ""
                )
            )
            if (manager.cardTokens.isNotEmpty()) {
                tokens[manager.wallet.blockchain] = manager.cardTokens
            }
        }
    }

    fun updateOnSendError(
        wallet: Wallet,
        host: String,
        amountToSend: Amount,
        feeAmount: Amount,
        destinationAddress: String,
    ) {
        onSendErrorWalletInfo = EmailWalletInfo(
            blockchain = wallet.blockchain,
            address = getAddress(wallet),
            explorerLink = getExploreUri(wallet),
            host = host,
            derivationPath = wallet.publicKey.derivationPath?.rawPath ?: ""
        )

        this.destinationAddress = destinationAddress
        amount = amountToSend.value?.stripZeroPlainString() ?: "0"
        fee = feeAmount.value?.stripZeroPlainString() ?: "0"
        token = if (amountToSend.type is AmountType.Token) amountToSend.currencySymbol else ""
    }

    private fun getAddress(wallet: Wallet): String {
        return if (wallet.addresses.size == 1) {
            wallet.address
        } else {
            val addresses = wallet.addresses.joinToString(", ") {
                "${it.type.javaClass.simpleName} - ${it.value}"
            }
            "Multiple address: $addresses"
        }
    }

    private fun getExploreUri(wallet: Wallet): String {
        return if (wallet.addresses.size == 1) {
            wallet.getExploreUrl(wallet.address)
        } else {
            val links = wallet.addresses.joinToString(", ") {
                "${it.type.javaClass.simpleName} - ${wallet.getExploreUrl(it.value)}"
            }
            "Multiple explorers links: $links"
        }
    }
}