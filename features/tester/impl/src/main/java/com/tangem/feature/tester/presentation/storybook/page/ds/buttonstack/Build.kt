package com.tangem.feature.tester.presentation.storybook.page.ds.buttonstack

import com.tangem.core.ui.ds2.button.TangemButtonStack
import com.tangem.feature.tester.presentation.storybook.entity.TangemButtonStackStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemButtonStackStory>.build(): TangemButtonStackStory {
    return TangemButtonStackStory(
        variant = TangemButtonStack.Variant.Material,
        entryCount = 3,
        entryKind = TangemButtonStackStory.EntryKind.Icon,
        background = TangemButtonStackStory.Background.Rainbow,
        isBlurEnabled = true,
        onVariantChange = { variant ->
            updateStory { it.copy(variant = variant) }
        },
        onEntryCountChange = { count ->
            updateStory { it.copy(entryCount = count) }
        },
        onEntryKindChange = { kind ->
            updateStory { it.copy(entryKind = kind) }
        },
        onBackgroundChange = { background ->
            updateStory { it.copy(background = background) }
        },
        onBlurToggle = {
            updateStory { it.copy(isBlurEnabled = !it.isBlurEnabled) }
        },
    )
}

internal val tangemButtonStackStoryFactory
    get() = storyPageFactory(StateUpdater<TangemButtonStackStory>::build)