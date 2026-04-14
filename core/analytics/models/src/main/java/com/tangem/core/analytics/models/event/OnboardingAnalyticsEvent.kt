package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.CriticalEvent
import com.tangem.core.analytics.models.getReferralParams

sealed class OnboardingAnalyticsEvent(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    sealed class Onboarding(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingAnalyticsEvent(category = "Onboarding", event = event, params = params) {

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

        class ButtonCreateWallet : CreateWallet("Button - Create Wallet")

        class WalletCreatedSuccessfully(
            creationType: WalletCreationType = WalletCreationType.PrivateKey,
            seedPhraseLength: Int? = null,
            passPhraseState: AnalyticsParam.EmptyFull,
            referralId: String?,
            source: String? = null,
        ) : CreateWallet(
            event = "Wallet Created Successfully",
            params = buildMap {
                put("Creation Type", creationType.value)
                put("Passphrase", passPhraseState.value)
                if (seedPhraseLength != null) {
                    put("Seed Phrase Length", seedPhraseLength.toString())
                }
                source?.let { put(AnalyticsParam.SOURCE, it) }
                putAll(getReferralParams(referralId))
            },
        ), AppsFlyerIncludedEvent, CriticalEvent

        sealed class WalletCreationType(val value: String) {
            data object PrivateKey : WalletCreationType(value = "Private Key")
            data object NewSeed : WalletCreationType(value = "New Seed")
            data object SeedImport : WalletCreationType(value = "Seed Import")
        }
    }

    sealed class SeedPhrase(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingAnalyticsEvent(category = "Onboarding / Seed Phrase", event = event, params = params) {

        class CreateMobileScreenOpened(
            source: String,
        ) : SeedPhrase(
            event = "Create Mobile Screen Opened",
            params = mapOf(
                AnalyticsParam.SOURCE to source,
            ),
        ), AppsFlyerIncludedEvent

        class ButtonImportWallet : SeedPhrase("Button - Import Wallet")
        class ImportSeedPhraseScreenOpened : SeedPhrase("Import Seed Phrase Screen Opened"), CriticalEvent
        class ButtonImport : SeedPhrase("Button - Import"), CriticalEvent
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