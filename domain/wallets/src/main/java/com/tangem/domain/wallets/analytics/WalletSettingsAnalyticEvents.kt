package com.tangem.domain.wallets.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ACTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATUS

sealed class WalletSettingsAnalyticEvents(
    category: String = "Settings / Wallet Settings",
    event: String,
    params: Map<String, String> = mapOf(),
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
        params = mapOf(ACTION to if (isCodeSet) "Set" else "Changing"),
    )

    object BackupScreenOpened : WalletSettingsAnalyticEvents(
        event = "Backup Screen Opened",
    )

    object ButtonManualBackup : WalletSettingsAnalyticEvents(
        event = "Button - Manual Backup",
    )

    object NoticeBackupFirst : WalletSettingsAnalyticEvents(
        event = "Notice - Backup First",
    )

    object ButtonHardwareUpdate : WalletSettingsAnalyticEvents(
        event = "Button - Hardware Update",
    )

    object HardwareUpdateScreenOpened : WalletSettingsAnalyticEvents(
        event = "Hardware Update Screen Opened",
    )
}