@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.topbar

import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.feature.tester.presentation.storybook.entity.TangemTopBarStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemTopBarStory>.build(): TangemTopBarStory {
    return TangemTopBarStory(
        selectedType = TangemTopBarType.Default,
        onTypeChange = { type ->
            updateStory { it.copy(selectedType = type) }
        },
    )
}

internal val tangemTopBarStoryFactory
    get() = storyPageFactory(StateUpdater<TangemTopBarStory>::build)