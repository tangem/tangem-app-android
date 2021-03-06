package com.tangem.tap.features.details.redux

import com.tangem.blockchain.common.Wallet
import com.tangem.common.card.Card
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TwinCardNumber
import com.tangem.operations.pins.CheckUserCodesResponse
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.domain.termsOfUse.CardTou
import com.tangem.wallet.R
import org.rekotlin.Action

sealed class DetailsAction : Action {

    data class PrepareScreen(
        val scanResponse: ScanResponse,
        val wallets: List<Wallet>,
        val cardTou: CardTou,
    ) : DetailsAction()

    object ShowDisclaimer : DetailsAction()
    data class ReCreateTwinsWallet(val number: TwinCardNumber) : DetailsAction()

    sealed class ResetToFactory : DetailsAction() {
        object Check : ResetToFactory()
        object Proceed : ResetToFactory() {
            object NotAllowedByCard : ResetToFactory(), NotificationAction {
                override val messageResource = R.string.error_purge_prohibited
            }

            object NotEmpty : ResetToFactory(), NotificationAction {
                override val messageResource = R.string.details_notification_erase_wallet_not_possible
            }
        }

        object Confirm : ResetToFactory()
        object Cancel : ResetToFactory()
        object Failure : ResetToFactory()
        object Success : ResetToFactory()
    }

    object CreateBackup : DetailsAction()

    sealed class ManageSecurity : DetailsAction() {
        data class CheckCurrentSecurityOption(val card: Card) : ManageSecurity()
        data class SetCurrentOption(val userCodes: CheckUserCodesResponse) : ManageSecurity()
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

    data class ChangeAppCurrency(val fiatCurrency: FiatCurrency) : DetailsAction()
}
