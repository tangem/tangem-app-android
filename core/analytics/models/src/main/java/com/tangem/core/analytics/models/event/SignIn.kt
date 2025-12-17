package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class SignIn(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent("Sign In", event, params) {

    data class ScreenOpened(
        val walletsCount: Int,
    ) : SignIn(
        event = "Sign In Screen Opened",
        params = mapOf(
            "Wallets Count" to walletsCount.toString(),
        ),
    )

    class ButtonBiometricSignIn : SignIn(event = "Button - Biometric Sign In")

    class ButtonUnlockAllWithBiometric : SignIn(event = "Button - Unlock All With Biometric")

    data class ErrorBiometricUpdated(
        val isFromUnlockAll: Boolean,
    ) : SignIn(event = "Error - Biometric Updated")

    class ButtonWallet(
        signInType: SignInType,
        walletsCount: Int,
    ) : SignIn(
        event = "Button - Wallet",
        params = buildMap {
            put("Wallets Count", walletsCount.toString())
            put("Sign in type", signInType.value)
        },
    ) {
        enum class SignInType(val value: String) {
            Card("Card"),
            Biometric("Biometric"),
            NoSecurity("No Security"),
            AccessCode("Access Code"),
        }
    }

    data class ButtonAddWallet(
        val sources: AnalyticsParam.ScreensSources,
    ) : SignIn(
        event = "Button - Add Wallet",
        params = mapOf(
            AnalyticsParam.SOURCE to sources.value,
        ),
    )
}