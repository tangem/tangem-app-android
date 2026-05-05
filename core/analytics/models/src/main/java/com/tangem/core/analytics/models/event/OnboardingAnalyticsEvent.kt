package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.*

sealed class OnboardingAnalyticsEvent(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    sealed class Onboarding(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingAnalyticsEvent(category = "Onboarding", event = event, params = params) {

        /**
         * Tracks the start of the onboarding process.
         */
        class Started(
            source: AnalyticsParam.ScreensSources? = null,
        ) : Onboarding(
            event = "Onboarding Started",
            params = buildMap {
                source?.value?.let { put(AnalyticsParam.SOURCE, it) }
            },
        ), CriticalEvent

        /**
         * Tracks the completion of the onboarding process.
         */
        class Finished(
            source: AnalyticsParam.ScreensSources? = null,
        ) : Onboarding(
            event = "Onboarding Finished",
            params = buildMap {
                source?.value?.let { put(AnalyticsParam.SOURCE, it) }
            },
        ), CriticalEvent

        class ButtonMobileWallet(
            source: String,
        ) : Onboarding(
            event = "Button - Mobile Wallet",
            params = mapOf(
                AnalyticsParam.SOURCE to source,
            ),
        )
    }

    sealed class CreateWallet(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingAnalyticsEvent(category = "Onboarding / Create Wallet", event = event, params = params) {

        /**
         * Tracks opening of the create wallet screen.
         */
        class ScreenOpened : CreateWallet("Create Wallet Screen Opened"), CriticalEvent

        /**
         * Tracks the user clicking the "Create Wallet" button.
         */
        class ButtonCreateWallet : CreateWallet("Button - Create Wallet"), CriticalEvent

        /**
         * Tracks the user clicking the "Other Options" button on the create wallet screen.
         */
        class ButtonOtherOptions : CreateWallet("Button - Other Options"), CriticalEvent

        /**
         * Tracks successful wallet creation, either on a Tangem card or as a mobile wallet.
         */
        class WalletCreatedSuccessfully(
            creationType: AnalyticsParam.WalletCreationType = AnalyticsParam.WalletCreationType.PrivateKey,
            seedPhraseLength: Int? = null,
            passPhraseState: AnalyticsParam.EmptyFull,
            referralId: String?,
            source: AnalyticsParam.ScreensSources? = null,
        ) : CreateWallet(
            event = "Wallet Created Successfully",
            params = buildMap {
                put("Creation Type", creationType.value)
                put("Passphrase", passPhraseState.value)
                if (seedPhraseLength != null) {
                    put("Seed Phrase Length", seedPhraseLength.toString())
                }
                source?.value?.let { put(AnalyticsParam.SOURCE, it) }
                putAll(getReferralParams(referralId))
            },
        ), AppsFlyerIncludedEvent, CriticalEvent
    }

    sealed class SeedPhrase(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingAnalyticsEvent(category = "Onboarding / Seed Phrase", event = event, params = params) {

        class CreateMobileScreenOpened(
            source: AnalyticsParam.ScreensSources,
        ) : SeedPhrase(
            event = "Create Mobile Screen Opened",
            params = mapOf(
                AnalyticsParam.SOURCE to source.value,
            ),
        ), AppsFlyerIncludedEvent

        class ButtonImportWallet : SeedPhrase("Button - Import Wallet")
        /**
         * Tracks opening of the seed phrase import screen.
         */
        class ImportSeedPhraseScreenOpened : SeedPhrase("Import Seed Phrase Screen Opened"), CriticalEvent
        class ButtonImport : SeedPhrase("Button - Import")
    }

    sealed class Error(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingAnalyticsEvent(category = "Error", event = event, params = params) {

        data class OfflineAttestationFailed(
            val source: AnalyticsParam.ScreensSources,
        ) : Error(
            event = "Offline Attestation Failed",
            params = mapOf(AnalyticsParam.SOURCE to source.value),
        )
    }
}