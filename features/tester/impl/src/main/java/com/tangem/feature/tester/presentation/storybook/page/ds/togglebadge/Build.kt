package com.tangem.feature.tester.presentation.storybook.page.ds.togglebadge

import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.feature.tester.presentation.storybook.entity.TangemToggleBadgeStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemToggleBadgeStory>.build(): TangemToggleBadgeStory {
    return TangemToggleBadgeStory(
        isSelected = false,
        isEnabled = true,
        size = TangemBadge.Size.X9,
        hasIconStart = true,
        hasIconEnd = false,
        textScale = 1f,
        onSelectedChange = { isSelected ->
            updateStory { it.copy(isSelected = isSelected) }
        },
        onEnabledToggle = {
            updateStory { it.copy(isEnabled = !it.isEnabled) }
        },
        onSizeChange = { size ->
            updateStory { it.copy(size = size) }
        },
        onIconStartToggle = {
            updateStory { it.copy(hasIconStart = !it.hasIconStart) }
        },
        onIconEndToggle = {
            updateStory { it.copy(hasIconEnd = !it.hasIconEnd) }
        },
        onTextScaleChange = { scale ->
            updateStory { it.copy(textScale = scale) }
        },
    )
}

internal val tangemToggleBadgeStoryFactory
    get() = storyPageFactory(StateUpdater<TangemToggleBadgeStory>::build)