package com.tangem.feature.tester.presentation.storybook.page.ds.button

import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.feature.tester.presentation.storybook.entity.TangemButtonStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemButtonStory>.build(): TangemButtonStory {
    return TangemButtonStory(
        variant = TangemButton.Variant.Primary,
        size = TangemButton.Size.X10,
        background = TangemButtonStory.Background.Rainbow,
        isLoading = false,
        isEnabled = true,
        hasIconStart = false,
        hasIconEnd = false,
        hasText = true,
        isBlurEnabled = true,
        textScale = 1f,
        onVariantChange = { variant ->
            updateStory { it.copy(variant = variant) }
        },
        onSizeChange = { size ->
            updateStory { it.copy(size = size) }
        },
        onBackgroundChange = { background ->
            updateStory { it.copy(background = background) }
        },
        onLoadingToggle = {
            updateStory { it.copy(isLoading = !it.isLoading) }
        },
        onEnabledToggle = {
            updateStory { it.copy(isEnabled = !it.isEnabled) }
        },
        onIconStartToggle = {
            updateStory { it.copy(hasIconStart = !it.hasIconStart) }
        },
        onIconEndToggle = {
            updateStory { it.copy(hasIconEnd = !it.hasIconEnd) }
        },
        onTextToggle = {
            updateStory { it.copy(hasText = !it.hasText) }
        },
        onBlurToggle = {
            updateStory { it.copy(isBlurEnabled = !it.isBlurEnabled) }
        },
        onTextScaleChange = { scale ->
            updateStory { it.copy(textScale = scale) }
        },
    )
}

internal val tangemButtonStoryFactory
    get() = storyPageFactory(StateUpdater<TangemButtonStory>::build)