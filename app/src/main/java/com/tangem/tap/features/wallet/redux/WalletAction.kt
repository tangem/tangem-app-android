package com.tangem.tap.features.wallet.redux

import android.content.Context
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.address.AddressType
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.wallet.R
import org.rekotlin.Action

sealed class WalletAction : Action {

    object PopBackToInitialScreen : WalletAction()

    data class UpdateCanSaveUserWallets(val canSaveUserWallets: Boolean) : WalletAction()

    object LoadData : WalletAction() {
        object Refresh : WalletAction()
        object Success : WalletAction()
        data class Failure(val error: TapError?) : WalletAction()
    }

    sealed class MultiWallet : WalletAction() {

        data class SelectWallet(val currency: Currency?) : MultiWallet()

        data class TryToRemoveWallet(val currency: Currency) : MultiWallet()
        data class RemoveWallet(val currency: Currency) : MultiWallet()

        object BackupWallet : MultiWallet()
        data class AddMissingDerivations(val blockchains: List<BlockchainNetwork>) : MultiWallet()
        object ScanToGetDerivations : MultiWallet()

        /**
         * Display warning if card has no backup
         *
         * @param card card to check status
         * */
        data class CheckForBackupWarning(val card: CardDTO) : MultiWallet()
    }

    sealed class Warnings : WalletAction() {
        object CheckHashesCount : Warnings() {

            /**
             * Start online verification of signed hashes for single currency wallets if the warning not displayed
             * */
            object VerifyOnlineIfNeeded : Warnings()
            object SaveCardId : Warnings()
        }

        object CheckIfNeeded : Warnings()
        object Update : Warnings()
        data class Set(val warningList: List<WarningMessage>) : Warnings()

        object AppRating : Warnings() {
            object SetNeverToShow : Warnings()
            object RemindLater : Warnings()
        }

        class CheckRemainingSignatures(val remainingSignatures: Int?) : Warnings()
    }

    data class Scan(val onScanSuccessEvent: AnalyticsEvent?) : WalletAction()

    data class Send(val amount: Amount? = null) : WalletAction()

    data class CopyAddress(val address: String, val context: Context) : WalletAction() {
        object Success : WalletAction(), NotificationAction {
            override val messageResource = R.string.wallet_notification_address_copied
        }
    }

    data class ShareAddress(val address: String, val context: Context) : WalletAction()

    sealed class DialogAction : WalletAction() {
        data class QrCode(
            val currency: Currency,
            val selectedAddress: WalletDataModel.AddressData,
        ) : DialogAction()

        object SignedHashesMultiWalletDialog : DialogAction()
        data class ChooseTradeActionDialog(
            val buyAllowed: Boolean,
            val sellAllowed: Boolean,
            val swapAllowed: Boolean,
        ) : DialogAction()

        data class ChooseCurrency(val amounts: List<Amount>) : DialogAction()
        data class RussianCardholdersWarningDialog(
            val dialogData: WalletDialog.RussianCardholdersWarningDialog.Data? = null,
        ) : DialogAction()

        object Hide : DialogAction()
    }

    data class ExploreAddress(val exploreUrl: String, val context: Context) : WalletAction()

    object CreateWallet : WalletAction()
    object ChangeWallet : WalletAction()
    object ShowSaveWalletIfNeeded : WalletAction()

    sealed class TradeCryptoAction : WalletAction() {
        object Sell : TradeCryptoAction()

        data class Buy(val checkUserLocation: Boolean = true) : TradeCryptoAction()

        data class FinishSelling(val transactionId: String) : TradeCryptoAction()
        data class SendCrypto(
            val currencyId: String,
            val amount: String,
            val destinationAddress: String,
            val transactionId: String,
        ) : TradeCryptoAction()

        object Swap : TradeCryptoAction()
    }

    data class ChangeSelectedAddress(val type: AddressType) : WalletAction()

    sealed class AppCurrencyAction : WalletAction() {
        object ChooseAppCurrency : AppCurrencyAction()
        data class SelectAppCurrency(val fiatCurrency: FiatCurrency) : AppCurrencyAction()
    }

    data class UserWalletChanged(val userWallet: UserWallet) : WalletAction()
    data class WalletStoresChanged(val walletStores: List<WalletStoreModel>) : WalletAction()

    data class TotalFiatBalanceChanged(val balance: TotalFiatBalance) : WalletAction()

    data class UpdateUserWalletArtwork(val walletId: UserWalletId) : WalletAction()

    data class SetArtworkUrl(val userWalletId: UserWalletId, val url: String) : WalletAction()
}
