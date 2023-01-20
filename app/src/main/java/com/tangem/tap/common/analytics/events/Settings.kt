package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.tap.features.details.ui.details.SocialNetwork

/**
 * Created by Anton Zhilenkov on 28.09.2022.
 */
sealed class Settings(
    category: String = "Settings",
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent(category, event, params, error) {

    class ScreenOpened : Settings(event = "Settings Screen Opened")
    class ButtonChat : Settings(event = "Button - Chat")
    class ButtonSendFeedback : Settings(event = "Button - Send Feedback")
    class ButtonStartWalletConnectSession : Settings(event = "Button - Start Wallet Connect Session")
    class ButtonStopWalletConnectSession : Settings(event = "Button - Stop Wallet Connect Session")
    class ButtonCardSettings : Settings(event = "Button - Card Settings")
    class ButtonAppSettings : Settings(event = "Button - App Settings")
    class ButtonCreateBackup : Settings(event = "Button - Create Backup")
    class ButtonWalletConnect : Settings(event = "Button - Wallet Connect")

    class ButtonSocialNetwork(network: SocialNetwork) : Settings(
        event = "Button - Social Network",
        params = mapOf("Network" to network.id),
    )

    sealed class CardSettings(
        event: String,
        params: Map<String, String> = mapOf(),
        error: Throwable? = null,
    ) : Settings("Settings / Card Settings", event, params, error) {

        class ButtonFactoryReset : CardSettings("Button - Factory Reset")
        class FactoryResetFinished(error: Throwable? = null) : CardSettings(
            event = "Factory Reset Finished",
            error = error,
        )

        class UserCodeChanged : CardSettings("User Code Changed")
        class ButtonChangeSecurityMode : CardSettings("Button - Change Security Mode")

        class ButtonChangeUserCode(type: AnalyticsParam.UserCode) : CardSettings(
            event = "Button - Change User Code",
            params = mapOf("Type" to type.value),
        )

        class SecurityModeChanged(mode: AnalyticsParam.SecurityMode, error: Throwable? = null) : CardSettings(
            event = "Security Mode Changed",
            params = mapOf("Mode" to mode.value),
            error = error,
        )
    }

    sealed class AppSettings(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Settings("Settings / App Settings", event, params) {

        class SaveWalletSwitcherChanged(state: AnalyticsParam.OnOffState) : CardSettings(
            event = "Save Wallet Switcher Changed",
            params = mapOf("State" to state.value),
        )

        class SaveAccessCodeSwitcherChanged(state: AnalyticsParam.OnOffState) : CardSettings(
            event = "Save Access Code Switcher Changed",
            params = mapOf("State" to state.value),
        )

        object ButtonEnableBiometricAuthentication : AppSettings("Button - Enable Biometric Authentication")
    }
}
