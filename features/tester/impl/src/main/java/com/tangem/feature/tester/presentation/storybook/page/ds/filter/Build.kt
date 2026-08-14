package com.tangem.feature.tester.presentation.storybook.page.ds.filter

import com.tangem.core.ui.ds2.filter.TangemFilterItem
import com.tangem.feature.tester.presentation.storybook.entity.TangemFilterGroupStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet

internal fun StateUpdater<TangemFilterGroupStory>.build(): TangemFilterGroupStory {
    return TangemFilterGroupStory(
        variant = TangemFilterItem.Variant.Material,
        background = TangemFilterGroupStory.Background.Rainbow,
        activeFilterIds = persistentSetOf(DEMO_FILTERS.first().id),
        hasCounter = false,
        isLoading = false,
        isBlurEnabled = true,
        textScale = 1f,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onBackgroundChange = { background -> updateStory { it.copy(background = background) } },
        // Clicking a chip stands in for picking a value from the options list.
        onFilterClick = { id ->
            updateStory { story ->
                story.copy(activeFilterIds = (story.activeFilterIds + id).toPersistentSet())
            }
        },
        onFilterClear = { id ->
            updateStory { story ->
                story.copy(activeFilterIds = (story.activeFilterIds - id).toPersistentSet())
            }
        },
        onCounterToggle = { updateStory { it.copy(hasCounter = !it.hasCounter) } },
        onLoadingToggle = { updateStory { it.copy(isLoading = !it.isLoading) } },
        onBlurToggle = { updateStory { it.copy(isBlurEnabled = !it.isBlurEnabled) } },
        onTextScaleChange = { scale -> updateStory { it.copy(textScale = scale) } },
    )
}

internal val tangemFilterGroupStoryFactory
    get() = storyPageFactory(StateUpdater<TangemFilterGroupStory>::build)