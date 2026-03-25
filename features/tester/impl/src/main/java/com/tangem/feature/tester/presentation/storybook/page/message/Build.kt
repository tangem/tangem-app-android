@file:Suppress("MagicNumber", "LongMethod")
package com.tangem.feature.tester.presentation.storybook.page.message

import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemMessageStory>.build(): TangemMessageStory {
    return TangemMessageStory(
        selectedEffect = TangemMessageEffect.None,
        onEffectChange = { newEffect ->
            updateStory { it.copy(selectedEffect = newEffect) }
        },
    )
}

internal val tangemMessageStoryFactory
    get() = storyPageFactory(StateUpdater<TangemMessageStory>::build)