package com.tangem.tap.features.wallet.redux

import android.graphics.Bitmap
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.address.AddressType
import com.tangem.tap.common.entities.Button
import com.tangem.tap.common.extensions.toQrCode
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.common.toggleWidget.WidgetState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsState
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.store
import org.rekotlin.StateType
import kotlin.properties.ReadOnlyProperty

data class WalletState(
    val userWallet: UserWallet? = null,
    val state: ProgressState = ProgressState.Done,
    val error: ErrorType? = null,
    val cardImage: Artwork? = null,
    val hashesCountVerified: Boolean? = null,
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
}

enum class ProgressState : WidgetState { Loading, Refreshing, Done, Error }

enum class ErrorType {
    NoInternetConnection,
    UnknownBlockchain,
}

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
        const val SALT_PAY_URL = "key_for_switch_url_to_drawableId_of_salt_pay_card"
    }
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