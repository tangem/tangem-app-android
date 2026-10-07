package com.tangem.feature.tester.presentation.storybook.page.ds.accountrow

import com.tangem.feature.tester.presentation.storybook.entity.TangemAccountRowStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemAccountRowStory>.build(): TangemAccountRowStory {
    return TangemAccountRowStory(
        variant = TangemAccountRowStory.Variant.Default,
        tokenCount = 2,
        isExpanded = false,
        hasMembersAmount = true,
        hasUpdateWarning = false,
        hasSection = false,
        longTitle = false,
        isBalanceHidden = false,
        isBalanceFlickering = false,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onTokenCountChange = { count -> updateStory { it.copy(tokenCount = count) } },
        onExpandedToggle = { updateStory { it.copy(isExpanded = !it.isExpanded) } },
        onMembersAmountToggle = { updateStory { it.copy(hasMembersAmount = !it.hasMembersAmount) } },
        onUpdateWarningToggle = { updateStory { it.copy(hasUpdateWarning = !it.hasUpdateWarning) } },
        onSectionToggle = { updateStory { it.copy(hasSection = !it.hasSection) } },
        onLongTitleToggle = { updateStory { it.copy(longTitle = !it.longTitle) } },
        onBalanceHiddenToggle = { updateStory { it.copy(isBalanceHidden = !it.isBalanceHidden) } },
        onBalanceFlickeringToggle = { updateStory { it.copy(isBalanceFlickering = !it.isBalanceFlickering) } },
    )
}

internal val tangemAccountRowStoryFactory
    get() = storyPageFactory(StateUpdater<TangemAccountRowStory>::build)