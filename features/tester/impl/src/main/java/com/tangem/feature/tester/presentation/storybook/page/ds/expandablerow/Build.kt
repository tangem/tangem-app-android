package com.tangem.feature.tester.presentation.storybook.page.ds.expandablerow

import com.tangem.feature.tester.presentation.storybook.entity.TangemExpandableRowStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemExpandableRowStory>.build(): TangemExpandableRowStory {
    return TangemExpandableRowStory(
        variant = TangemExpandableRowStory.Variant.Default,
        tokenCount = 2,
        isExpanded = false,
        hasBadge = false,
        hasSubtitle = true,
        hasValue = true,
        hasCaption = true,
        longTitle = false,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onTokenCountChange = { count -> updateStory { it.copy(tokenCount = count) } },
        onExpandedToggle = { updateStory { it.copy(isExpanded = !it.isExpanded) } },
        onBadgeToggle = { updateStory { it.copy(hasBadge = !it.hasBadge) } },
        onSubtitleToggle = { updateStory { it.copy(hasSubtitle = !it.hasSubtitle) } },
        onValueToggle = { updateStory { it.copy(hasValue = !it.hasValue) } },
        onCaptionToggle = { updateStory { it.copy(hasCaption = !it.hasCaption) } },
        onLongTitleToggle = { updateStory { it.copy(longTitle = !it.longTitle) } },
    )
}

internal val tangemExpandableRowStoryFactory
    get() = storyPageFactory(StateUpdater<TangemExpandableRowStory>::build)