package com.tangem.features.account.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class WalletSettingsAccountAnalyticEvents(
    category: String = "Settings / Wallet Settings",
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category, event, params) {

    class AccountCreated : WalletSettingsAccountAnalyticEvents(
        event = "Account Created",
    )

    class AccountRecovered : WalletSettingsAccountAnalyticEvents(
        event = "Account Recovered",
    )

    class ArchivedAccountsScreenOpened : WalletSettingsAccountAnalyticEvents(
        event = "Archived Accounts Screen Opened",
    )

    class ButtonRecoverAccount : WalletSettingsAccountAnalyticEvents(
        event = "Button - Recover Account",
    )
}