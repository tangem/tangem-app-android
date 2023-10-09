package com.tangem.tap.common.feedback

import android.os.Build
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.userwallets.UserWalletIdBuilder
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.scope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AdditionalFeedbackInfo(
    userWalletsListManager: UserWalletsListManager,
    walletManagersFacade: WalletManagersFacade,
    walletFeatureToggles: WalletFeatureToggles,
) {

    init {
        if (walletFeatureToggles.isRedesignedScreenEnabled) {
            userWalletsListManager.selectedUserWallet
                .distinctUntilChanged()
                .onEach { userWallet ->
                    setCardInfo(data = userWallet.scanResponse)

                    walletManagersFacade.getAll(userWalletId = userWallet.walletId)
                        .onEach(::setWalletsInfo)
                        .launchIn(scope)
                }
                .launchIn(scope)
        }
    }

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
    }

    @Deprecated("Don't use it directly")
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