package com.tangem.feature.tester.presentation.storybook.page.ds.loader

import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.feature.tester.presentation.storybook.entity.TangemLoaderStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemLoaderStory>.build(): TangemLoaderStory {
    return TangemLoaderStory(
        selectedSize = TangemLoaderSize.X24,
        onSizeChange = { size ->
            updateStory { it.copy(selectedSize = size) }
        },
    )
}

internal val tangemLoaderStoryFactory
    get() = storyPageFactory(StateUpdater<TangemLoaderStory>::build)