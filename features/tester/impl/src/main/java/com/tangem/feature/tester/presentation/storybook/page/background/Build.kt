@file:Suppress("MagicNumber", "LongMethod")
package com.tangem.feature.tester.presentation.storybook.page.background

import com.tangem.feature.tester.presentation.storybook.entity.NorthernLightsStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<NorthernLightsStory>.build(): NorthernLightsStory {
    return NorthernLightsStory(
        variant = NorthernLightsStory.Variant.Shader,
        onVariantChange = { newVariant ->
            updateStory { currentState ->
                currentState.copy(variant = newVariant)
            }
        },
    )
}

internal val northernLightsStoryFactory
    get() = storyPageFactory(StateUpdater<NorthernLightsStory>::build)