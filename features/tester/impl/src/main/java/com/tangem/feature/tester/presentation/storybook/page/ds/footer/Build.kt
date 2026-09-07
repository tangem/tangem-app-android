package com.tangem.feature.tester.presentation.storybook.page.ds.footer

import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.feature.tester.presentation.storybook.entity.TangemFooterStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemFooterStory>.build(): TangemFooterStory {
    return TangemFooterStory(
        actions = TangemFooterStory.Actions.ButtonGroupVertical,
        fadeVariant = TangemFade.Variant.Hard,
        hasExtraTop = true,
        isFadeEnabled = true,
        isBlurEnabled = true,
        isInModal = false,
        onActionsChange = { actions ->
            updateStory { it.copy(actions = actions) }
        },
        onFadeVariantChange = { variant ->
            updateStory { it.copy(fadeVariant = variant) }
        },
        onExtraTopToggle = {
            updateStory { it.copy(hasExtraTop = !it.hasExtraTop) }
        },
        onFadeToggle = {
            updateStory { it.copy(isFadeEnabled = !it.isFadeEnabled) }
        },
        onBlurToggle = {
            updateStory { it.copy(isBlurEnabled = !it.isBlurEnabled) }
        },
        onInModalToggle = {
            updateStory { it.copy(isInModal = !it.isInModal) }
        },
    )
}

internal val tangemFooterStoryFactory
    get() = storyPageFactory(StateUpdater<TangemFooterStory>::build)