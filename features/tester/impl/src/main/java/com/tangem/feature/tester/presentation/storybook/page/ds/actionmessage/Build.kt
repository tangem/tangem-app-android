package com.tangem.feature.tester.presentation.storybook.page.ds.actionmessage

import com.tangem.core.ui.ds2.actionmessage.TangemActionMessage
import com.tangem.feature.tester.presentation.storybook.entity.TangemActionMessageStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemActionMessageStory>.build(): TangemActionMessageStory {
    return TangemActionMessageStory(
        variant = TangemActionMessage.Variant.Neutral,
        isLongText = false,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onLongTextToggle = { updateStory { it.copy(isLongText = !it.isLongText) } },
    )
}

internal val tangemActionMessageStoryFactory
    get() = storyPageFactory(StateUpdater<TangemActionMessageStory>::build)