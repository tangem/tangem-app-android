package com.tangem.tap.common.redux.global

import com.tangem.blockchain.common.WalletManager
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.tap.common.redux.DebugErrorAction
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.common.redux.ToastNotificationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.features.feedback.EmailData
import com.tangem.tap.features.feedback.FeedbackManager
import com.tangem.tap.network.moonpay.MoonPayUserStatus
import org.rekotlin.Action

sealed class GlobalAction : Action {

    // notifications
    data class ShowNotification(override val messageResource: Int) : GlobalAction(), NotificationAction
    data class ShowToastNotification(override val messageResource: Int) : GlobalAction(), ToastNotificationAction

    // dialogs
    data class ShowDialog(val stateDialog: StateDialog) : GlobalAction()
    object HideDialog : GlobalAction()

    data class DebugShowError(override val error: TapError) : GlobalAction(), DebugErrorAction

    data class ReadCard(
        val onSuccess: (suspend (ScanNoteResponse) -> Unit)? = null,
        val onFailure: (suspend (TangemError) -> Unit)? = null,
        val messageResId: Int? = null,
    ) : GlobalAction()

    object ScanFailsCounter {
        data class ChooseBehavior(val result: CompletionResult<ScanNoteResponse>) : GlobalAction()
        object Reset : GlobalAction()
        object Increment : GlobalAction()
    }

    data class SaveScanNoteResponse(val scanNoteResponse: ScanNoteResponse) : GlobalAction()

    data class SetIfCardVerifiedOnline(val verified: Boolean) : GlobalAction()

    data class ChangeAppCurrency(val appCurrency: FiatCurrencyName) : GlobalAction()
    object RestoreAppCurrency : GlobalAction() {
        data class Success(val appCurrency: FiatCurrencyName) : GlobalAction()
    }

    data class UpdateWalletSignedHashes(
        val walletSignedHashes: Int?,
        val remainingSignatures: Int?,
        val walletPublicKey: ByteArray,
    ) : GlobalAction()

    data class HideWarningMessage(val warning: WarningMessage) : GlobalAction()
    data class UpdateSecurityOptions(val securityOption: SecurityOption) : GlobalAction()

    data class SetConfigManager(val configManager: ConfigManager) : GlobalAction()
    data class SetWarningManager(val warningManager: WarningMessagesManager) : GlobalAction()
    data class SetFeedbackManager(val feedbackManager: FeedbackManager) : GlobalAction()

    data class SendFeedback(val emailData: EmailData) : GlobalAction()
    data class UpdateFeedbackInfo(val walletManagers: List<WalletManager>) : GlobalAction()

    object GetMoonPayUserStatus : GlobalAction() {
        data class Success(val moonPayUserStatus: MoonPayUserStatus) : GlobalAction()
    }
}