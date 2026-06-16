package com.tangem.feature.tester.presentation.storybook.page.ds.fade

import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.feature.tester.presentation.storybook.entity.TangemFadeStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemFadeStory>.build(): TangemFadeStory {
    return TangemFadeStory(
        variant = TangemFade.Variant.Hard,
        position = TangemFade.Position.Top,
        isBlur = false,
        onVariantChange = { variant ->
            updateStory { it.copy(variant = variant) }
        },
        onPositionChange = { position ->
            updateStory { it.copy(position = position) }
        },
        onBlurToggle = {
            updateStory { it.copy(isBlur = !it.isBlur) }
        },
    )
}

internal val tangemFadeStoryFactory
    get() = storyPageFactory(StateUpdater<TangemFadeStory>::build)