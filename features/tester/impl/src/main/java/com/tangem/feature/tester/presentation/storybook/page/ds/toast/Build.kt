package com.tangem.feature.tester.presentation.storybook.page.ds.toast

import com.tangem.core.ui.ds2.toast.TangemToast
import com.tangem.feature.tester.presentation.storybook.entity.TangemToastStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemToastStory>.build(): TangemToastStory {
    return TangemToastStory(
        appearance = TangemToast.Appearance.Neutral,
        duration = TangemToast.Duration.Short,
        isMultiLine = false,
        hasIcon = true,
        hasAction = false,
        isClosable = true,
        onAppearanceChange = { appearance -> updateStory { it.copy(appearance = appearance) } },
        onDurationChange = { duration -> updateStory { it.copy(duration = duration) } },
        onMultiLineToggle = { updateStory { it.copy(isMultiLine = !it.isMultiLine) } },
        onIconToggle = { updateStory { it.copy(hasIcon = !it.hasIcon) } },
        onActionToggle = { updateStory { it.copy(hasAction = !it.hasAction) } },
        onClosableToggle = { updateStory { it.copy(isClosable = !it.isClosable) } },
    )
}

internal val tangemToastStoryFactory
    get() = storyPageFactory(StateUpdater<TangemToastStory>::build)