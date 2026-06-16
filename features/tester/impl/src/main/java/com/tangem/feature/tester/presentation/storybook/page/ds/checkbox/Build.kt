package com.tangem.feature.tester.presentation.storybook.page.ds.checkbox

import androidx.compose.ui.state.ToggleableState
import com.tangem.feature.tester.presentation.storybook.entity.TangemCheckboxV2Story
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemCheckboxV2Story>.build(): TangemCheckboxV2Story {
    return TangemCheckboxV2Story(
        state = ToggleableState.Off,
        isEnabled = true,
        onStateChange = { state ->
            updateStory { it.copy(state = state) }
        },
        onEnabledToggle = {
            updateStory { it.copy(isEnabled = !it.isEnabled) }
        },
    )
}

internal val tangemCheckboxV2StoryFactory
    get() = storyPageFactory(StateUpdater<TangemCheckboxV2Story>::build)