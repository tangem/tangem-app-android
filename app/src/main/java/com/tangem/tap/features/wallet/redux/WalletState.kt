package com.tangem.tap.features.wallet.redux

import android.graphics.Bitmap
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.extensions.isAboveZero
import com.tangem.tap.common.entities.Button
import com.tangem.tap.common.extensions.toQrCode
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.common.toggleWidget.WidgetState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.extensions.buyIsAllowed
import com.tangem.tap.domain.extensions.sellIsAllowed
import com.tangem.tap.domain.extensions.toSendableAmounts
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsState
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.models.toPendingTransactions
import com.tangem.tap.features.wallet.models.toPendingTransactionsForToken
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.network.moonpay.MoonpayStatus
import com.tangem.tap.store
import org.rekotlin.StateType
import java.math.BigDecimal
import kotlin.properties.ReadOnlyProperty

data class WalletState(
    val state: ProgressState = ProgressState.Done,
    val error: ErrorType? = null,
    val cardImage: Artwork? = null,
    val hashesCountVerified: Boolean? = null,
    val walletDialog: StateDialog? = null,
    val mainWarningsList: List<WarningMessage> = mutableListOf(),
    val wallets: List<WalletData> = emptyList(),
    val walletManagers: List<WalletManager> = emptyList(),
    val isMultiwalletAllowed: Boolean = false,
    val cardCurrency: CryptoCurrencyName? = null,
    val selectedWallet: Currency? = null,
    val primaryBlockchain: Blockchain? = null,
    val primaryToken: Token? = null,
    val isTestnet: Boolean = false,
) : StateType {

    // if you do not delegate - the application crashes on startup,
    // because twinCardsState has not been created yet
    val twinCardsState: TwinCardsState by ReadOnlyProperty<Any, TwinCardsState> { thisRef, property ->
        store.state.twinCardsState
    }

    val isTangemTwins: Boolean
        get() = store.state.globalState.scanResponse?.isTangemTwins() == true

    val primaryWallet = if (wallets.isNotEmpty()) wallets[0] else null

    val shouldShowDetails: Boolean =
            primaryWallet?.currencyData?.status != BalanceStatus.EmptyCard &&
                    primaryWallet?.currencyData?.status != BalanceStatus.UnknownBlockchain

    val blockchains: List<Blockchain>
        get() = walletManagers.map { it.wallet.blockchain }

    val currencies: List<Currency>
        get() = wallets.mapNotNull { it.currency }

    fun getWalletManager(token: Token?): WalletManager? {
        if (token == null) return null
        return walletManagers.find { it.wallet.blockchain == token.blockchain }
    }

    fun getWalletManager(currency: Currency?): WalletManager? {
        if (currency?.blockchain == null) return null
        return walletManagers.find { it.wallet.blockchain == currency.blockchain }
    }

    fun getWalletManager(blockchain: Blockchain): WalletManager? {
        return walletManagers.find { it.wallet.blockchain == blockchain }
    }

    fun getWalletData(currency: Currency?): WalletData? {
        if (currency == null) return null
        return wallets.find { it.currency == currency }
    }

    fun getWalletData(blockchain: Blockchain?): WalletData? {
        if (blockchain == null) return null
        return wallets.find { (it.currency as? Currency.Blockchain)?.blockchain == blockchain }
    }

    fun getWalletData(token: Token?): WalletData? {
        if (token == null) return null
        return wallets.find { (it.currency as? Currency.Token)?.token == token }
    }

    fun getSelectedWalletData(): WalletData? {
        return wallets.find { it.currency == selectedWallet }
    }

    fun canBeRemoved(walletData: WalletData?): Boolean {
        if (walletData == null) return false

        if (!isPrimaryCurrency(walletData)) {
            val walletManager = getWalletManager(walletData.currency)
                    ?: return true

            if (walletData.currency is Currency.Blockchain &&
                    walletManager.cardTokens.isNotEmpty()
            ) {
                return false
            }

            val wallet = walletManager.wallet

            if (walletData.currency is Currency.Blockchain) {
                return wallet.recentTransactions.toPendingTransactions(wallet.address).isEmpty() &&
                        wallet.amounts.toSendableAmounts().isEmpty()
            } else if (walletData.currency is Currency.Token) (
                    return wallet.recentTransactions.toPendingTransactionsForToken(
                        walletData.currency.token, wallet.address).isEmpty()
                            && wallet.amounts[AmountType.Token(token = walletData.currency.token)]
                            ?.isAboveZero() != true
                    )
        }
        return false
    }

    private fun isPrimaryCurrency(walletData: WalletData): Boolean {
        return (walletData.currency is Currency.Blockchain &&
                walletData.currency.blockchain == store.state.walletState.primaryBlockchain)
                || (walletData.currency is Currency.Token &&
                walletData.currency.token == store.state.walletState.primaryToken)
    }

    fun replaceWalletInWallets(walletData: WalletData?): List<WalletData> {
        if (walletData == null) return wallets
        var changed = false
        val updatedWallets = wallets.map {
            if (it.currency == walletData.currency) {
                changed = true
                walletData
            } else {
                it
            }
        }
        return if (changed) updatedWallets else wallets + walletData
    }

    fun replaceSomeWallets(newWallets: List<WalletData>): List<WalletData> {
        val remainingWallets: MutableList<WalletData> = newWallets.toMutableList()
        val updatedWallets = wallets.map { wallet ->
            val newWallet = newWallets
                    .firstOrNull { wallet.currency == it.currency }
            if (newWallet == null) {
                wallet
            } else {
                remainingWallets.remove(newWallet)
                newWallet
            }
        }
        return updatedWallets + remainingWallets
    }

    fun updateTradeCryptoState(moonpayStatus: MoonpayStatus?, walletData: WalletData): WalletData {
        return walletData.copy(tradeCryptoState = TradeCryptoState.from(moonpayStatus, walletData))
    }

    fun updateTradeCryptoState(moonpayStatus: MoonpayStatus?, walletDataList: List<WalletData>): List<WalletData> {
        return walletDataList.map { it.copy(tradeCryptoState = TradeCryptoState.from(moonpayStatus, it)) }
    }

    fun addWalletManagers(newWalletManagers: List<WalletManager>): WalletState {
        val updatedWalletManagers = this.walletManagers +
                newWalletManagers.filterNot { this.blockchains.contains(it.wallet.blockchain) }
        return copy(walletManagers = updatedWalletManagers)
    }
}

sealed class WalletDialog : StateDialog {
    data class SelectAmountToSendDialog(val amounts: List<Amount>?) : WalletDialog()
    object SignedHashesMultiWalletDialog : WalletDialog()
    object ChooseTradeActionDialog : WalletDialog()
}

enum class ProgressState : WidgetState { Loading, Done, Error }

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
) {
    val qrCode: Bitmap by lazy { shareUrl.toQrCode() }
}

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

        const val TEMP_CARDANO = "https://verify.tangem.com/card/artwork?artworkId=card_ru039&CID=CB19000000040976&publicKey=0416E29423A6CC77CD07CBA52873E8F6F894B1AFB18EB3688ACC2C8D8E5AC84B80B0BA1B17B85E578E47044CE96BCFF3FB4499FA4941CAD3C1EF300A492B5B9659"
    }
}

data class TradeCryptoState(
    val sellingAllowed: Boolean = false,
    val buyingAllowed: Boolean = false,
) {
    companion object {
        fun from(moonpayStatus: MoonpayStatus?, walletData: WalletData): TradeCryptoState {
            val status = moonpayStatus ?: return walletData.tradeCryptoState
            val currency = walletData.currency

            return TradeCryptoState(status.sellIsAllowed(currency), status.buyIsAllowed(currency))
        }
    }
}

data class WalletData(
    val pendingTransactions: List<PendingTransaction> = emptyList(),
    val hashesCountVerified: Boolean? = null,
    val walletAddresses: WalletAddresses? = null,
    val currencyData: BalanceWidgetData = BalanceWidgetData(),
    val updatingWallet: Boolean = false,
    val tradeCryptoState: TradeCryptoState = TradeCryptoState(),
    val allowToSend: Boolean = true,
    val fiatRateString: String? = null,
    val fiatRate: BigDecimal? = null,
    val mainButton: WalletMainButton = WalletMainButton.SendButton(false),
    val currency: Currency
) {
    fun shouldShowMultipleAddress(): Boolean {
        val listOfAddresses = walletAddresses?.list ?: return false
        return listOfAddresses.size > 1
    }
}

sealed interface Currency {

    val blockchain: com.tangem.blockchain.common.Blockchain
    val currencySymbol: CryptoCurrencyName

    data class Token(
        val token: com.tangem.blockchain.common.Token
    ) : Currency {
        override val blockchain = token.blockchain
        override val currencySymbol: CryptoCurrencyName = token.symbol
    }

    data class Blockchain(
        override val blockchain: com.tangem.blockchain.common.Blockchain
    ) : Currency {
        override val currencySymbol: CryptoCurrencyName = blockchain.currency
    }
}