package com.tangem.tap.features.wallet.redux

import android.graphics.Bitmap
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.entities.Button
import com.tangem.tap.common.extensions.toQrCode
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.common.toggleWidget.WidgetState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsState
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.models.TotalBalance
import com.tangem.tap.features.wallet.models.WalletRent
import com.tangem.tap.features.wallet.models.WalletWarning
import com.tangem.tap.features.wallet.redux.reducers.calculateTotalFiatAmount
import com.tangem.tap.features.wallet.redux.reducers.findProgressState
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManager
import org.rekotlin.StateType
import java.math.BigDecimal
import kotlin.properties.ReadOnlyProperty

data class WalletState(
    val cardId: String = "",
    val state: ProgressState = ProgressState.Done,
    val error: ErrorType? = null,
    val cardImage: Artwork? = null,
    val hashesCountVerified: Boolean? = null,
    val mainWarningsList: List<WarningMessage> = mutableListOf(),
    val walletsStores: List<WalletStore> = listOf(),
    val isMultiwalletAllowed: Boolean = false,
    val cardCurrency: CryptoCurrencyName? = null,
    val selectedCurrency: Currency? = null,
    val primaryBlockchain: Blockchain? = null,
    val primaryToken: Token? = null,
    val isTestnet: Boolean = false,
    val totalBalance: TotalBalance? = null,
    val showBackupWarning: Boolean = false,
    val missingDerivations: List<BlockchainNetwork> = emptyList(),
    val loadingUserTokens: Boolean = false,
    val walletCardsCount: Int? = null,
) : StateType {

    val walletsDataFromStores: List<WalletData>
        get() = walletsStores.map { it.walletsData }.flatten()

    val selectedWalletData: WalletData?
        get() = walletsDataFromStores.firstOrNull { it.currency == selectedCurrency }

    // if you do not delegate - the application crashes on startup,
    // because twinCardsState has not been created yet
    val twinCardsState: TwinCardsState by ReadOnlyProperty<Any, TwinCardsState> { _, _ ->
        store.state.twinCardsState
    }

    val isTangemTwins: Boolean
        get() = store.state.globalState.scanResponse?.cardTypesResolver?.isTangemTwins() == true

    val isExchangeServiceFeatureOn: Boolean
        get() = store.state.globalState.exchangeManager.featureIsSwitchedOn()

    val blockchains: List<Blockchain>
        get() = walletsStores.mapNotNull { it.walletManager?.wallet?.blockchain }

    val currencies: List<Currency>
        get() = walletsStores.flatMap { it.walletsData }.map { it.currency }

    val walletManagers: List<WalletManager>
        get() = walletsStores.mapNotNull { it.walletManager }

    val primaryWallet: WalletData? = walletsStores.firstOrNull()?.walletsData?.firstOrNull()

    val primaryWalletManager: WalletManager? = if (walletsStores.isNotEmpty()) walletsStores[0].walletManager else null

    val shouldShowDetails: Boolean =
        primaryWallet?.currencyData?.status != BalanceStatus.EmptyCard &&
            primaryWallet?.currencyData?.status != BalanceStatus.UnknownBlockchain

    val hasSavedWallets: Boolean
        get() = userWalletsListManager.hasSavedUserWallets

    fun getWalletManager(currency: Currency?): WalletManager? {
        if (currency?.blockchain == null) return null
        return getWalletStore(currency)?.walletManager
    }

    fun getWalletManager(blockchain: BlockchainNetwork): WalletManager? {
        return walletsStores.find { it.blockchainNetwork == blockchain }?.walletManager
    }

    fun getWalletData(blockchain: BlockchainNetwork?): WalletData? {
        if (blockchain == null) return null
        return walletsDataFromStores.find {
            it.currency is Currency.Blockchain &&
                it.currency.blockchain == blockchain.blockchain &&
                it.currency.derivationPath == blockchain.derivationPath
        }
    }

    fun getWalletStore(currency: Currency?): WalletStore? {
        if (currency == null) return null
        return walletsStores.firstOrNull {
            it.blockchainNetwork.derivationPath == currency.derivationPath &&
                it.blockchainNetwork.blockchain == currency.blockchain
        }
    }

    fun getWalletStore(wallet: Wallet?): WalletStore? {
        if (wallet == null) return null
        val currency =
            Currency.Blockchain(wallet.blockchain, wallet.publicKey.derivationPath?.rawPath)
        return getWalletStore(currency)
    }

    fun getWalletStore(blockchainNetwork: BlockchainNetwork?): WalletStore? {
        if (blockchainNetwork == null) return null
        return walletsStores.firstOrNull {
            it.blockchainNetwork.derivationPath == blockchainNetwork.derivationPath &&
                it.blockchainNetwork.blockchain == blockchainNetwork.blockchain
        }
    }

    fun getWalletData(currency: Currency?): WalletData? {
        if (currency == null) return null
        return getWalletStore(currency)?.walletsData?.firstOrNull { it.currency == currency }
    }

    fun replaceWalletStoreInWalletsStores(wallet: WalletStore?): List<WalletStore> {
        if (wallet == null) return walletsStores
        var changed = false
        val updatedWallets = walletsStores.map {
            if (it.blockchainNetwork == wallet.blockchainNetwork) {
                changed = true
                wallet
            } else {
                it
            }
        }
        return if (changed) updatedWallets else walletsStores + wallet
    }

    fun updateWalletData(walletData: WalletData?): WalletState {
        if (walletData == null) return this
        return updateWalletsData(listOf(walletData))
    }

    fun updateWalletsData(walletsData: List<WalletData>): WalletState {
        val walletStores = walletsData
            .map { BlockchainNetwork(it.currency.blockchain, it.currency.derivationPath, emptyList()) }
            .distinct().map { getWalletStore(it) }.mapNotNull { it?.updateWallets(walletsData) }

        return updateWalletsStores(walletStores)
    }

    fun updateWalletStore(walletStore: WalletStore?): WalletState {
        return copy(walletsStores = replaceWalletStoreInWalletsStores(walletStore))
            .updateTotalBalance()
            .updateProgressState()
    }

    private fun updateWalletsStores(walletStores: List<WalletStore>): WalletState {
        val walletStoresMutable = walletStores.toMutableList()
        val updatedWallets = walletsStores.map { oldWalletStore ->
            val walletStore = walletStoresMutable.find {
                it.blockchainNetwork == oldWalletStore.blockchainNetwork
            }
            if (walletStore != null) {
                walletStoresMutable.remove(walletStore)
                walletStore
            } else {
                oldWalletStore
            }
        }
        return copy(walletsStores = updatedWallets + walletStoresMutable)
            .updateTotalBalance()
            .updateProgressState()
    }

    fun removeWalletData(walletData: WalletData?): WalletState {
        if (walletData == null) return this
        return when (val currency = walletData.currency) {
            is Currency.Blockchain -> {
                val walletStores = walletsStores.filterNot {
                    it.blockchainNetwork.blockchain == currency.blockchain &&
                        it.blockchainNetwork.derivationPath == currency.derivationPath
                }
                copy(walletsStores = walletStores)
                    .updateTotalBalance()
                    .updateProgressState()
            }
            is Currency.Token -> {
                val walletStore = getWalletStore(walletData.currency)
                val walletDataList = walletStore?.walletsData
                    ?.filterNot { it.currency == walletData.currency }
                    ?: emptyList()
                val updatedWalletManager = walletStore?.walletManager?.also { it.removeToken(currency.token) }
                val updatedWalletStore = walletStore?.copy(
                    walletsData = walletDataList,
                    walletManager = updatedWalletManager,
                )
                updateWalletStore(updatedWalletStore)
            }
        }
    }

    private fun updateTotalBalance(): WalletState {
        val walletsData = this.walletsStores
            .flatMap(WalletStore::walletsData)

        return if (walletsData.isNotEmpty()) {
            this.copy(
                totalBalance = TotalBalance(
                    state = walletsData.findProgressState(),
                    fiatAmount = walletsData.calculateTotalFiatAmount(),
                    fiatCurrency = store.state.globalState.appCurrency,
                ),
            )
        } else {
            this.copy(totalBalance = null)
        }
    }

    private fun updateProgressState(): WalletState {
        val walletsData = this.walletsStores
            .flatMap(WalletStore::walletsData)

        return if (walletsData.isNotEmpty()) {
            val newProgressState = walletsData.findProgressState()

            this.copy(
                state = walletsData.findProgressState(),
                error = this.error.takeIf { newProgressState == ProgressState.Error },
            )
        } else this
    }

    companion object {
        const val UNKNOWN_AMOUNT_SIGN = "—"
        const val ROUGH_SIGN = "≈"
        const val CAN_BE_LOWER_SIGN = "<"
    }
}

fun List<WalletData>.replaceSomeWalletsData(newWallets: List<WalletData>): List<WalletData> {
    val remainingWallets: MutableList<WalletData> = newWallets.toMutableList()
    val updatedWallets = this.map { wallet ->
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

enum class ProgressState : WidgetState { Loading, Refreshing, Done, Error }

enum class ErrorType { NoInternetConnection }

sealed class WalletMainButton(enabled: Boolean) : Button(enabled) {
    class SendButton(enabled: Boolean) : WalletMainButton(enabled)
    class CreateWalletButton(enabled: Boolean) : WalletMainButton(enabled)
}

data class WalletAddresses(
    val selectedAddress: AddressData,
    val list: List<AddressData>,
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
    val artwork: Bitmap? = null,
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

data class WalletData(
    val pendingTransactions: List<PendingTransaction> = emptyList(),
    val hashesCountVerified: Boolean? = null,
    val walletAddresses: WalletAddresses? = null,
    val currencyData: BalanceWidgetData = BalanceWidgetData(),
    val updatingWallet: Boolean = false,
    val fiatRateString: String? = null,
    val fiatRate: BigDecimal? = null,
    val mainButton: WalletMainButton = WalletMainButton.SendButton(false),
    val currency: Currency,
    val walletRent: WalletRent? = null,
    val existentialDepositString: String? = null,
) {
    val isAvailableToBuy: Boolean
        get() = store.state.globalState.exchangeManager.availableForBuy(currency)

    val isAvailableToSell: Boolean
        get() = store.state.globalState.exchangeManager.availableForSell(currency)

    val isAvailableToSwap: Boolean
        get() = currency.blockchain.isEvm() && currency.coinId != null

    fun shouldShowMultipleAddress(): Boolean {
        val listOfAddresses = walletAddresses?.list ?: return false
        return listOfAddresses.size > 1
    }

    fun shouldEnableTokenSendButton(): Boolean = if (blockchainAmountIsEmpty()) {
        false
    } else {
        !tokenAmountIsEmpty()
    }

    fun assembleWarnings(): List<WalletWarning> {
        val walletWarnings = mutableListOf<WalletWarning>()
        assembleNonTypedWarnings(walletWarnings)
        assembleBlockchainWarnings(walletWarnings)
        assembleTokenWarnings(walletWarnings)

        return walletWarnings.sortedBy { it.showingPosition }
    }

    private fun assembleNonTypedWarnings(walletWarnings: MutableList<WalletWarning>) {
        if (currencyData.status == BalanceStatus.SameCurrencyTransactionInProgress) {
            walletWarnings.add(WalletWarning.TransactionInProgress(currency.currencyName))
        }
    }

    private fun assembleBlockchainWarnings(walletWarnings: MutableList<WalletWarning>) {
        if (!currency.isBlockchain()) return

        if (existentialDepositString != null) {
            val warning = WalletWarning.ExistentialDeposit(
                currencyName = currency.currencyName,
                edStringValueWithSymbol = "$existentialDepositString ${currency.currencySymbol}",
            )
            walletWarnings.add(warning)
        }
        if (walletRent != null) {
            walletWarnings.add(WalletWarning.Rent(walletRent))
        }
    }

    private fun assembleTokenWarnings(walletWarnings: MutableList<WalletWarning>) {
        if (!currency.isToken()) return

        val blockchainFullName = currency.blockchain.fullName
        if (blockchainAmountIsEmpty() && !tokenAmountIsEmpty()) {
            walletWarnings.add(WalletWarning.BalanceNotEnoughForFee(blockchainFullName))
        }
    }

    private fun blockchainAmountIsEmpty(): Boolean = currencyData.blockchainAmount?.isZero() == true

    private fun tokenAmountIsEmpty(): Boolean = currencyData.amount?.isZero() == true
}

data class WalletStore(
    val walletManager: WalletManager?,
    val blockchainNetwork: BlockchainNetwork,
    val walletsData: List<WalletData>,
) {
    fun updateWallets(walletDataList: List<WalletData>): WalletStore {
        val relevantWalletDataList = walletDataList.filter {
            it.currency.blockchain == blockchainNetwork.blockchain &&
                it.currency.derivationPath == blockchainNetwork.derivationPath
        }.toMutableList()
        val updatedWalletDataList = walletsData.map { walletData ->
            val matchingWalletData = relevantWalletDataList.find { it.currency == walletData.currency }
            if (matchingWalletData != null) relevantWalletDataList.remove(matchingWalletData)
            matchingWalletData ?: walletData
        }
        return copy(walletsData = updatedWalletDataList + relevantWalletDataList)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WalletStore

        if (walletManager != other.walletManager) return false
        if (blockchainNetwork != other.blockchainNetwork) return false

        return true
    }

    override fun hashCode(): Int {
        var result = walletManager?.hashCode() ?: 0
        result = 31 * result + blockchainNetwork.hashCode()
        return result
    }
}
