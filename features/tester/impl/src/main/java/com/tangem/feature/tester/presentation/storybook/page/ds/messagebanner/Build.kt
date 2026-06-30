package com.tangem.feature.tester.presentation.storybook.page.ds.messagebanner

import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageBannerStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageBannerStory.Background
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemMessageBannerStory>.build(): TangemMessageBannerStory {
    return TangemMessageBannerStory(
        variant = TangemMessageBanner.Variant.Default,
        contentAlign = TangemMessageBanner.ContentAlign.Start,
        hasGlowRing = true,
        hasDescription = true,
        hasSecondaryButton = true,
        hasPrimaryButton = true,
        hasCloseButton = true,
        hasSlotStart = true,
        hasSlotEnd = true,
        hasExtraContent = true,
        background = Background.BgSecondary,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onContentAlignChange = { align -> updateStory { it.copy(contentAlign = align) } },
        onGlowRingToggle = { updateStory { it.copy(hasGlowRing = !it.hasGlowRing) } },
        onDescriptionToggle = { updateStory { it.copy(hasDescription = !it.hasDescription) } },
        onSecondaryButtonToggle = { updateStory { it.copy(hasSecondaryButton = !it.hasSecondaryButton) } },
        onPrimaryButtonToggle = { updateStory { it.copy(hasPrimaryButton = !it.hasPrimaryButton) } },
        onCloseButtonToggle = { updateStory { it.copy(hasCloseButton = !it.hasCloseButton) } },
        onSlotStartToggle = { updateStory { it.copy(hasSlotStart = !it.hasSlotStart) } },
        onSlotEndToggle = { updateStory { it.copy(hasSlotEnd = !it.hasSlotEnd) } },
        onExtraContentToggle = { updateStory { it.copy(hasExtraContent = !it.hasExtraContent) } },
        onBackgroundChange = { background -> updateStory { it.copy(background = background) } },
    )
}

internal val tangemMessageBannerStoryFactory
    get() = storyPageFactory(StateUpdater<TangemMessageBannerStory>::build)