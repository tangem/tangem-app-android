package com.tangem.features.onboarding.v2.multiwallet.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class OnboardingEvent(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    data object Started : OnboardingEvent("Onboarding", "Onboarding Started")
    data object Finished : OnboardingEvent("Onboarding", "Onboarding Finished")

    sealed class CreateWallet(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Create Wallet", event, params) {

        data object ScreenOpened : CreateWallet("Create Wallet Screen Opened")
        data object ButtonCreateWallet : CreateWallet("Button - Create Wallet")
        class WalletCreatedSuccessfully(
            creationType: WalletCreationType = WalletCreationType.PrivateKey,
            seedPhraseLength: Int? = null,
        ) : CreateWallet(
            event = "Wallet Created Successfully",
            params = buildMap {
                put("Creation Type", creationType.value)
                if (seedPhraseLength != null) {
                    put("Seed Phrase Length", seedPhraseLength.toString())
                }
            },
        )

        sealed class WalletCreationType(val value: String) {
            data object PrivateKey : WalletCreationType(value = "Private Key")
            data object NewSeed : WalletCreationType(value = "New Seed")
            data object SeedImport : WalletCreationType(value = "Seed Import")
        }
    }

    sealed class Backup(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Backup", event, params) {

        data object ScreenOpened : Backup("Backup Screen Opened")
        data object Started : Backup("Backup Started")
        data object Skipped : Backup("Backup Skipped")
        data object SettingAccessCodeStarted : Backup("Setting Access Code Started")
        data object AccessCodeEntered : Backup("Access Code Entered")
        data object AccessCodeReEntered : Backup("Access Code Re-entered")

        class Finished(cardsCount: Int) : Backup(
            event = "Backup Finished",
            params = mapOf("Cards count" to "$cardsCount"),
        )

        data object ResetCancelEvent : Backup(
            event = "Reset Card Notification",
            params = mapOf("Option" to "Cancel"),
        )

        data object ResetPerformEvent : Backup(
            event = "Reset Card Notification",
            params = mapOf("Option" to "Reset"),
        )
    }
}
