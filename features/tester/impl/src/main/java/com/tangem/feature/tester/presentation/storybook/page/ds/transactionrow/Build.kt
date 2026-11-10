package com.tangem.feature.tester.presentation.storybook.page.ds.transactionrow

import com.tangem.core.ui.ds2.transactionrow.TangemTransactionRow
import com.tangem.feature.tester.presentation.storybook.entity.TangemTransactionRowStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemTransactionRowStory>.build(): TangemTransactionRowStory {
    return TangemTransactionRowStory(
        variant = TangemTransactionRowStory.Variant.Content,
        status = TangemTransactionRow.Status.Progress,
        subtitle = TangemTransactionRowStory.Subtitle.Entity,
        hasDescription = false,
        hasAmount = true,
        hasCaption = true,
        hasBadge = false,
        hasStatusMessage = false,
        isBalanceHidden = false,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onStatusChange = { status -> updateStory { it.copy(status = status) } },
        onSubtitleChange = { subtitle -> updateStory { it.copy(subtitle = subtitle) } },
        onDescriptionToggle = { updateStory { it.copy(hasDescription = !it.hasDescription) } },
        onAmountToggle = { updateStory { it.copy(hasAmount = !it.hasAmount) } },
        onCaptionToggle = { updateStory { it.copy(hasCaption = !it.hasCaption) } },
        onBadgeToggle = { updateStory { it.copy(hasBadge = !it.hasBadge) } },
        onStatusMessageToggle = { updateStory { it.copy(hasStatusMessage = !it.hasStatusMessage) } },
        onBalanceHiddenToggle = { updateStory { it.copy(isBalanceHidden = !it.isBalanceHidden) } },
    )
}

internal val tangemTransactionRowStoryFactory
    get() = storyPageFactory(StateUpdater<TangemTransactionRowStory>::build)