package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AppsFlyerEvent

sealed class OnboardingAnalyticsEvent(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    sealed class Onboarding(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingAnalyticsEvent(category = "Onboarding", event = event, params = params) {

        class AppsFlyerEntryScreenView : Onboarding(event = "wallet_entry_screen_view"), AppsFlyerEvent

        class Started(
            source: String,
        ) : Onboarding(
            event = "Onboarding Started",
            params = mapOf(
                AnalyticsParam.SOURCE to source,
            ),
        )

        class Finished(
            source: String,
        ) : Onboarding(
            event = "Onboarding Finished",
            params = mapOf(
                AnalyticsParam.SOURCE to source,
            ),
        )

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

        data object ButtonCreateWallet : CreateWallet("Button - Create Wallet")

        class WalletCreatedSuccessfully(
            source: String,
            creationType: WalletCreationType = WalletCreationType.NewSeed,
            seedPhraseLength: Int? = null,
            passPhraseState: AnalyticsParam.EmptyFull,
        ) : CreateWallet(
            event = "Wallet Created Successfully",
            params = buildMap {
                put(AnalyticsParam.SOURCE, source)
                put("Creation Type", creationType.value)
                put("Passphrase", passPhraseState.value)
                if (seedPhraseLength != null) {
                    put("Seed Phrase Length", seedPhraseLength.toString())
                }
            },
        )

        class AppsFlyerWalletCreatedSuccessfully(
            source: String,
            creationType: WalletCreationType = WalletCreationType.NewSeed,
            seedPhraseLength: Int? = null,
            passPhraseState: AnalyticsParam.EmptyFull,
        ) : CreateWallet(
            event = "wallet_created_successfully",
            params = buildMap {
                put(AnalyticsParam.SOURCE, source)
                put("Creation Type", creationType.value)
                put("Passphrase", passPhraseState.value)
                if (seedPhraseLength != null) {
                    put("Seed Phrase Length", seedPhraseLength.toString())
                }
            },
        )

        sealed class WalletCreationType(val value: String) {
            data object NewSeed : WalletCreationType(value = "New Seed")
            data object SeedImport : WalletCreationType(value = "Seed Import")
        }
    }

    sealed class SeedPhrase(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingAnalyticsEvent(category = "Onboarding / Seed Phrase", event = event, params = params) {

        data object CreateMobileScreenOpened : SeedPhrase("Create Mobile Screen Opened")
        data object ButtonImportWallet : SeedPhrase("Button - Import Wallet")
        data object ImportSeedPhraseScreenOpened : SeedPhrase("Import Seed Phrase Screen Opened")
        data object ButtonImport : SeedPhrase("Button - Import")
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