package com.tangem.tap.features.wallet.redux

import android.content.Context
import com.tangem.TangemError
import com.tangem.blockchain.common.*
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


    data class LoadWallet(val allowTopUp: Boolean? = null, val currency: CryptoCurrencyName? = null) : WalletAction() {
        data class Success(val wallet: Wallet) : WalletAction()
        data class NoAccount(val wallet: Wallet, val amountToCreateAccount: String) : WalletAction()
        data class Failure(val wallet: Wallet, val errorMessage: String? = null) : WalletAction()
    }

    data class SetArtworkId(val artworkId: String?) : WalletAction()


    //    sealed class ProcessWallet : WalletAction() {
//        data class LoadWallet(val artworkId: String?, val allowTopUp: Boolean
//        ) : ProcessWallet() {
//            data class Success(val wallet: Wallet) : ProcessWallet()
//            data class NoAccount(val wallet: Wallet, val amountToCreateAccount: String) : ProcessWallet()
//            data class Failure(val wallet: Wallet, val errorMessage: String? = null) : ProcessWallet()
//        }
//
//        data class UpdateWallet(val currency: CryptoCurrencyName? = null) : ProcessWallet() {
//            object ScheduleUpdatingWallet : ProcessWallet()
//            data class Success(val wallet: Wallet) : ProcessWallet()
//            data class Failure(val errorMessage: String? = null) : ProcessWallet()
//        }
//    }

    sealed class MultiWallet : WalletAction() {
        data class SetIsMultiwalletAllowed(val isMultiwalletAllowed: Boolean) : MultiWallet()
        data class AddWalletManagers(val walletManagers: List<WalletManager>) : MultiWallet()
        data class AddBlockchain(val blockchain: Blockchain) : MultiWallet()
        data class AddBlockchains(val blockchains: List<Blockchain>) : MultiWallet()
        data class AddTokens(val tokens: List<Token>) : MultiWallet()
        data class AddToken(val token: Token) : MultiWallet()
        data class TokenLoaded(val amount: Amount) : MultiWallet()
        data class SelectWallet(val walletData: WalletData?) : MultiWallet()
        data class RemoveWallet(val walletData: WalletData) : MultiWallet()
        data class SetPrimaryBlockchain(val blockchain: Blockchain) : MultiWallet()
        data class SetPrimaryToken(val token: Token) : MultiWallet()
    }

    sealed class Warnings : WalletAction() {
        object CheckHashesCount : Warnings() {
            object CheckHashesCountOnline : Warnings()
            object NeedToCheckHashesCountOnline : Warnings()
            object ConfirmHashesCount : Warnings()
            object SaveCardId : Warnings()
        }

        object CheckIfNeeded : Warnings()
        data class SetWarnings(val warningList: List<WarningMessage>) : Warnings()

        object AppRating : Warnings() {
            object SetNeverToShow : Warnings()
            object RemindLater : Warnings()
        }
    }

    data class UpdateWallet(val currency: CryptoCurrencyName? = null) : WalletAction() {
        object ScheduleUpdatingWallet : WalletAction()
        data class Success(val wallet: Wallet) : WalletAction()
        data class Failure(val errorMessage: String? = null) : WalletAction()
    }

    data class LoadFiatRate(
            val wallet: Wallet? = null, val currency: CryptoCurrencyName? = null
    ) : WalletAction() {
        data class Success(val fiatRate: Pair<CryptoCurrencyName, BigDecimal?>) : WalletAction()
        object Failure : WalletAction()
    }

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


    object EmptyField : WalletAction(), ErrorAction {
        override val error = TapError.PayIdEmptyField
    }

    data class CopyAddress(val address: String, val context: Context) : WalletAction() {
        object Success : WalletAction(), NotificationAction {
            override val messageResource = R.string.wallet_notification_address_copied
        }
    }

    data class ShareAddress(val address: String, val context: Context) : WalletAction()

    object ShowDialog : WalletAction() {
        object QrCode : WalletAction()
        object ScanFails : WalletAction()
    }

    object HideDialog : WalletAction()

    data class ExploreAddress(val exploreUrl: String, val context: Context) : WalletAction()

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