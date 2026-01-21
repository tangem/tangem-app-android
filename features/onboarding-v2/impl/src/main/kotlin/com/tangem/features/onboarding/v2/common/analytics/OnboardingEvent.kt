package com.tangem.features.onboarding.v2.common.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent

sealed class OnboardingEvent(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    class Started : OnboardingEvent("Onboarding", "Onboarding Started")
    class Finished : OnboardingEvent("Onboarding", "Onboarding Finished")

    sealed class CreateWallet(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Create Wallet", event, params) {

        class ScreenOpened : CreateWallet("Create Wallet Screen Opened")
        class ButtonCreateWallet : CreateWallet("Button - Create Wallet")
        class ButtonOtherOptions : CreateWallet("Button - Other Options")
        class WalletCreatedSuccessfully(
            creationType: WalletCreationType = WalletCreationType.PrivateKey,
            seedPhraseLength: Int? = null,
            passPhraseState: AnalyticsParam.EmptyFull,
        ) : CreateWallet(
            event = "Wallet Created Successfully",
            params = buildMap {
                put("Creation Type", creationType.value)
                put("Passphrase", passPhraseState.value)
                if (seedPhraseLength != null) {
                    put("Seed Phrase Length", seedPhraseLength.toString())
                }
            },
        ), AppsFlyerIncludedEvent

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

        class ResetCancelEvent : Backup(
            event = "Reset Card Notification",
            params = mapOf("Option" to "Cancel"),
        )

        class ResetPerformEvent : Backup(
            event = "Reset Card Notification",
            params = mapOf("Option" to "Reset"),
        )

        class ResumeInterruptedBackup : Backup(
            event = "Notice - Backup Canceled",
            params = mapOf("Action" to "Resume"),
        )

        class CancelInterruptedBackup : Backup(
            event = "Notice - Backup Canceled",
            params = mapOf("Action" to "Cancel"),
        )
    }

    sealed class Twins(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Twins", event, params) {

        class ScreenOpened : Twins("Twinning Screen Opened")
        class SetupFinished : Twins("Twin Setup Finished")
    }

    data class OfflineAttestationFailed(
        val source: AnalyticsParam.ScreensSources,
    ) : OnboardingEvent(
        category = "Error",
        event = "Offline Attestation Failed",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )
}