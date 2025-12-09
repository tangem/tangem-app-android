package com.tangem.features.onboarding.v2.common.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

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

        class WalletCreatedSuccessfullyAppsFlyer(
            creationType: WalletCreationType = WalletCreationType.PrivateKey,
            seedPhraseLength: Int? = null,
            hasPassphrase: Boolean,
        ) : CreateWallet(
            event = "wallet_created_successfully",
            params = buildMap {
                put("Creation Type", creationType.value)
                if (seedPhraseLength != null) {
                    put("Seed Phrase Length", seedPhraseLength.toString())
                }
                put("passphrase", if (hasPassphrase) "Full" else "Empty")
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

        data object ResumeInterruptedBackup : Backup(
            event = "Notice - Backup Canceled",
            params = mapOf("Action" to "Resume"),
        )

        data object CancelInterruptedBackup : Backup(
            event = "Notice - Backup Canceled",
            params = mapOf("Action" to "Cancel"),
        )
    }

    sealed class Twins(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Twins", event, params) {

        data object ScreenOpened : Twins("Twinning Screen Opened")
        data object SetupFinished : Twins("Twin Setup Finished")
    }

    data class OfflineAttestationFailed(
        val source: AnalyticsParam.ScreensSources,
    ) : OnboardingEvent(
        category = "Error",
        event = "Offline Attestation Failed",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )
}