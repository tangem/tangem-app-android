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

    class ButtonUnlockAllWithBiometric : SignIn(event = "Button - Unlock All With Biometric")

    class ButtonWallet(
        signInType: SignInType,
    ) : SignIn(
        event = "Button - Wallet",
        params = buildMap {
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