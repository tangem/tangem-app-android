package com.tangem.features.collectibles.impl.onboarding.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_24
import com.tangem.core.ui.res.generated.icons.ic_wallet_24
import com.tangem.features.collectibles.impl.onboarding.ui.state.CollectiblesOnboardingUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

/**
 * Builds the Onboarding screen's initial state.
 *
 * The copy is hardcoded here, verbatim from the design, until the keys move to Lokalise — one place to edit,
 * instead of a spread across the composables.
 *
 * The legal line itself is assembled in the footer from `prediction_onboarding_legal`, which still names
 * Predictions: it is the design's own line, copied from Polymarket, and stays until design supplies this
 * screen's own wording.
 */
internal class CollectiblesOnboardingUMConverter :
    Converter<CollectiblesOnboardingUMConverter.Callbacks, CollectiblesOnboardingUM> {

    override fun convert(value: Callbacks): CollectiblesOnboardingUM {
        return CollectiblesOnboardingUM(
            title = stringReference(TITLE),
            subtitle = stringReference(SUBTITLE),
            benefits = persistentListOf(
                CollectiblesOnboardingUM.Benefit(
                    icon = Icons.ic_lightning_24,
                    title = stringReference("Tapless pack opening"),
                    subtitle = stringReference("No card scan needed to open"),
                ),
                CollectiblesOnboardingUM.Benefit(
                    icon = Icons.ic_shield_checkmark_24,
                    title = stringReference("Your keys, funds and cards"),
                    subtitle = stringReference("Only your card moves them out"),
                ),
                CollectiblesOnboardingUM.Benefit(
                    icon = Icons.ic_shield_checkmark_24,
                    title = stringReference("Transparency"),
                    subtitle = stringReference("Every card in the machine, plus recent openings"),
                ),
                CollectiblesOnboardingUM.Benefit(
                    icon = Icons.ic_wallet_24,
                    title = stringReference("Instant offers"),
                    subtitle = stringReference("85–90% of insured value, in USDC"),
                ),
            ),
            faq = persistentListOf(
                CollectiblesOnboardingUM.FaqEntry(
                    question = stringReference("What happens when I tap my card?"),
                    answer = stringReference(FAQ_TAP_ANSWER),
                ),
                CollectiblesOnboardingUM.FaqEntry(
                    question = stringReference("What are the risks?"),
                    answer = stringReference(FAQ_RISKS_ANSWER),
                ),
                CollectiblesOnboardingUM.FaqEntry(
                    question = stringReference("Who provides the service?"),
                    answer = stringReference(FAQ_PROVIDER_ANSWER),
                ),
                CollectiblesOnboardingUM.FaqEntry(
                    question = stringReference("Is it available where I live?"),
                    answer = stringReference(FAQ_AVAILABILITY_ANSWER),
                ),
            ),
            collectiblesTitle = stringReference(COLLECTIBLES_TERMS_TITLE),
            tangemTitle = resourceReference(R.string.prediction_onboarding_legal_tangem_terms),
            createAccountButtonText = stringReference("Create Gacha account"),
            isCreatingAccount = false,
            onCloseClick = value.onCloseClick,
            onCreateAccountClick = value.onCreateAccountClick,
            onCollectiblesTermsClick = value.onCollectiblesTermsClick,
            onTangemTermsClick = value.onTangemTermsClick,
        )
    }

    data class Callbacks(
        val onCloseClick: () -> Unit,
        val onCreateAccountClick: () -> Unit,
        val onCollectiblesTermsClick: () -> Unit,
        val onTangemTermsClick: () -> Unit,
    )

    private companion object {

        const val TITLE = "Open Gacha account"

        const val SUBTITLE = "Buy sealed packs of real graded collectible cards — Pokémon, One Piece, sports. " +
            "Whatever you open is yours: keep it in the insured physical vault, take the instant offer, " +
            "have the slab shipped to you, or send it to a friend as a gift"

        const val COLLECTIBLES_TERMS_TITLE = "Collector Crypt T&Cs"

        const val FAQ_TAP_ANSWER = "A sealed pack containing one graded physical collectible card, held 1:1 " +
            "in an insured physical vault and redeemable for delivery at any time. Every card inside a machine " +
            "is transparently visible in the app and the draw odds are published up front"

        const val FAQ_RISKS_ANSWER = "Collectible values change and can fall. Some cards in a pack pool can be " +
            "worth less than the pack price — never nothing, but less than you paid. Only spend what you're " +
            "comfortable spending on collectibles. This is not financial advice"

        const val FAQ_PROVIDER_ANSWER = "Collector Crypt provides the card inventory, the packs, the physical " +
            "vault, the instant offers and the shipping, under its own terms. Tangem provides a passive " +
            "interface for accessing that service and for signing — it never holds your cards, your funds or " +
            "your keys"

        const val FAQ_AVAILABILITY_ANSWER = "Availability varies by region and some countries are restricted. " +
            "Age limits may apply. You're responsible for following the laws and regulations of your own " +
            "jurisdiction"
    }
}