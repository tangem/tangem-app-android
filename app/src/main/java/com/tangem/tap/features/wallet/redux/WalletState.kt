package com.tangem.tap.features.wallet.redux

import android.graphics.Bitmap
import com.tangem.blockchain.common.Wallet
import com.tangem.tap.common.entities.Button
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import org.rekotlin.StateType

data class WalletState(
        val state: ProgressState = ProgressState.Done,
        val cardImage: Artwork? = null,
        val wallet: Wallet? = null,
        val pendingTransactions: List<PendingTransaction> = emptyList(),
        val addressData: AddressData? = null,
        val currencyData: BalanceWidgetData = BalanceWidgetData(),
        val payIdData: PayIdData = PayIdData(),
        val qrCode: Bitmap? = null,
        val creatingPayIdState: CreatingPayIdState? = null,
        val mainButton: WalletMainButton = WalletMainButton.SendButton(false)
) : StateType


enum class ProgressState { Loading, Done, Error }

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
        val artworkId: String,
        val artwork: Bitmap
)