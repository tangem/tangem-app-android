package com.tangem.feature.tester.presentation.storybook.page.ds.glowring

import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.feature.tester.presentation.storybook.entity.TangemGlowRingStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemGlowRingStory>.build(): TangemGlowRingStory {
    return TangemGlowRingStory(
        variant = TangemGlowRing.Variant.Magic,
        quality = TangemGlowRing.Quality.Auto,
        background = TangemGlowRingStory.Background.BgPrimary,
        isAnimated = true,
        onVariantChange = { variant ->
            updateStory { it.copy(variant = variant) }
        },
        onQualityChange = { quality ->
            updateStory { it.copy(quality = quality) }
        },
        onBackgroundChange = { background ->
            updateStory { it.copy(background = background) }
        },
        onAnimatedToggle = {
            updateStory { it.copy(isAnimated = !it.isAnimated) }
        },
    )
}

internal val tangemGlowRingStoryFactory
    get() = storyPageFactory(StateUpdater<TangemGlowRingStory>::build)