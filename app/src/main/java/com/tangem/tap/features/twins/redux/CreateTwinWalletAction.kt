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
sealed class CreateTwinWalletAction : Action {
    data class ShowWarning(
        val twinCardNumber: TwinCardNumber?,
        val createTwinWallet: CreateTwinWallet = CreateTwinWallet.RecreateWallet
    ) : CreateTwinWalletAction()

    object NotEmpty : CreateTwinWalletAction(), NotificationAction {
        override val messageResource = R.string.details_notification_erase_wallet_not_possible
    }

    object ShowAlert : CreateTwinWalletAction()
    object HideAlert : CreateTwinWalletAction()
    object Proceed : CreateTwinWalletAction()

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
        data class Success(val scanResponse: ScanResponse) : CreateTwinWalletAction()
        object Failure : CreateTwinWalletAction()
    }
}