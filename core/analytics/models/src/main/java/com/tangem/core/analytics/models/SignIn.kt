package com.tangem.core.analytics.models

import com.tangem.core.analytics.models.AnalyticsParam.Key.ACTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.WALLETS_COUNT

sealed class SignIn(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Sign In", event, params) {

    class SignInScreenOpened(
        walletsCount: Int,
    ) : SignIn(
        event = "Sign In Screen Opened",
        params = mapOf(
            WALLETS_COUNT to walletsCount.toString(),
        ),
    )

    data object ErrorBiometricUpdated : SignIn("Error - Biometric Updated")

    data object ButtonUnlockAllWithBiometric : SignIn("Button - Unlock All With Biometric")

    class ButtonAddWallet(
        action: Action,
    ) : SignIn(
        event = "Button - Add Wallet",
        params = mapOf(
            ACTION to action.value,
        ),
    ) {
        enum class Action(val value: String) {
            Create("Create"),
            Import("Import"),
            Buy("Buy"),
        }
    }
}