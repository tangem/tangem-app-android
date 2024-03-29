package com.tangem.tap.common.redux.global

import com.tangem.blockchain.common.WalletManager
import com.tangem.common.CompletionResult
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.navigation.StateDialog
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.config.models.ChatConfig
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.tap.common.feedback.FeedbackData
import com.tangem.tap.common.feedback.FeedbackManager
import com.tangem.tap.common.redux.DebugErrorAction
import com.tangem.tap.common.redux.ErrorAction
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.details.redux.SecurityOption
import org.rekotlin.Action

sealed class GlobalAction : Action {

    // notifications
    data class ShowNotification(override val messageResource: Int) : GlobalAction(), NotificationAction
    data class ShowErrorNotification(override val error: TapError) : GlobalAction(), ErrorAction
    data class DebugShowErrorNotification(override val error: TapError) : GlobalAction(), DebugErrorAction

    // dialogs
    data class ShowDialog(val stateDialog: StateDialog) : GlobalAction()
    object HideDialog : GlobalAction()

    sealed class Onboarding : GlobalAction() {
        /**
         * Initiate an onboarding process.
         * For resuming unfinished backup of standard Wallet see
         * BackupAction.CheckForUnfinishedBackup, GlobalAction.Onboarding.StartForUnfinishedBackup
         */
        data class Start(val scanResponse: ScanResponse, val canSkipBackup: Boolean = true) : Onboarding()

        /**
         * Initiate resuming of unfinished backup for standard Wallet.
         * See more BackupAction.CheckForUnfinishedBackup
         */
        data class StartForUnfinishedBackup(val addedBackupCardsCount: Int) : Onboarding()

        object Stop : Onboarding()

        data class ShouldResetCardOnCreate(val shouldReset: Boolean) : Onboarding()
    }

    object ScanFailsCounter {
        data class ChooseBehavior(
            val result: CompletionResult<ScanResponse>,
            val analyticsSource: AnalyticsParam.ScreensSources,
        ) : GlobalAction()

        object Reset : GlobalAction()
        object Increment : GlobalAction()
    }

    data class SaveScanResponse(val scanResponse: ScanResponse) : GlobalAction()

    data class SetIfCardVerifiedOnline(val verified: Boolean) : GlobalAction()

    data class ChangeAppCurrency(val appCurrency: AppCurrency) : GlobalAction()
    object RestoreAppCurrency : GlobalAction() {
        data class Success(val appCurrency: AppCurrency) : GlobalAction()
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
    data class OpenChat(val feedbackData: FeedbackData, val chatConfig: ChatConfig? = null) : GlobalAction()
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

    data class UpdateUserWalletsListManager(val manager: UserWalletsListManager) : GlobalAction()
    data class ChangeAppThemeMode(val appThemeMode: AppThemeMode) : GlobalAction()
}
