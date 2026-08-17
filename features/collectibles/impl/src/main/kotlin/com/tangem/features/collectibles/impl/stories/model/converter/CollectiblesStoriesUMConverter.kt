package com.tangem.features.collectibles.impl.stories.model.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.stories.ui.state.CollectiblesStoriesUM
import com.tangem.features.collectibles.impl.stories.ui.state.CollectiblesStorySlideUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

/**
 * Builds the stories screen's initial state.
 *
 * The copy is hardcoded here, verbatim from the design, until the keys move to Lokalise — the same approach as
 * [com.tangem.features.collectibles.impl.onboarding.model.converter.CollectiblesOnboardingUMConverter].
 *
 */
internal class CollectiblesStoriesUMConverter :
    Converter<CollectiblesStoriesUMConverter.Callbacks, CollectiblesStoriesUM> {

    override fun convert(value: Callbacks): CollectiblesStoriesUM {
        return CollectiblesStoriesUM(
            slides = persistentListOf(
                slide(title = FIRST_TITLE, subtitle = FIRST_SUBTITLE),
                slide(title = SECOND_TITLE, subtitle = SECOND_SUBTITLE),
                slide(title = THIRD_TITLE, subtitle = THIRD_SUBTITLE),
                slide(title = FOURTH_TITLE, subtitle = FOURTH_SUBTITLE),
                slide(title = FIFTH_TITLE, subtitle = FIFTH_SUBTITLE, buttonText = BROWSE_PACKS_BUTTON),
            ),
            currentIndex = 0,
            legal = stringReference(LEGAL),
            onNextSlideClick = value.onNextSlideClick,
            onPreviousSlideClick = value.onPreviousSlideClick,
            onContinueClick = value.onContinueClick,
            onCloseClick = value.onCloseClick,
        )
    }

    private fun slide(title: String, subtitle: String, buttonText: String = NEXT_BUTTON) = CollectiblesStorySlideUM(
        title = stringReference(title),
        subtitle = stringReference(subtitle),
        continueButtonText = stringReference(buttonText),
    )

    data class Callbacks(
        val onNextSlideClick: () -> Unit,
        val onPreviousSlideClick: () -> Unit,
        val onContinueClick: () -> Unit,
        val onCloseClick: () -> Unit,
    )

    private companion object {

        const val NEXT_BUTTON = "Next"
        const val BROWSE_PACKS_BUTTON = "Browse packs"

        const val LEGAL = "Gacha is provided by Collector Crypt under its own T&Cs. Tangem is the " +
            "interface provider. Card values change over time."

        const val FIRST_TITLE = "Digital packs.\nPhysical collectibles."
        const val FIRST_SUBTITLE = "Every card is professionally graded, so authenticity is guaranteed — " +
            "and each one is backed 1:1 by the physical slab in an insured physical vault"

        const val SECOND_TITLE = "High security, tapless design"
        const val SECOND_SUBTITLE = "One tap sets up your Solana smart contract, owned by your cold wallet. " +
            "Collect tapless; every withdrawal still requires your card"

        const val THIRD_TITLE = "There's always a buyer"
        const val THIRD_SUBTITLE = "Get 85–93% of your card's insured value. Accept for USDC in Gacha, " +
            "or tap to withdraw to your cold wallet"

        const val FOURTH_TITLE = "Nothing behind the curtain"
        const val FOURTH_SUBTITLE = "Browse every card before you buy and watch live openings from the " +
            "blockchain, with each collector's wallet and card value. All in-app"

        const val FIFTH_TITLE = "The card is yours, either way"
        const val FIFTH_SUBTITLE = "Leave it in the physical vault, have it delivered to your address, " +
            "or send it to a friend as a gift — the slab is redeemable whenever you want it"
    }
}