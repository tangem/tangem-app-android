package com.tangem.tap.features.details.redux

import android.content.Context
import com.tangem.Message
import com.tangem.blockchain.common.Wallet
import com.tangem.commands.common.card.Card
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.termsOfUse.CardTou
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.features.details.redux.twins.CreateTwinWallet
import com.tangem.tap.network.coinmarketcap.FiatCurrency
import com.tangem.wallet.R
import org.rekotlin.Action

sealed class DetailsAction : Action {

    data class PrepareScreen(
            val card: Card,
            val scanNoteResponse: ScanNoteResponse,
            val wallets: List<Wallet>,
            val isCreatingTwinWalletAllowed: Boolean?,
            val cardTou: CardTou,
            val fiatCurrencyName: FiatCurrencyName,
            val fiatCurrencies: List<FiatCurrencyName>? = null,
    ) : DetailsAction()

    object ShowDisclaimer : DetailsAction()


    sealed class EraseWallet : DetailsAction() {
        object Check : EraseWallet()
        object Proceed : EraseWallet() {
            object NotAllowedByCard : EraseWallet(), NotificationAction {
                override val messageResource = R.string.error_purge_prohibited
            }

            object NotEmpty : EraseWallet(), NotificationAction {
                override val messageResource = R.string.details_notification_erase_wallet_not_possible
            }
        }

        object Confirm : EraseWallet()
        object Cancel : EraseWallet()
        object Failure : EraseWallet()
        object Success : EraseWallet()
    }

    sealed class CreateTwinWalletAction : DetailsAction() {
        data class ShowWarning(
                val twinCardNumber: TwinCardNumber?,
                val createTwinWallet: CreateTwinWallet = CreateTwinWallet.RecreateWallet
        ) : CreateTwinWalletAction()
        object NotEmpty : CreateTwinWalletAction(), NotificationAction {
            override val messageResource = R.string.details_notification_erase_wallet_not_possible
        }
        object ShowAlert : CreateTwinWalletAction()
        object HideAlert : CreateTwinWalletAction()
        object Proceed: CreateTwinWalletAction()

        object Cancel : CreateTwinWalletAction() {
            object Confirm : CreateTwinWalletAction()
        }

        data class LaunchFirstStep(
            val message: Message, val context: Context
        ) : CreateTwinWalletAction() {
            object Success : CreateTwinWalletAction()
            object Failure : CreateTwinWalletAction()
        }

        data class LaunchSecondStep(
                val initialMessage: Message,
                val preparingMessage: Message,
                val creatingWalletMessage: Message,
        ) : CreateTwinWalletAction() {
            object Success : CreateTwinWalletAction()
            object Failure : CreateTwinWalletAction()
        }

        data class LaunchThirdStep(val message: Message) : CreateTwinWalletAction() {
            data class Success(val scanNoteResponse: ScanNoteResponse) : CreateTwinWalletAction()
            object Failure : CreateTwinWalletAction()
        }
    }

    sealed class AppCurrencyAction : DetailsAction() {
        data class SetCurrencies(val currencies: List<FiatCurrency>) : AppCurrencyAction()
        object ChooseAppCurrency : AppCurrencyAction()
        object Cancel : AppCurrencyAction()
        data class SelectAppCurrency(val fiatCurrencyName: FiatCurrencyName) : AppCurrencyAction()
    }

    sealed class ManageSecurity : DetailsAction() {
        object OpenSecurity : ManageSecurity()
        data class SelectOption(val option: SecurityOption) : ManageSecurity()
        object SaveChanges : ManageSecurity() {
            object Success : ManageSecurity()
            object Failure : ManageSecurity()
        }

        data class ConfirmSelection(val option: SecurityOption) : ManageSecurity() {
            object AlreadySet : ManageSecurity(), NotificationAction {
                override val messageResource = R.string.details_notification_security_option_already_active
            }
        }
    }

}