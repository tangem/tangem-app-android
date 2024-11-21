package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.EventValue

/**
[REDACTED_AUTHOR]
 */
sealed class Settings(
    category: String = "Settings",
    event: String,
    params: Map<String, EventValue> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent(category, event, params, error) {

    class ScreenOpened : Settings(event = "Settings Screen Opened")
    class ButtonStartWalletConnectSession : Settings(event = "Button - Start Wallet Connect Session")
    class ButtonStopWalletConnectSession : Settings(event = "Button - Stop Wallet Connect Session")

    sealed class CardSettings(
        event: String,
        params: Map<String, EventValue> = mapOf(),
        error: Throwable? = null,
    ) : Settings("Settings / Card Settings", event, params, error) {

        class ButtonFactoryReset : CardSettings("Button - Factory Reset")
        class FactoryResetFinished(cardsCount: Int? = null, error: Throwable? = null) : CardSettings(
            event = "Factory Reset Finished",
            params = buildMap {
                cardsCount?.let { put("Cards Count", it.asStringValue()) }
            },
            error = error,
        )

        class FactoryResetCanceled(cardsCount: Int) : CardSettings(
            event = "Factory Reset Canceled",
            params = mapOf("Cards Count" to cardsCount.asStringValue()),
        )

        class UserCodeChanged : CardSettings("User Code Changed")
        class ButtonChangeSecurityMode : CardSettings("Button - Change Security Mode")

        class ButtonChangeUserCode(type: AnalyticsParam.UserCode) : CardSettings(
            event = "Button - Change User Code",
            params = mapOf("Type" to type.value.asStringValue()),
        )

        class SecurityModeChanged(mode: AnalyticsParam.SecurityMode, error: Throwable? = null) : CardSettings(
            event = "Security Mode Changed",
            params = mapOf("Mode" to mode.value.asStringValue()),
            error = error,
        )

        class AccessCodeRecoveryChanged(status: AnalyticsParam.AccessCodeRecoveryStatus) : CardSettings(
            event = "Access Code Recovery Changed",
            params = mapOf(status.key to status.value.asStringValue()),
        )
    }

    sealed class AppSettings(
        event: String,
        params: Map<String, EventValue> = mapOf(),
    ) : Settings(category = "Settings / App Settings", event = event, params = params) {

        class SaveWalletSwitcherChanged(state: AnalyticsParam.OnOffState) : AppSettings(
            event = "Save Wallet Switcher Changed",
            params = mapOf("State" to state.value.asStringValue()),
        )

        class SaveAccessCodeSwitcherChanged(state: AnalyticsParam.OnOffState) : AppSettings(
            event = "Save Access Code Switcher Changed",
            params = mapOf("State" to state.value.asStringValue()),
        )

        object ButtonEnableBiometricAuthentication : AppSettings(event = "Button - Enable Biometric Authentication")

        class MainCurrencyChanged(currencyType: String) : AppSettings(
            event = "Main Currency Changed",
            params = mapOf("Currency Type" to currencyType.asStringValue()),
        )

        class ThemeSwitched(theme: AnalyticsParam.AppTheme) : AppSettings(
            event = "App Theme Switched",
            params = mapOf("State" to theme.value.asStringValue()),
        )

        object EnableBiometrics : AppSettings(event = "Notice - Enable Biometric")

        class HideBalanceChanged(state: AnalyticsParam.OnOffState) : AppSettings(
            event = "Hide Balance Changed",
            params = mapOf("State" to state.value.asStringValue()),
        )
    }
}