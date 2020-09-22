package com.tangem.tap.features.wallet.redux

import android.graphics.Bitmap
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Wallet
import com.tangem.tap.common.entities.Button
import com.tangem.tap.common.extensions.toBitmap
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import org.rekotlin.StateType

data class WalletState(
        val state: ProgressState = ProgressState.Done,
        val error: ErrorType? = null,
        val cardImage: Artwork? = null,
        val wallet: Wallet? = null,
        val pendingTransactions: List<PendingTransaction> = emptyList(),
        val addressData: AddressData? = null,
        val currencyData: BalanceWidgetData = BalanceWidgetData(),
        val payIdData: PayIdData = PayIdData(),
        val walletDialog: WalletDialog? = null,
        val mainButton: WalletMainButton = WalletMainButton.SendButton(false)
) : StateType {
    val showDetails: Boolean =
            currencyData.status != com.tangem.tap.features.wallet.ui.BalanceStatus.EmptyCard &&
                    currencyData.status != com.tangem.tap.features.wallet.ui.BalanceStatus.UnknownBlockchain
}

sealed class WalletDialog {
    data class QrDialog(
            val qrCode: Bitmap?, val shareUrl: String?, val currencyName: CryptoCurrencyName?
    ) : WalletDialog()

    data class CreatePayIdDialog(val creatingPayIdState: CreatingPayIdState?) : WalletDialog()
    data class SelectAmountToSendDialog(val amounts: List<Amount>?) : WalletDialog()
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

data class AddressData(
        val address: String,
        val shareUrl: String,
        val exploreUrl: String
)

data class Artwork(
        val artworkId: String? = null,
        val artwork: Bitmap? = null,
        val artworkResId: Int? = null
) {
    constructor(artworkId: String, artworkBytes: ByteArray) : this(artworkId, artworkBytes.toBitmap())
}