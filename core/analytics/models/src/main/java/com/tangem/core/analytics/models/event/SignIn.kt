package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.CriticalEvent

sealed class SignIn(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Sign In", event = event, params = params) {

    /**
     * Tracks the user landing on the app's sign-in screen when a saved card exists.
     */
    class ScreenOpened(walletsCount: Int) : SignIn(
        event = "Sign In Screen Opened",
        params = mapOf(
            AnalyticsParam.WALLETS_COUNT to walletsCount.toString(),
        ),
    ), CriticalEvent

    class ButtonBiometricSignIn : SignIn(event = "Button - Biometric Sign In")

    class ButtonUnlockAllWithBiometric : SignIn(event = "Button - Unlock All With Biometric")

    data class ErrorBiometricUpdated(
        val isFromUnlockAll: Boolean,
    ) : SignIn(event = "Error - Biometric Updated")

    class ButtonWallet(
        signInType: AnalyticsParam.SignInType,
        walletsCount: Int,
    ) : SignIn(
        event = "Button - Wallet",
        params = buildMap {
            put(AnalyticsParam.WALLETS_COUNT, walletsCount.toString())
            put(AnalyticsParam.SIGN_IN_TYPE, signInType.value)
        },
    )

    class ButtonAddWallet(
        val sources: AnalyticsParam.ScreensSources,
    ) : SignIn(
        event = "Button - Add Wallet",
        params = mapOf(
            AnalyticsParam.SOURCE to sources.value,
        ),
    )
}