package com.tangem.tap.common.analytics.events

/**
 * Created by Anton Zhilenkov on 28.09.2022.
 */
sealed class Onboarding(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    class Started : Onboarding("Onboarding", "Onboarding Started")
    class Finished : Onboarding("Onboarding", "Onboarding Finished")

    sealed class CreateWallet(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Onboarding("Onboarding / Create Wallet", event, params) {

        class ScreenOpened : CreateWallet("Create Wallet Screen Opened")
        class ButtonCreateWallet : CreateWallet("Button - Create Wallet")
        class WalletCreatedSuccessfully : CreateWallet("Wallet Created Successfully")
    }

    sealed class Backup(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Onboarding("Onboarding / Backup", event, params) {

        class ScreenOpened : Backup("Backup Screen Opened")
        class Started : Backup("Backup Started")
        class Skipped : Backup("Backup Skipped")
        class SettingAccessCodeStarted : Backup("Setting Access Code Started")
        class AccessCodeEntered : Backup("Access Code Entered")
        class AccessCodeReEntered : Backup("Access Code Re-entered")

        class Finished(cardsCount: Int) : Backup(
            event = "Backup Finished",
            params = mapOf("Cards count" to "$cardsCount"),
        )
    }

    sealed class Topup(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Onboarding("Onboarding / Top Up", event, params) {

        class ScreenOpened : Topup("Activation Screen Opened")

        class ButtonBuyCrypto(currency: AnalyticsParam.CurrencyType) : Topup(
            event = "Button - Buy Crypto",
            params = mapOf("Currency" to currency.value),
        )

        class ButtonShowWalletAddress : Topup("Button - Show the Wallet Address")
    }

    sealed class Twins(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Onboarding("Onboarding / Twins", event, params) {

        class ScreenOpened : Twins("Twinning Screen Opened")
        class SetupStarted : Twins("Twin Setup Started")
        class SetupFinished : Twins("Twin Setup Finished")
    }

    class PinCodeSet : Onboarding("Onboarding", "PIN code set")
    class ButtonConnect : Onboarding("Onboarding", "Button - Connect")
    class KYCStarted : Onboarding("Onboarding", "KYC started")
    class KYCInProgress : Onboarding("Onboarding", "KYC in progress")
    class KYCRejected : Onboarding("Onboarding", "KYC rejected")
    class ClaimScreenOpened : Onboarding("Onboarding", "Claim screen opened")
    class ButtonClaim : Onboarding("Onboarding", "Button - Claim")
    class ClaimWasSuccessfully : Onboarding("Onboarding", "Claim was successfully")
    class EnableBiometrics(state: AnalyticsParam.OnOffState) : Onboarding(
        category = "Onboarding / Biometric",
        event = "Enable Biometric",
        params = mapOf("State" to state.value),
    )
}
