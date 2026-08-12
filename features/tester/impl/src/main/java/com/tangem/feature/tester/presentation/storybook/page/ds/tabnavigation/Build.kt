package com.tangem.feature.tester.presentation.storybook.page.ds.tabnavigation

import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.feature.tester.presentation.storybook.entity.TangemTabNavigationStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemTabNavigationStory>.build(): TangemTabNavigationStory {
    return TangemTabNavigationStory(
        variant = TangemTabItem.Variant.Material,
        background = TangemTabNavigationStory.Background.Rainbow,
        theme = TangemTabNavigationStory.Theme.System,
        selectedTabId = DEMO_TABS.first().id,
        hasCounter = false,
        hasIcon = false,
        isLoading = false,
        isBlurEnabled = true,
        textScale = 1f,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onBackgroundChange = { background -> updateStory { it.copy(background = background) } },
        onThemeChange = { theme -> updateStory { it.copy(theme = theme) } },
        onTabClick = { id -> updateStory { it.copy(selectedTabId = id) } },
        onCounterToggle = { updateStory { it.copy(hasCounter = !it.hasCounter) } },
        onIconToggle = { updateStory { it.copy(hasIcon = !it.hasIcon) } },
        onLoadingToggle = { updateStory { it.copy(isLoading = !it.isLoading) } },
        onBlurToggle = { updateStory { it.copy(isBlurEnabled = !it.isBlurEnabled) } },
        onTextScaleChange = { scale -> updateStory { it.copy(textScale = scale) } },
    )
}

internal val tangemTabNavigationStoryFactory
    get() = storyPageFactory(StateUpdater<TangemTabNavigationStory>::build)