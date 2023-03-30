package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.AnalyticsEvent

/**
* [REDACTED_AUTHOR]
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
            params = mapOf(AnalyticsParam.Currency to currency.value),
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

    class PinScreenOpened : Onboarding("Onboarding", "PIN screen opened")
    class ButtonSetPinCode : Onboarding("Onboarding", "Button - Set PIN Code")
    class CardConnectionScreenOpened : Onboarding("Onboarding", "Card Connection Screen Opened")
    class ButtonConnect : Onboarding("Onboarding", "Button - Connect")
    class PinCodeSet : Onboarding("Onboarding", "PIN Code Set")

    class KYCScreenOpened : Onboarding("Onboarding", "KYC screen opened")
    class KYCStarted : Onboarding("Onboarding", "KYC Started")
    class KYCInProgress : Onboarding("Onboarding", "KYC In Progress")
    class KYCRejected : Onboarding("Onboarding", "KYC Rejected")

    class ClaimScreenOpened : Onboarding("Onboarding", "Claim Screen Opened")
    class ButtonClaim : Onboarding("Onboarding", "Button - Claim")
    class ClaimWasSuccessfully : Onboarding("Onboarding", "Claim Was Successfully")

    class ButtonChat : Onboarding("Onboarding", "Button - Chat")

    class NotEnoughGasError : Onboarding("Onboarding", "Not Enough Gas Error")
    class CardNotPassedError : Onboarding("Onboarding", "Card Not Passed Error")

    class EnableBiometrics(state: AnalyticsParam.OnOffState) : Onboarding(
        category = "Onboarding / Biometric",
        event = "Enable Biometric",
        params = mapOf("State" to state.value),
    )
}
