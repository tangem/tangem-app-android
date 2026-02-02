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
    ) : WalletSettingsAnalyticEvents(
        event = "Backup Screen Opened",
        params = mapOf("Manual Backup" to if (isBackedUp) "Yes" else "No"),
    )

    class ButtonRecoveryPhrase : WalletSettingsAnalyticEvents(
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

    class ButtonStartUpgrade : WalletSettingsAnalyticEvents(
        event = "Button - Start Upgrade",
    )

    class WalletUpgraded : WalletSettingsAnalyticEvents(
        event = "Wallet Upgraded",
    ), AppsFlyerIncludedEvent

    enum class RecoveryPhraseScreenAction(val value: String) {
        Upgrade("Upgrade"),
        Backup("Backup"),
        AccessCode("Access Code"),
        Remove("Remove"),
    }
}