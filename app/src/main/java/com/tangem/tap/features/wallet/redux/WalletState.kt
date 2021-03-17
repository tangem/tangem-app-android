package com.tangem.tap.features.wallet.redux

import android.graphics.Bitmap
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.extensions.isAboveZero
import com.tangem.tap.common.entities.Button
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.extensions.toSendableAmounts
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.models.toPendingTransactions
import com.tangem.tap.features.wallet.models.toPendingTransactionsForToken
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.store
import org.rekotlin.StateType
import java.math.BigDecimal

data class WalletState(
        val state: ProgressState = ProgressState.Done,
        val error: ErrorType? = null,
        val cardImage: Artwork? = null,
        val hashesCountVerified: Boolean? = null,
        val walletDialog: WalletDialog? = null,
        val twinCardsState: TwinCardsState? = null,
        val mainWarningsList: List<WarningMessage> = mutableListOf(),
        val wallets: List<WalletData> = emptyList(),
        val walletManagers: List<WalletManager> = emptyList(),
        val isMultiwalletAllowed: Boolean = false,
        val cardCurrency: CryptoCurrencyName? = null,
        val selectedWallet: CryptoCurrencyName? = null,
        val primaryBlockchain: Blockchain? = null,
        val primaryToken: Token? = null
) : StateType {

    val primaryWallet = if (wallets.isNotEmpty()) wallets[0] else null
//    val primaryWalletManager = if (walletManagers.isNotEmpty()) walletManagers[0] else null

    val shouldShowDetails: Boolean =
            primaryWallet?.currencyData?.status != com.tangem.tap.features.wallet.ui.BalanceStatus.EmptyCard &&
                    primaryWallet?.currencyData?.status != com.tangem.tap.features.wallet.ui.BalanceStatus.UnknownBlockchain

    fun getWalletManager(currencyName: CryptoCurrencyName?): WalletManager? {
        if (currencyName == null) return null
        val walletManager = walletManagers.find { it.wallet.blockchain.currency == currencyName }
        if (walletManager != null) return walletManager

        val ethereumWalletManager = walletManagers.find { it.wallet.blockchain == Blockchain.Ethereum }
        return if (ethereumWalletManager?.presetTokens?.find { it.symbol == currencyName } != null) {
            ethereumWalletManager
        } else {
            null
        }
    }

    fun getWalletData(currencyName: CryptoCurrencyName?): WalletData? {
        if (currencyName == null) return null
        return wallets.find { it.currencyData.currencySymbol == currencyName }
    }

    fun getSelectedWalletData(): WalletData? {
        return wallets.find { it.currencyData.currencySymbol == selectedWallet }
    }

    fun canBeRemoved(walletData: WalletData?): Boolean {
        if (walletData == null) return false

        if (walletData.blockchain != store.state.walletState.primaryBlockchain
                && (walletData.token == null || walletData.token != store.state.walletState.primaryToken)) {
            val walletManager = getWalletManager(walletData.currencyData.currencySymbol)
                    ?: return true
            val wallet = walletManager.wallet
            if (walletData.blockchain != null) {
                return wallet.recentTransactions.toPendingTransactions(wallet.address).isEmpty() &&
                        wallet.amounts.toSendableAmounts().isEmpty()
            } else if (walletData.token != null) (
                    return wallet.recentTransactions.toPendingTransactionsForToken(
                            walletData.token, wallet.address).isEmpty()
                            && wallet.amounts[AmountType.Token(token = walletData.token)]?.isAboveZero() != true
                    )
        }
        return false
    }

    fun replaceWalletInWallets(walletData: WalletData?): List<WalletData> {
        if (walletData == null) return wallets
        return wallets.filter { it.currencyData.currency != walletData.currencyData.currency } + walletData
    }

    fun replaceSomeWallets(newWallets: List<WalletData>): List<WalletData> {
        val currencies = newWallets.map { it.currencyData.currency }
        return wallets.filter { !currencies.contains(it.currencyData.currency) } + newWallets
    }

}

sealed class WalletDialog {
    data class QrDialog(
            val qrCode: Bitmap?, val shareUrl: String?, val currencyName: CryptoCurrencyName?
    ) : WalletDialog()

    data class SelectAmountToSendDialog(val amounts: List<Amount>?) : WalletDialog()
}

enum class ProgressState { Loading, Done, Error }

enum class ErrorType { NoInternetConnection }

sealed class WalletMainButton(enabled: Boolean) : Button(enabled) {
    class SendButton(enabled: Boolean) : WalletMainButton(enabled)
    class CreateWalletButton(enabled: Boolean) : WalletMainButton(enabled)
}

data class WalletAddresses(
        val selectedAddress: AddressData,
        val list: List<AddressData>
)

data class AddressData(
        val address: String,
        val type: AddressType,
        val shareUrl: String,
        val exploreUrl: String,
        val qrCode: Bitmap? = null
)

data class Artwork(
        val artworkId: String,
        val artwork: Bitmap? = null
) {
    companion object {
        const val DEFAULT_IMG_URL = "https://app.tangem.com/cards/card_default.png"
        const val SERGIO_CARD_URL = "https://app.tangem.com/cards/card_tg059.png"
        const val MARTA_CARD_URL = "https://app.tangem.com/cards/card_tg083.png"
        const val SERGIO_CARD_ID = "BC01"
        const val MARTA_CARD_ID = "BC02"
        const val TWIN_CARD_1 = "https://app.tangem.com/cards/card_tg085.png"
        const val TWIN_CARD_2 = "https://app.tangem.com/cards/card_tg086.png"
    }
}

data class TopUpState(
        val allowed: Boolean = true,
        val url: String? = null,
        val redirectUrl: String? = null
)

data class TwinCardsState(
        val secondCardId: String?,
        val cardNumber: TwinCardNumber?,
        val showTwinOnboarding: Boolean,
        val isCreatingTwinCardsAllowed: Boolean
)

data class WalletData(
        val pendingTransactions: List<PendingTransaction> = emptyList(),
        val hashesCountVerified: Boolean? = null,
        val walletAddresses: WalletAddresses? = null,
        val currencyData: BalanceWidgetData = BalanceWidgetData(),
        val updatingWallet: Boolean = false,
        val topUpState: TopUpState = TopUpState(),
        val allowToSend: Boolean = true,
        val fiatRateString: String? = null,
        val fiatRate: BigDecimal? = null,
        val mainButton: WalletMainButton = WalletMainButton.SendButton(false),
        val token: Token? = null,
        val blockchain: Blockchain? = null,
) {
    fun shouldShowMultipleAddress(): Boolean {
        val listOfAddresses = walletAddresses?.list ?: return false
        return (currencyData.currencySymbol == Blockchain.Bitcoin.currency ||
                currencyData.currencySymbol == Blockchain.BitcoinTestnet.currency ||
                currencyData.currencySymbol == Blockchain.CardanoShelley.currency) &&
                listOfAddresses.size > 1
    }
}