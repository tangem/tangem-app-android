package com.tangem.features.collectibles.impl.stories.ui.preview

import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.stories.ui.state.CollectiblesStoriesUM
import com.tangem.features.collectibles.impl.stories.ui.state.CollectiblesStorySlideUM
import kotlinx.collections.immutable.persistentListOf

/**
 * Fixtures for the stories `@Preview`s. Not used in production code.
 */
internal object CollectiblesStoriesPreviewData {

    val state: CollectiblesStoriesUM = CollectiblesStoriesUM(
        slides = persistentListOf(
            CollectiblesStorySlideUM(
                title = stringReference("Digital packs.\nPhysical collectibles."),
                subtitle = stringReference(
                    "Every card is professionally graded, so authenticity is guaranteed — and each one is " +
                        "backed 1:1 by the physical slab in an insured physical vault",
                ),
                continueButtonText = stringReference("Next"),
            ),
            CollectiblesStorySlideUM(
                title = stringReference("The card is yours, either way"),
                subtitle = stringReference(
                    "Leave it in the physical vault, have it delivered to your address, or send it to a " +
                        "friend as a gift — the slab is redeemable whenever you want it",
                ),
                continueButtonText = stringReference("Browse packs"),
            ),
        ),
        currentIndex = 0,
        legal = stringReference(
            "Gacha is provided by Collector Crypt under its own T&Cs. Tangem is the interface provider. " +
                "Card values change over time.",
        ),
        onNextSlideClick = {},
        onPreviousSlideClick = {},
        onContinueClick = {},
        onCloseClick = {},
    )

    val lastSlideState: CollectiblesStoriesUM = state.copy(currentIndex = state.slides.lastIndex)
}