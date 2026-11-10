package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

/**
[REDACTED_AUTHOR]
 */
sealed class Settings(
    category: String = "Settings",
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category, event, params) {

    sealed class AppSettings(
        event: String,
        params: Map<String, String> = emptyMap(),
    ) : Settings(category = "Settings / App Settings", event = event, params = params) {

        class SaveAccessCodeSwitcherChanged(state: AnalyticsParam.OnOffState) : AppSettings(
            event = "Save Access Code Switcher Changed",
            params = mapOf("State" to state.value),
        )

        class ButtonEnableBiometricAuthentication : AppSettings(event = "Button - Enable Biometric Authentication")

        class MainCurrencyChanged(currencyType: String) : AppSettings(
            event = "Main Currency Changed",
            params = mapOf("Currency Type" to currencyType),
        )

        class ThemeSwitched(theme: Theme) : AppSettings(
            event = "App Theme Switched",
            params = mapOf("State" to theme.value),
        ) {
            enum class Theme(val value: String) {
                System("System"),
                Dark("Dark"),
                Light("Light"),
            }
        }

        class EnableBiometrics : AppSettings(event = "Notice - Enable Biometric")

        class HideBalanceChanged(state: AnalyticsParam.OnOffState) : AppSettings(
            event = "Hide Balance Changed",
            params = mapOf("State" to state.value),
        )
    }
}