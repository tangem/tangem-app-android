package com.tangem.tap.features.twins.redux

import android.content.Context
import com.tangem.Message
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.wallet.R
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed class TwinCardsAction : Action {
    object ShowOnboarding : TwinCardsAction()
    object SetOnboardingShown : TwinCardsAction()
    data class SetTwinCard(
        val number: TwinCardNumber,
        val secondCardId: String?,
        val isCreatingTwinCardsAllowed: Boolean,
    ) : TwinCardsAction()

    sealed class CreateWallet : TwinCardsAction() {
        data class Create(
            val number: TwinCardNumber,
            val createTwinWalletMode: CreateTwinWalletMode
        ) : CreateWallet()

        object NotEmpty : CreateWallet(), NotificationAction {
            override val messageResource = R.string.details_notification_erase_wallet_not_possible
        }

        object ShowAlert : CreateWallet()
        object HideAlert : CreateWallet()
        object Proceed : CreateWallet()

        object Cancel : CreateWallet() {
            object Confirm : CreateWallet()
        }

        data class LaunchFirstStep(
            val message: Message, val context: Context
        ) : CreateWallet() {
            object Success : CreateWallet()
            object Failure : CreateWallet()
        }

        data class LaunchSecondStep(
            val initialMessage: Message,
            val preparingMessage: Message,
            val creatingWalletMessage: Message,
        ) : CreateWallet() {
            object Success : CreateWallet()
            object Failure : CreateWallet()
        }

        data class LaunchThirdStep(val message: Message) : CreateWallet() {
            data class Success(val scanResponse: ScanResponse) : CreateWallet()
            object Failure : CreateWallet()
        }
    }
}