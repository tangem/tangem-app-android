package com.tangem.domain.wallets.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ACTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATUS
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent

sealed class WalletSettingsAnalyticEvents(
    category: String = "Settings / Wallet Settings",
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category, event, params) {

    data class NftToggleSwitch(val enabled: AnalyticsParam.OnOffState) : WalletSettingsAnalyticEvents(
        event = "NFT toggle switch",
        params = mapOf(STATUS to enabled.value),
    )

    class WalletSettingsScreenOpened(
        private val accountsCount: Int?,
    ) : WalletSettingsAnalyticEvents(
        event = "Wallet Settings Screen Opened",
        params = buildMap {
            if (accountsCount != null) put("Accounts Count", accountsCount.toString())
        },
    )

    class ButtonBackup : WalletSettingsAnalyticEvents(
        event = "Button - Backup",
    )

    class ButtonAddAccount : WalletSettingsAnalyticEvents(
        event = "Button - Add Account",
    )

    class ButtonOpenExistingAccount : WalletSettingsAnalyticEvents(
        event = "Button - Open Existing Account",
    )

    class ButtonArchivedAccounts : WalletSettingsAnalyticEvents(
        event = "Button - Archived Accounts",
    )

    class LongtapAccountsOrder : WalletSettingsAnalyticEvents(
        event = "Longtap - Accounts Order",
    )

    data class ButtonAccessCode(
        private val isCodeSet: Boolean,
    ) : WalletSettingsAnalyticEvents(
        event = "Button - Access Code",
        params = mapOf(ACTION to if (isCodeSet) "Changing" else "Set"),
    )

    data class BackupScreenOpened(
        val isBackedUp: Boolean,
        val cloudBackupState: AnalyticsParam.CloudBackupState?,
    ) : WalletSettingsAnalyticEvents(
        event = "Backup Screen Opened",
        params = buildMap {
            put(AnalyticsParam.MANUAL_BACKUP, isBackedUp.toYesNo())
            cloudBackupState?.let { put(AnalyticsParam.CLOUD_BACKUP, it.value) }
        },
    )

    class SetCloudPasswordScreen : WalletSettingsAnalyticEvents(
        event = "Set Cloud Password Screen",
    )

    class ConfirmCloudPasswordScreen : WalletSettingsAnalyticEvents(
        event = "Confirm Cloud Password Screen",
    )

    data class CloudBackupCreationError(
        val errorMessage: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Cloud Backup Creation Error",
        params = mapOf(AnalyticsParam.ERROR_MESSAGE to errorMessage),
    )

    class CloudBackupScreenClosed : WalletSettingsAnalyticEvents(
        event = "Cloud Backup Screen Closed",
    )

    class CloudBackupDetailsScreen : WalletSettingsAnalyticEvents(
        event = "Cloud Backup Details Screen",
    )

    class CloudBackupDeletionRequest : WalletSettingsAnalyticEvents(
        event = "Cloud Backup Deletion Request",
    )

    /** Sent both when the user removes a backup and when the app removes it (upgrade, forget wallet) */
    class CloudBackupDeleted : WalletSettingsAnalyticEvents(
        event = "Cloud Backup Deleted",
    )

    data class CloudBackupDeletionError(
        val errorMessage: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Cloud Backup Deletion Error",
        params = mapOf(AnalyticsParam.ERROR_MESSAGE to errorMessage),
    )

    data class ForgetWalletRequest(
        val source: String,
        val cloudBackupState: AnalyticsParam.CloudBackupState?,
        val isBackedUp: Boolean,
    ) : WalletSettingsAnalyticEvents(
        event = "Forget Wallet Request",
        params = buildMap {
            put(AnalyticsParam.SOURCE, source)
            put(AnalyticsParam.MANUAL_BACKUP, isBackedUp.toYesNo())
            cloudBackupState?.let { put(AnalyticsParam.CLOUD_BACKUP, it.value) }
        },
    )

    class ForgetWalletScreen : WalletSettingsAnalyticEvents(
        event = "Forget Wallet Screen",
    )

    class WalletForgotten : WalletSettingsAnalyticEvents(
        event = "Wallet Forgotten",
    )

    class ButtonRecoveryPhrase : WalletSettingsAnalyticEvents(
        event = "Button - Recovery phrase",
    )

    class ButtonGoogleDriveBackup : WalletSettingsAnalyticEvents(
        event = "Button - Cloud Backup",
    )

    data class NoticeBackupFirst(
        val source: String,
        val action: Action,
        val cloudBackupState: AnalyticsParam.CloudBackupState? = null,
        val isBackedUp: Boolean? = null,
    ) : WalletSettingsAnalyticEvents(
        event = "Notice - Backup First",
        params = buildMap {
            put(AnalyticsParam.SOURCE, source)
            put(ACTION, action.value)
            cloudBackupState?.let { put(AnalyticsParam.CLOUD_BACKUP, it.value) }
            isBackedUp?.let { put(AnalyticsParam.MANUAL_BACKUP, it.toYesNo()) }
        },
    ) {
        enum class Action(val value: String) {
            AccessCode("Access Code"),
            Upgrade("Upgrade"),
            Remove("Remove"),
        }
    }

    class ButtonHardwareUpdate : WalletSettingsAnalyticEvents(
        event = "Button - Hardware Update",
    )

    class HardwareUpgradeScreenOpened : WalletSettingsAnalyticEvents(
        event = "Hardware Upgrade Screen Opened",
    )

    class ButtonCreateNewWallet : WalletSettingsAnalyticEvents(
        event = "Button - Create New Wallet",
    )

    class ButtonUpgradeCurrent : WalletSettingsAnalyticEvents(
        event = "Button - Upgrade Current",
    )

    class CreateWalletScreenOpened : WalletSettingsAnalyticEvents(
        event = "Create Wallet Screen Opened",
    )

    class HardwareBackupScreenOpened : WalletSettingsAnalyticEvents(
        event = "Hardware Backup Screen Opened",
    )

    data class RecoveryPhraseScreenInfo(
        val source: String,
        val action: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Recovery Phrase Screen Info",
        params = mapOf(
            AnalyticsParam.SOURCE to source,
            ACTION to action,
        ),
    )

    data class RecoveryPhraseScreen(
        val source: String,
        val action: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Recovery Phrase Screen",
        params = mapOf(
            AnalyticsParam.SOURCE to source,
            ACTION to action,
        ),
    )

    data class RecoveryPhraseCheck(
        val source: String,
        val action: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Recovery Phrase Check",
        params = mapOf(
            AnalyticsParam.SOURCE to source,
            ACTION to action,
        ),
    )

    data class BackupCompleteScreen(
        val source: String,
        val action: String,
        val backupType: AnalyticsParam.BackupType? = null,
    ) : WalletSettingsAnalyticEvents(
        event = "Backup Complete Screen",
        params = buildMap {
            put(AnalyticsParam.SOURCE, source)
            put(ACTION, action)
            backupType?.let { put(AnalyticsParam.BACKUP_TYPE, it.value) }
        },
    )

    data class AccessCodeScreenOpened(
        val source: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Access Code Screen Opened",
        params = mapOf(AnalyticsParam.SOURCE to source),
    )

    data class ReEnterAccessCodeScreen(
        val source: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Re-enter Access Code Screen",
        params = mapOf(AnalyticsParam.SOURCE to source),
    )

    class ButtonStartUpgrade : WalletSettingsAnalyticEvents(
        event = "Button - Start Upgrade",
    )

    class WalletUpgraded : WalletSettingsAnalyticEvents(
        event = "Wallet Upgraded",
    ), AppsFlyerIncludedEvent

    class WalletsReorder : WalletSettingsAnalyticEvents(
        event = "Longtap - Wallets Order",
    )

    enum class RecoveryPhraseScreenAction(val value: String) {
        Upgrade("Upgrade"),
        Backup("Backup"),
        AccessCode("Access Code"),
        Remove("Remove"),
    }
}

private fun Boolean.toYesNo(): String = if (this) "Yes" else "No"