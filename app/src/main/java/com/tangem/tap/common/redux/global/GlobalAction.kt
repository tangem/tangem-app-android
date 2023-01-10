package com.tangem.tap.common.redux.global

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.feedback.FeedbackData
import com.tangem.tap.common.feedback.FeedbackManager
import com.tangem.tap.common.redux.DebugErrorAction
import com.tangem.tap.common.redux.ErrorAction
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.common.redux.ToastNotificationAction
import com.tangem.tap.common.zendesk.ZendeskConfig
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.details.redux.SecurityOption
import org.rekotlin.Action

sealed class GlobalAction : Action {

    // notifications
    data class ShowNotification(override val messageResource: Int) : GlobalAction(), NotificationAction
    data class ShowToastNotification(override val messageResource: Int) : GlobalAction(), ToastNotificationAction
    data class ShowErrorNotification(override val error: TapError) : GlobalAction(), ErrorAction
    data class DebugShowErrorNotification(override val error: TapError) : GlobalAction(), DebugErrorAction

    // dialogs
    data class ShowDialog(val stateDialog: StateDialog) : GlobalAction()
    object HideDialog : GlobalAction()

    sealed class Onboarding : GlobalAction() {
        /**
         * Initiate an onboarding process.
         * For SaltPay cards it's additionally checks for unfinished backup.
         * For resuming unfinished backup for standard Wallet cards see CheckForUnfinishedBackup and
         * StartForUnfinishedBackup
         */
        data class Start(val scanResponse: ScanResponse, val canSkipBackup: Boolean = true) : Onboarding()

        /**
         * Initiate resuming of unfinished backup only for standard Wallet cards.
         * For SaltPay cards unfinished backup resumed after scanning the card on HomeScreen through Onboarding.Start.
         * See more Onboarding.Start, CheckForUnfinishedBackup
         */
        data class StartForUnfinishedBackup(val addedBackupCardsCount: Int) : Onboarding()
        object Stop : Onboarding()
    }

    data class ScanCard(
        val additionalBlockchainsToDerive: Collection<Blockchain>? = null,
        val onSuccess: ((ScanResponse) -> Unit)? = null,
        val onFailure: ((TangemError) -> Unit)? = null,
        val messageResId: Int? = null,
    ) : GlobalAction()

    object ScanFailsCounter {
        data class ChooseBehavior(val result: CompletionResult<ScanResponse>) : GlobalAction()
        object Reset : GlobalAction()
        object Increment : GlobalAction()
    }

    data class SaveScanResponse(val scanResponse: ScanResponse) : GlobalAction()

    data class SetIfCardVerifiedOnline(val verified: Boolean) : GlobalAction()

    data class ChangeAppCurrency(val appCurrency: FiatCurrency) : GlobalAction()
    object RestoreAppCurrency : GlobalAction() {
        data class Success(val appCurrency: FiatCurrency) : GlobalAction()
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

    data class SendEmail(val feedbackData: FeedbackData) : GlobalAction()
    data class OpenChat(val feedbackData: FeedbackData, val zendeskConfig: ZendeskConfig? = null) : GlobalAction()
    data class UpdateFeedbackInfo(val walletManagers: List<WalletManager>) : GlobalAction()

    object ExchangeManager : GlobalAction() {
        object Init : GlobalAction() {
            data class Success(
                val exchangeManager: com.tangem.tap.network.exchangeServices.CurrencyExchangeManager,
            ) : GlobalAction()
        }

        object Update : GlobalAction()
    }

    object FetchUserCountry : GlobalAction() {
        data class Success(val countryCode: String) : GlobalAction()
    }
}
