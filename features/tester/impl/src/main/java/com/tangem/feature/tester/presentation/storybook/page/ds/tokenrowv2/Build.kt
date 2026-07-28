package com.tangem.feature.tester.presentation.storybook.page.ds.tokenrowv2

import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.feature.tester.presentation.storybook.entity.TangemTokenRowV2Story
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemTokenRowV2Story>.build(): TangemTokenRowV2Story {
    return TangemTokenRowV2Story(
        variant = TangemTokenRowV2Story.Variant.Default,
        direction = TangemPriceChange.Direction.Up,
        hasBadge = true,
        isBadgeFilled = false,
        hasPending = false,
        hasQuote = true,
        hasPriceChange = true,
        hasCryptoBalance = true,
        hasContractWarning = false,
        hasUpdateWarning = false,
        hasMessageBubble = true,
        isBalanceHidden = false,
        isQuoteFlickering = false,
        isBalanceFlickering = false,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onDirectionChange = { direction -> updateStory { it.copy(direction = direction) } },
        onBadgeToggle = { updateStory { it.copy(hasBadge = !it.hasBadge) } },
        onBadgeFilledToggle = { updateStory { it.copy(isBadgeFilled = !it.isBadgeFilled) } },
        onPendingToggle = { updateStory { it.copy(hasPending = !it.hasPending) } },
        onQuoteToggle = { updateStory { it.copy(hasQuote = !it.hasQuote) } },
        onPriceChangeToggle = { updateStory { it.copy(hasPriceChange = !it.hasPriceChange) } },
        onCryptoBalanceToggle = { updateStory { it.copy(hasCryptoBalance = !it.hasCryptoBalance) } },
        onContractWarningToggle = { updateStory { it.copy(hasContractWarning = !it.hasContractWarning) } },
        onUpdateWarningToggle = { updateStory { it.copy(hasUpdateWarning = !it.hasUpdateWarning) } },
        onMessageBubbleToggle = { updateStory { it.copy(hasMessageBubble = !it.hasMessageBubble) } },
        onBalanceHiddenToggle = { updateStory { it.copy(isBalanceHidden = !it.isBalanceHidden) } },
        onQuoteFlickeringToggle = { updateStory { it.copy(isQuoteFlickering = !it.isQuoteFlickering) } },
        onBalanceFlickeringToggle = { updateStory { it.copy(isBalanceFlickering = !it.isBalanceFlickering) } },
    )
}

internal val tangemTokenRowV2StoryFactory
    get() = storyPageFactory(StateUpdater<TangemTokenRowV2Story>::build)