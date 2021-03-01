package com.tangem.tap.features.wallet.redux

import android.graphics.Bitmap
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressType
import com.tangem.tap.common.entities.Button
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import org.rekotlin.StateType

data class WalletState(
        val state: ProgressState = ProgressState.Done,
        val error: ErrorType? = null,
        val cardImage: Artwork? = null,
        val wallet: Wallet? = null,
        val pendingTransactions: List<PendingTransaction> = emptyList(),
        val hashesCountVerified: Boolean? = null,
        val walletAddresses: WalletAddresses? = null,
        val currencyData: BalanceWidgetData = BalanceWidgetData(),
        val payIdData: PayIdData = PayIdData(),
        val walletDialog: WalletDialog? = null,
        val updatingWallet: Boolean = false,
        val mainButton: WalletMainButton = WalletMainButton.SendButton(false),
        val topUpState: TopUpState = TopUpState(),
        val twinCardsState: TwinCardsState? = null,
        val mainWarningsList: List<WarningMessage> = mutableListOf(),
        val scanCardFailsCounter: Int = 0,
) : StateType {
    val showDetails: Boolean =
            currencyData.status != com.tangem.tap.features.wallet.ui.BalanceStatus.EmptyCard &&
                    currencyData.status != com.tangem.tap.features.wallet.ui.BalanceStatus.UnknownBlockchain

    val showMultipleAddress: Boolean
        get() {
            val listOfAddresses = walletAddresses?.list ?: return false
            return (wallet?.blockchain == Blockchain.Bitcoin ||
                    wallet?.blockchain == Blockchain.BitcoinTestnet ||
                    wallet?.blockchain == Blockchain.CardanoShelley) &&
                    listOfAddresses.size > 1
        }
}

sealed class WalletDialog {
    data class QrDialog(
            val qrCode: Bitmap?, val shareUrl: String?, val currencyName: CryptoCurrencyName?
    ) : WalletDialog()

    data class CreatePayIdDialog(val creatingPayIdState: CreatingPayIdState?) : WalletDialog()
    data class SelectAmountToSendDialog(val amounts: List<Amount>?) : WalletDialog()
    data class TwinsOnboardingFragment(val secondCardId: String): WalletDialog()
    object ScanFailsDialog: WalletDialog()
}

enum class ProgressState { Loading, Done, Error }

enum class ErrorType { NoInternetConnection }

enum class PayIdState { Disabled, Loading, NotCreated, Created, ErrorLoading }

data class PayIdData(
        val payIdState: PayIdState = PayIdState.Loading,
        val payId: String? = null
)

enum class CreatingPayIdState { EnterPayId, Waiting }

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
        val exploreUrl: String
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