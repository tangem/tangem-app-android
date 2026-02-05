package com.tangem.features.account.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

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

    class ArchivedAccountsScreenOpened(
        private val accountsCount: Int,
    ) : WalletSettingsAccountAnalyticEvents(
        event = "Archived Accounts Screen Opened",
        params = buildMap {
            put("Accounts Count", accountsCount.toString())
        },
    )

    class ButtonRecoverAccount(
        accountDerivation: Int?,
    ) : WalletSettingsAccountAnalyticEvents(
        event = "Button - Recover Account",
        params = buildMap {
            accountDerivation?.let {
                put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString())
            }
        },
    )
}