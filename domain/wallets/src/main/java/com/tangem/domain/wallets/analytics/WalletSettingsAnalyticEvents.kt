package com.tangem.domain.wallets.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ACTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATUS

sealed class WalletSettingsAnalyticEvents(
    category: String = "Settings / Wallet Settings",
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category, event, params) {

    data class NftToggleSwitch(val enabled: AnalyticsParam.OnOffState) : WalletSettingsAnalyticEvents(
        event = "NFT toggle switch",
        params = mapOf(STATUS to enabled.value),
    )

    object WalletSettingsScreenOpened : WalletSettingsAnalyticEvents(
        event = "Wallet Settings Screen Opened",
    )

    object ButtonBackup : WalletSettingsAnalyticEvents(
        event = "Button - Backup",
    )

    data class ButtonAccessCode(
        private val isCodeSet: Boolean,
    ) : WalletSettingsAnalyticEvents(
        event = "Button - Access Code",
        params = mapOf(ACTION to if (isCodeSet) "Changing" else "Set"),
    )

    data class BackupScreenOpened(
        val isManualBackupEnabled: Boolean,
    ) : WalletSettingsAnalyticEvents(
        event = "Backup Screen Opened",
        params = mapOf("Manual Backup" to if (isManualBackupEnabled) "Enabled" else "Disabled"),
    )

    object ButtonRecoveryPhrase : WalletSettingsAnalyticEvents(
        event = "Button - Recovery phrase",
    )

    data class NoticeBackupFirst(
        val source: String,
        val action: Action,
    ) : WalletSettingsAnalyticEvents(
        event = "Notice - Backup First",
        params = mapOf(
            AnalyticsParam.Key.SOURCE to source,
            AnalyticsParam.Key.ACTION to action.value,
        ),
    ) {
        enum class Action(val value: String) {
            AccessCode("Access Code"),
            Upgrade("Upgrade"),
            Remove("Remove"),
        }
    }

    object ButtonHardwareUpdate : WalletSettingsAnalyticEvents(
        event = "Button - Hardware Update",
    )

    object HardwareUpgradeScreenOpened : WalletSettingsAnalyticEvents(
        event = "Hardware Upgrade Screen Opened",
    )

    object ButtonCreateNewWallet : WalletSettingsAnalyticEvents(
        event = "Button - Create New Wallet",
    )

    object ButtonUpgradeCurrent : WalletSettingsAnalyticEvents(
        event = "Button - Upgrade Current",
    )

    object CreateWalletScreenOpened : WalletSettingsAnalyticEvents(
        event = "Create Wallet Screen Opened",
    )

    object HardwareBackupScreenOpened : WalletSettingsAnalyticEvents(
        event = "Hardware Backup Screen Opened",
    )

    data class RecoveryPhraseScreenInfo(
        val source: String,
        val action: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Recovery Phrase Screen Info",
        params = mapOf(
            AnalyticsParam.Key.SOURCE to source,
            AnalyticsParam.Key.ACTION to action,
        ),
    )

    data class RecoveryPhraseScreen(
        val source: String,
        val action: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Recovery Phrase Screen",
        params = mapOf(
            AnalyticsParam.Key.SOURCE to source,
            AnalyticsParam.Key.ACTION to action,
        ),
    )

    data class RecoveryPhraseCheck(
        val source: String,
        val action: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Recovery Phrase Check",
        params = mapOf(
            AnalyticsParam.Key.SOURCE to source,
            AnalyticsParam.Key.ACTION to action,
        ),
    )

    data class BackupCompleteScreen(
        val source: String,
        val action: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Backup Complete Screen",
        params = mapOf(
            AnalyticsParam.Key.SOURCE to source,
            AnalyticsParam.Key.ACTION to action,
        ),
    )

    data class AccessCodeScreenOpened(
        val source: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Access Code Screen Opened",
        params = mapOf(AnalyticsParam.Key.SOURCE to source),
    )

    data class ReEnterAccessCodeScreen(
        val source: String,
    ) : WalletSettingsAnalyticEvents(
        event = "Re-enter Access Code Screen",
        params = mapOf(AnalyticsParam.Key.SOURCE to source),
    )

    object ButtonStartUpgrade : WalletSettingsAnalyticEvents(
        event = "Button - Start Upgrade",
    )

    enum class RecoveryPhraseScreenAction(val value: String) {
        Upgrade("Upgrade"),
        Backup("Backup"),
        AccessCode("Access Code"),
        Remove("Remove"),
    }
}