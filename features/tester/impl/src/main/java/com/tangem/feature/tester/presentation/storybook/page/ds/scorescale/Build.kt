package com.tangem.feature.tester.presentation.storybook.page.ds.scorescale

import com.tangem.feature.tester.presentation.storybook.entity.TangemScoreScaleStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemScoreScaleStory>.build(): TangemScoreScaleStory {
    return TangemScoreScaleStory(
        selectedScore = null,
        width = TangemScoreScaleStory.Width.Full,
        textScale = 1f,
        isEnabled = true,
        onScoreClick = { score -> updateStory { it.copy(selectedScore = score) } },
        onWidthChange = { width -> updateStory { it.copy(width = width) } },
        onTextScaleChange = { scale -> updateStory { it.copy(textScale = scale) } },
        onEnabledToggle = { updateStory { it.copy(isEnabled = !it.isEnabled) } },
    )
}

internal val tangemScoreScaleStoryFactory
    get() = storyPageFactory(StateUpdater<TangemScoreScaleStory>::build)