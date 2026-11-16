package com.tangem.feature.tester.presentation.storybook.page.ds.menu

import com.tangem.core.ui.ds2.menu.TangemMenu
import com.tangem.feature.tester.presentation.storybook.entity.TangemMenuStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemMenuStory>.build(): TangemMenuStory {
    return TangemMenuStory(
        variant = TangemMenu.Variant.Button,
        isExpanded = false,
        isSelectable = false,
        selectedIndex = 0,
        hasIconStart = true,
        hasIconEnd = false,
        hasDescription = false,
        hasDivider = true,
        hasDividerTitle = true,
        lastItemVariant = TangemMenu.ItemVariant.Error,
        isLastItemEnabled = true,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onExpandedChange = { isExpanded -> updateStory { it.copy(isExpanded = isExpanded) } },
        onItemSelect = { index -> updateStory { it.copy(selectedIndex = index) } },
        onSelectableToggle = { updateStory { it.copy(isSelectable = !it.isSelectable) } },
        onIconStartToggle = { updateStory { it.copy(hasIconStart = !it.hasIconStart) } },
        onIconEndToggle = { updateStory { it.copy(hasIconEnd = !it.hasIconEnd) } },
        onDescriptionToggle = { updateStory { it.copy(hasDescription = !it.hasDescription) } },
        onDividerToggle = { updateStory { it.copy(hasDivider = !it.hasDivider) } },
        onDividerTitleToggle = { updateStory { it.copy(hasDividerTitle = !it.hasDividerTitle) } },
        onLastItemVariantChange = { variant -> updateStory { it.copy(lastItemVariant = variant) } },
        onLastItemEnabledToggle = { updateStory { it.copy(isLastItemEnabled = !it.isLastItemEnabled) } },
    )
}

internal val tangemMenuStoryFactory
    get() = storyPageFactory(StateUpdater<TangemMenuStory>::build)