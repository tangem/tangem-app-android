package com.tangem.feature.tester.presentation.storybook.page.ds.messagebubble

import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageBubbleStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageBubbleStory.Background
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageBubbleStory.Message
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageBubbleStory.TrailingButton
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemMessageBubbleStory>.build(): TangemMessageBubbleStory {
    return TangemMessageBubbleStory(
        variant = TangemMessageBubble.Variant.Neutral,
        message = Message.Short,
        trailingButton = TrailingButton.Close,
        background = Background.BgPrimary,
        showTip = true,
        hasIcon = true,
        isClickable = true,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onMessageChange = { message -> updateStory { it.copy(message = message) } },
        onTrailingButtonChange = { button -> updateStory { it.copy(trailingButton = button) } },
        onBackgroundChange = { background -> updateStory { it.copy(background = background) } },
        onTipToggle = { updateStory { it.copy(showTip = !it.showTip) } },
        onIconToggle = { updateStory { it.copy(hasIcon = !it.hasIcon) } },
        onClickableToggle = { updateStory { it.copy(isClickable = !it.isClickable) } },
    )
}

internal val tangemMessageBubbleStoryFactory
    get() = storyPageFactory(StateUpdater<TangemMessageBubbleStory>::build)