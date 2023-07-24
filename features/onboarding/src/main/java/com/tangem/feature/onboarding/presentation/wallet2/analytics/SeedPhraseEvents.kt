package com.tangem.feature.onboarding.presentation.wallet2.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class SeedPhraseEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(ONBOARDING_SEED_PHRASE, event, params) {

    object ButtonGenerateSeedPhrase : SeedPhraseEvents("Button - Generate Seed Phrase")

    object ButtonImportWallet : SeedPhraseEvents("Button - Import Wallet")
    object ButtonReadMore : SeedPhraseEvents("Button - Read More")
    object ButtonImport : SeedPhraseEvents("Button - Import")

    object IntroScreenOpened : SeedPhraseEvents("Seed Intro Screen Opened")
    object GenerationScreenOpened : SeedPhraseEvents("Seed Generation Screen Opened")
    object CheckingScreenOpened : SeedPhraseEvents("Seed Checking Screen Opened")
    object ImportScreenOpened : SeedPhraseEvents("Import Seed Phrase Screen Opened")

    companion object {
        const val ONBOARDING_SEED_PHRASE = "Onboarding / Seed Phrase"
    }
}

object OnboardingSeedButtonOtherOptions : AnalyticsEvent(
    category = "Onboarding / Create Wallet",
    event = "Button - Other Options",
)

enum class SeedPhraseSource {
    IMPORTED, GENERATED
}