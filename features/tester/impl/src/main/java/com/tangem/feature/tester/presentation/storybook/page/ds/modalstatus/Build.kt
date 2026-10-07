package com.tangem.feature.tester.presentation.storybook.page.ds.modalstatus

import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.feature.tester.presentation.storybook.entity.TangemModalStatusStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemModalStatusStory>.build(): TangemModalStatusStory {
    return TangemModalStatusStory(
        variant = TangemModalStatus.Variant.Info,
        titleStyle = TangemModalStatus.TitleStyle.Status,
        hasDescription = true,
        hasCustomAsset = false,
        hasExtraBottom = false,
        hasDetails = false,
        openedExample = null,
        isMessageSheetShown = false,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onTitleStyleChange = { titleStyle -> updateStory { it.copy(titleStyle = titleStyle) } },
        onDescriptionToggle = { updateStory { it.copy(hasDescription = !it.hasDescription) } },
        onCustomAssetToggle = { updateStory { it.copy(hasCustomAsset = !it.hasCustomAsset) } },
        onExtraBottomToggle = { updateStory { it.copy(hasExtraBottom = !it.hasExtraBottom) } },
        onDetailsToggle = { updateStory { it.copy(hasDetails = !it.hasDetails) } },
        onExampleOpen = { example -> updateStory { it.copy(openedExample = example) } },
        onExampleDismiss = { updateStory { it.copy(openedExample = null) } },
        onMessageSheetShownChange = { isShown -> updateStory { it.copy(isMessageSheetShown = isShown) } },
    )
}

internal val tangemModalStatusStoryFactory
    get() = storyPageFactory(StateUpdater<TangemModalStatusStory>::build)