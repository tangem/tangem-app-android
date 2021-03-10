package com.tangem.tap.features.wallet.redux

import android.content.Context
import com.tangem.TangemError
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressType
import com.tangem.commands.common.card.Card
import com.tangem.tap.common.redux.ErrorAction
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.wallet.R
import org.rekotlin.Action
import java.math.BigDecimal

sealed class WalletAction : Action {

    object ResetState : WalletAction()

    object LoadData : WalletAction() {
        data class Failure(val error: TapError) : WalletAction()
    }

    data class LoadWallet(
            val wallet: Wallet, val artworkId: String?, val allowTopUp: Boolean,
    ) : WalletAction() {
        data class Success(val wallet: Wallet) : WalletAction()
        data class NoAccount(val amountToCreateAccount: String) : WalletAction()
        data class Failure(val errorMessage: String? = null) : WalletAction()
    }

    object CheckHashesCountOnline : WalletAction()
    object NeedToCheckHashesCountOnline : WalletAction()
    object ConfirmHashesCount : WalletAction()
    object SaveCardId : WalletAction()

    object Warnings : WalletAction() {
        object CheckIfNeeded : WalletAction()
        data class SetWarnings(val warningList: List<WarningMessage>) : WalletAction()

        object AppRating : WalletAction() {
            object SetNeverToShow : WalletAction()
            object RemindLater : WalletAction()
        }
    }

    object UpdateWallet : WalletAction() {
        object ScheduleUpdatingWallet : WalletAction()
        data class Success(val wallet: Wallet) : WalletAction()
        data class Failure(val errorMessage: String? = null) : WalletAction()
    }

    object LoadFiatRate : WalletAction() {
        data class Success(val fiatRates: Pair<CryptoCurrencyName, BigDecimal?>) : WalletAction()
        object Failure : WalletAction()
    }

    object LoadPayId : WalletAction() {
        data class Success(val payId: String) : WalletAction()
        object NotCreated : WalletAction()
        object Failure : WalletAction()
    }

    object DisablePayId : WalletAction()

    data class LoadArtwork(val card: Card, val artworkId: String?) : WalletAction() {
        data class Success(val artwork: Artwork) : WalletAction()
        object Failure : WalletAction()
    }

    object Scan : WalletAction()
    class ScanCardFinished(val scanError: TangemError? = null) : WalletAction()

    data class Send(val amount: Amount? = null) : WalletAction() {
        data class ChooseCurrency(val amounts: List<Amount>?) : WalletAction()
        object Cancel : WalletAction()
    }

    object CreatePayId : WalletAction() {
        data class CompleteCreatingPayId(val payId: String) : WalletAction()
        data class Success(val payId: String) : WalletAction()
        object EmptyField : WalletAction(), ErrorAction {
            override val error = TapError.PayIdEmptyField
        }

        class Failure(override val error: TapError) : WalletAction(), ErrorAction
        object Cancel : WalletAction()
    }

    data class CopyAddress(val context: Context) : WalletAction() {
        object Success : WalletAction(), NotificationAction {
            override val messageResource = R.string.wallet_notification_address_copied
        }
    }

    object ShowDialog : WalletAction() {
        object QrCode : WalletAction()
        object ScanFails : WalletAction()
    }
    object HideDialog : WalletAction()

    data class ExploreAddress(val context: Context) : WalletAction()
    object CreateWallet : WalletAction()
    object EmptyWallet : WalletAction()

    sealed class TopUpAction : WalletAction() {
        data class TopUp(val context: Context, val toolbarColor: Int) : TopUpAction()
    }

    data class ChangeSelectedAddress(val type: AddressType) : WalletAction()

    sealed class TwinsAction : WalletAction() {
        object ShowOnboarding : TwinsAction()
        object SetOnboardingShown : TwinsAction()
        data class SetTwinCard(
                val secondCardId: String, val number: TwinCardNumber,
                val isCreatingTwinCardsAllowed: Boolean,
        ) : TwinsAction()
    }
}