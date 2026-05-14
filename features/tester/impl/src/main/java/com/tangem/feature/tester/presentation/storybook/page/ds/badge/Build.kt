package com.tangem.feature.tester.presentation.storybook.page.ds.badge

import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.feature.tester.presentation.storybook.entity.TangemBadgeV2Story
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemBadgeV2Story>.build(): TangemBadgeV2Story {
    return TangemBadgeV2Story(
        variant = TangemBadge.Variant.Tinted,
        status = TangemBadge.Status.Info,
        size = TangemBadge.Size.X9,
        background = TangemBadgeV2Story.Background.BgPrimary,
        hasIconStart = false,
        hasIconEnd = false,
        textScale = 1f,
        onVariantChange = { variant ->
            updateStory { it.copy(variant = variant) }
        },
        onStatusChange = { status ->
            updateStory { it.copy(status = status) }
        },
        onSizeChange = { size ->
            updateStory { it.copy(size = size) }
        },
        onBackgroundChange = { background ->
            updateStory { it.copy(background = background) }
        },
        onIconStartToggle = {
            updateStory { it.copy(hasIconStart = !it.hasIconStart) }
        },
        onIconEndToggle = {
            updateStory { it.copy(hasIconEnd = !it.hasIconEnd) }
        },
        onTextScaleChange = { scale ->
            updateStory { it.copy(textScale = scale) }
        },
    )
}

internal val tangemBadgeV2StoryFactory
    get() = storyPageFactory(StateUpdater<TangemBadgeV2Story>::build)