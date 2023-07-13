package com.tangem.tap.features.wallet.redux

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.userwallets.Artwork
import com.tangem.tap.common.entities.Button
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.common.toggleWidget.WidgetState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsState
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.store
import org.rekotlin.StateType
import java.math.BigDecimal
import kotlin.properties.ReadOnlyProperty

data class WalletState(
    val state: ProgressState = ProgressState.Done,
    val error: ErrorType? = null,
    val cardImage: Artwork? = null,
    val mainWarningsList: List<WarningMessage> = mutableListOf(),
    val walletsStores: List<WalletStoreModel> = listOf(),
    val isMultiwalletAllowed: Boolean = false,
    val cardCurrency: CryptoCurrencyName? = null,
    val selectedCurrency: Currency? = null,
    val isTestnet: Boolean = false,
    val totalBalance: TotalFiatBalance? = null,
    val showBackupWarning: Boolean = false,
    val missingDerivations: List<BlockchainNetwork> = emptyList(),
    val loadingUserTokens: Boolean = false,
    val walletCardsCount: Int? = null,
    val canSaveUserWallets: Boolean = false,
) : StateType {

    val walletsDataFromStores: List<WalletDataModel>
        get() = walletsStores.flatMap { it.walletsData }

    val selectedWalletData: WalletDataModel?
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

    private val primaryWalletStore: WalletStoreModel?
        get() = if (isMultiwalletAllowed || walletsStores.isEmpty() || walletsStores.size > 1) {
            null
        } else {
            walletsStores[0]
        }

    val primaryWalletManager: WalletManager?
        get() = primaryWalletStore?.walletManager

    val primaryWalletData: WalletDataModel?
        get() = primaryWalletStore?.blockchainWalletData

    val primaryTokenData: WalletDataModel?
        get() = primaryWalletStore?.walletsData
            ?.firstOrNull { it.currency !is Currency.Blockchain }

    val shouldShowDetails: Boolean =
        primaryWalletData?.status !is WalletDataModel.Unreachable

    fun getWalletManager(currency: Currency?): WalletManager? {
        if (currency?.blockchain == null) return null
        return getWalletStore(currency)?.walletManager
    }

    fun getWalletManager(blockchain: BlockchainNetwork): WalletManager? {
        return walletsStores.firstOrNull {
            it.blockchain == blockchain.blockchain &&
                it.derivationPath?.rawPath == blockchain.derivationPath
        }?.walletManager
    }

    fun getWalletStore(currency: Currency?): WalletStoreModel? {
        if (currency == null) return null
        return walletsStores.firstOrNull {
            it.blockchain == currency.blockchain &&
                it.derivationPath?.rawPath == currency.derivationPath
        }
    }

    fun getBlockchainAmount(currency: Currency): BigDecimal =
        getWalletManager(currency)?.wallet?.amounts?.get(AmountType.Coin)?.value ?: BigDecimal.ZERO
}

enum class ProgressState : WidgetState { Loading, Refreshing, Done, Error }

enum class ErrorType {
    NoInternetConnection,
    UnknownBlockchain,
}

sealed class WalletMainButton(enabled: Boolean) : Button(enabled) {
    class SendButton(enabled: Boolean) : WalletMainButton(enabled)
}
