package com.tangem.tap.common.analytics.events

/**
* [REDACTED_AUTHOR]
 */
sealed class Settings(
    category: String = "Settings",
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    class ScreenOpened : Settings(event = "Settings Screen Opened")
    class ButtonChat : Settings(event = "Button - Chat")
    class ButtonSendFeedback : Settings(event = "Button - Send Feedback")
    class ButtonStartWalletConnectSession : Settings(event = "Button - Start Wallet Connect Session")
    class ButtonStopWalletConnectSession : Settings(event = "Button - Stop Wallet Connect Session")
    class ButtonCardSettings : Settings(event = "Button - Card Settings")
    class ButtonAppSettings : Settings(event = "Button - App Settings")
    class ButtonCreateBackup : Settings(event = "Button - Create Backup")

    sealed class ButtonSocialNetwork(network: AnalyticsParam.SocialNetwork) : Settings(
        event = "Button - Social Network",
        params = mapOf("Network" to network.value),
    )

    sealed class CardSettings(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Onboarding("Settings / Card Settings", event, params) {

        class ButtonFactoryReset : CardSettings("Button - Factory Reset")
        class FactoryResetFinished : CardSettings("Factory Reset Finished")
        class UserCodeChanged : CardSettings("User Code Changed")
        class ButtonChangeSecurityMode : CardSettings("Button - Change Security Mode")

        sealed class ButtonChangeUserCode(type: AnalyticsParam.UserCode) : CardSettings(
            event = "Button - Change User Code",
            params = mapOf("Type" to type.value),
        )

        sealed class SecurityModeChanged(mode: AnalyticsParam.SecurityMode) : CardSettings(
            event = "Security Mode Changed",
            params = mapOf("Mode" to mode.value),
        )
    }

    sealed class AppSettings(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Onboarding("Settings / App Settings", event, params) {

        sealed class FaceIDSwitcherChanged(state: AnalyticsParam.OnOffState) : CardSettings(
            event = "Face ID Switcher Changed",
            params = mapOf("State" to state.value),
        )

        sealed class SaveAccessCodeSwitcherChanged(state: AnalyticsParam.OnOffState) : CardSettings(
            event = "Save Access Code Switcher Changed",
            params = mapOf("State" to state.value),
        )

        class ButtonEnableBiometricAuthentication : AppSettings("Button - Enable Biometric Authentication")
    }
}
