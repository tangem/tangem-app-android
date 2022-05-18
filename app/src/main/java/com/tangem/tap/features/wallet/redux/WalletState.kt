package com.tangem.tap.features.wallet.redux

import android.graphics.Bitmap
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.extensions.isAboveZero
import com.tangem.common.extensions.isZero
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.tap.common.entities.Button
import com.tangem.tap.common.extensions.toQrCode
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.common.toggleWidget.WidgetState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.extensions.buyIsAllowed
import com.tangem.tap.domain.extensions.sellIsAllowed
import com.tangem.tap.domain.extensions.toSendableAmounts
import com.tangem.tap.domain.tokens.BlockchainNetwork
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsState
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.models.toPendingTransactions
import com.tangem.tap.features.wallet.models.toPendingTransactionsForToken
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
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
    val wallets: List<WalletStore> = listOf(),
    val isMultiwalletAllowed: Boolean = false,
    val cardCurrency: CryptoCurrencyName? = null,
    val selectedCurrency: Currency? = null,
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

    val primaryWallet: WalletData? = if (wallets.isNotEmpty()) wallets[0].walletsData[0] else null
    val primaryWalletManager: WalletManager? =
        if (wallets.isNotEmpty()) wallets[0].walletManager else null

    val shouldShowDetails: Boolean =
        primaryWallet?.currencyData?.status != BalanceStatus.EmptyCard &&
            primaryWallet?.currencyData?.status != BalanceStatus.UnknownBlockchain

    val blockchains: List<Blockchain>
        get() = wallets.mapNotNull { it.walletManager?.wallet?.blockchain }

    val currencies: List<Currency>
        get() = wallets.flatMap { it.walletsData }.map { it.currency }

    val walletsData: List<WalletData>
        get() = wallets.flatMap { it.walletsData }

    val walletManagers: List<WalletManager>
        get() = wallets.mapNotNull { it.walletManager }

    fun getWalletManager(currency: Currency?): WalletManager? {
        if (currency?.blockchain == null) return null
        return getWalletStore(currency)?.walletManager
    }

    fun getWalletManager(blockchain: BlockchainNetwork): WalletManager? {
        return wallets.find { it.blockchainNetwork == blockchain }?.walletManager
    }

    fun getWalletData(blockchain: BlockchainNetwork?): WalletData? {
        if (blockchain == null) return null
        return walletsData.find {
            it.currency is Currency.Blockchain &&
                it.currency.blockchain == blockchain.blockchain &&
                it.currency.derivationPath == blockchain.derivationPath
        }
    }

    fun getWalletStore(currency: Currency?): WalletStore? {
        if (currency == null) return null
        return wallets.firstOrNull {
            it.blockchainNetwork.derivationPath == currency.derivationPath &&
                (it.blockchainNetwork.blockchain == currency.blockchain)
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
        return wallets.firstOrNull {
            it.blockchainNetwork.derivationPath == blockchainNetwork.derivationPath &&
                (it.blockchainNetwork.blockchain == blockchainNetwork.blockchain)
        }
    }

    fun getWalletData(currency: Currency?): WalletData? {
        if (currency == null) return null
        return getWalletStore(currency)?.walletsData?.firstOrNull { it.currency == currency }
    }

    fun getSelectedWalletData(): WalletData? {
        return walletsData.find { it.currency == selectedCurrency }
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
                    walletData.currency.token, wallet.address
                ).isEmpty()
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

    fun replaceWalletInWallets(wallet: WalletStore?): List<WalletStore> {
        if (wallet == null) return wallets
        var changed = false
        val updatedWallets = wallets.map {
            if (it.blockchainNetwork == wallet.blockchainNetwork) {
                changed = true
                wallet
            } else {
                it
            }
        }
        return if (changed) updatedWallets else wallets + wallet
    }

    fun updateWalletData(walletData: WalletData?): WalletState {
        if (walletData == null) return this
        return updateWalletsData(listOf(walletData))
    }

    fun updateWalletsData(
        walletsData: List<WalletData>
    ): WalletState {

        val walletStores = walletsData
            .map { BlockchainNetwork(it.currency.blockchain, it.currency.derivationPath, emptyList()) }
            .distinct().map { getWalletStore(it) }.mapNotNull { it?.updateWallets(walletsData) }

        return updateWalletStores(walletStores)

    }

    fun updateWalletStore(walletStore: WalletStore?): WalletState {
        return copy(wallets = replaceWalletInWallets(walletStore))
    }

    fun updateWalletStores(walletStores: List<WalletStore>): WalletState {
        val walletStores = walletStores.toMutableList()
        val updatedWallets = wallets.map { oldWalletStore ->
            val walletStore = walletStores.find { it.blockchainNetwork == oldWalletStore.blockchainNetwork }
            if (walletStore != null) {
                walletStores.remove(walletStore)
                walletStore
            } else {
                oldWalletStore
            }
        }
        return copy(wallets = updatedWallets + walletStores)
    }

    fun removeWallet(walletData: WalletData?): WalletState {
        if (walletData == null) return this
        return if (walletData.currency is Currency.Blockchain) {
            val walletStores = wallets.filterNot {
                it.blockchainNetwork.blockchain == walletData.currency.blockchain
                    && it.blockchainNetwork.derivationPath == walletData.currency.derivationPath
            }
            copy(wallets = walletStores)
        } else {
            val walletStore = getWalletStore(walletData.currency)
            val walletDataList = walletStore?.walletsData
                ?.filterNot { it.currency == walletData.currency }
                ?: emptyList()
            val updatedWalletStore = walletStore?.copy(walletsData = walletDataList)
            updateWalletStore(updatedWalletStore)
        }
    }

    fun replaceSomeWallets(newWallets: List<WalletData>): List<WalletData> {
        val remainingWallets: MutableList<WalletData> = newWallets.toMutableList()
        val updatedWallets = walletsData.map { wallet ->
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

    fun updateTradeCryptoState(
        exchangeManager: CurrencyExchangeManager?,
        walletData: WalletData
    ): WalletData {
        return walletData.copy(
            tradeCryptoState = TradeCryptoState.from(
                exchangeManager,
                walletData
            )
        )
    }

    fun updateTradeCryptoState(
        exchangeManager: CurrencyExchangeManager?,
        walletDataList: List<WalletData>
    ): List<WalletData> {
        return walletDataList.map {
            it.copy(
                tradeCryptoState = TradeCryptoState.from(
                    exchangeManager,
                    it
                )
            )
        }
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

        const val TEMP_CARDANO =
            "https://verify.tangem.com/card/artwork?artworkId=card_ru039&CID=CB19000000040976&publicKey=0416E29423A6CC77CD07CBA52873E8F6F894B1AFB18EB3688ACC2C8D8E5AC84B80B0BA1B17B85E578E47044CE96BCFF3FB4499FA4941CAD3C1EF300A492B5B9659"
    }
}

data class TradeCryptoState(
    val sellingAllowed: Boolean = false,
    val buyingAllowed: Boolean = false,
) {
    companion object {
        fun from(
            exchangeManager: CurrencyExchangeManager?,
            walletData: WalletData
        ): TradeCryptoState {
            val status = exchangeManager ?: return walletData.tradeCryptoState
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
    val fiatRateString: String? = null,
    val fiatRate: BigDecimal? = null,
    val mainButton: WalletMainButton = WalletMainButton.SendButton(false),
    val currency: Currency,
    val walletRent: WalletRent? = null,
) {
    fun shouldShowMultipleAddress(): Boolean {
        val listOfAddresses = walletAddresses?.list ?: return false
        return listOfAddresses.size > 1
    }

    fun shouldShowCoinAmountWarning(): Boolean = when (currency) {
        is Currency.Blockchain -> false
        is Currency.Token -> blockchainAmountIsEmpty() && !tokenAmountIsEmpty()
    }

    fun shouldEnableTokenSendButton(): Boolean = !blockchainAmountIsEmpty() || !tokenAmountIsEmpty()

    private fun blockchainAmountIsEmpty(): Boolean =
        currencyData.blockchainAmount?.isZero() ?: false

    private fun tokenAmountIsEmpty(): Boolean = currencyData.amount?.isZero() == true
}

data class WalletRent(
    val minRentValue: String,
    val rentExemptValue: String
)

sealed interface Currency {

    val coinId: String?
        get() = when (this) {
            is Blockchain -> blockchain.toCoinId()
            is Token -> token.id
        }
    val blockchain: com.tangem.blockchain.common.Blockchain
    val currencySymbol: CryptoCurrencyName
    val derivationPath: String?

    data class Token(
        val token: com.tangem.blockchain.common.Token,
        override val blockchain: com.tangem.blockchain.common.Blockchain,
        override val derivationPath: String?
    ) : Currency {
        override val currencySymbol = token.symbol
    }

    data class Blockchain(
        override val blockchain: com.tangem.blockchain.common.Blockchain,
        override val derivationPath: String?
    ) : Currency {
        override val currencySymbol: CryptoCurrencyName = blockchain.currency
    }

    fun isCustomCurrency(derivationStyle: DerivationStyle?): Boolean {
        if (this is Token && this.token.id == null) return true

        if (derivationPath == null || derivationStyle == null) return false

        return derivationPath != blockchain.derivationPath(derivationStyle)?.rawPath
    }

    fun isBlockchain(): Boolean = this is Blockchain

    fun isToken(): Boolean = this is Token

    companion object {
        fun fromBlockchainNetwork(
            blockchainNetwork: BlockchainNetwork,
            token: com.tangem.blockchain.common.Token? = null
        ): Currency {
            return if (token != null) {
                Token(
                    token = token,
                    blockchain = blockchainNetwork.blockchain,
                    derivationPath = blockchainNetwork.derivationPath
                )
            } else {
                Blockchain(
                    blockchain = blockchainNetwork.blockchain,
                    derivationPath = blockchainNetwork.derivationPath
                )
            }
        }

        fun fromCustomCurrency(customCurrency: CustomCurrency): Currency {
            return when (customCurrency) {
                is CustomCurrency.CustomBlockchain -> Blockchain(
                    blockchain = customCurrency.network,
                    derivationPath = customCurrency.derivationPath?.rawPath
                )
                is CustomCurrency.CustomToken -> Token(
                    token = customCurrency.token,
                    blockchain = customCurrency.network,
                    derivationPath = customCurrency.derivationPath?.rawPath,
                )
            }
        }

        fun fromTokenWithBlockchain(tokenWithBlockchain: TokenWithBlockchain): Token {
            return Token(
                token = tokenWithBlockchain.token,
                blockchain = tokenWithBlockchain.blockchain,
                derivationPath = null
            )
        }
    }
}

data class WalletStore(
    val walletManager: WalletManager?,
    val blockchainNetwork: BlockchainNetwork,
    val walletsData: List<WalletData>
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