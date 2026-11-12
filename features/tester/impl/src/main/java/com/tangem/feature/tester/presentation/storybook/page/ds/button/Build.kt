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
        hasLoadingText = false,
        isEnabled = true,
        isHoldToConfirm = false,
        hasIconStart = false,
        hasIconEnd = false,
        hasText = true,
        isBlurEnabled = true,
        textScale = 1f,
        onVariantChange = { variant ->
            updateStory { story ->
                story.copy(
                    variant = variant,
                    isLoading = story.isLoading && !story.isHoldToConfirm,
                )
            }
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
        onLoadingTextToggle = {
            updateStory { it.copy(hasLoadingText = !it.hasLoadingText) }
        },
        onEnabledToggle = {
            updateStory { it.copy(isEnabled = !it.isEnabled) }
        },
        onHoldToConfirmToggle = {
            updateStory { story ->
                val isHoldToConfirm = !story.isHoldToConfirm
                story.copy(
                    isHoldToConfirm = isHoldToConfirm,
                    isLoading = story.isLoading && isHoldToConfirm,
                )
            }
        },
        onHoldConfirm = {
            updateStory { it.copy(isLoading = true) }
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