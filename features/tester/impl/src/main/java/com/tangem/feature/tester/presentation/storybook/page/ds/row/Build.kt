package com.tangem.feature.tester.presentation.storybook.page.ds.row

import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.feature.tester.presentation.storybook.entity.TangemRowStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemRowStory>.build(): TangemRowStory {
    return TangemRowStory(
        contentLead = TangemRowContentLead.Equal,
        verticalAlignment = TangemRowVerticalAlignment.Top,
        background = TangemRowStory.Background.BgPrimary,
        divider = false,
        includeInnerPaddings = true,
        isClickable = false,
        hasStartSlot = false,
        hasEndSlot = false,
        hasSubtitle = true,
        hasValue = true,
        hasSubvalue = true,
        hasExtraBottom = false,
        longTitle = false,
        textScale = 1f,
        onContentLeadChange = { contentLead ->
            updateStory { it.copy(contentLead = contentLead) }
        },
        onVerticalAlignmentChange = { alignment ->
            updateStory { it.copy(verticalAlignment = alignment) }
        },
        onBackgroundChange = { background ->
            updateStory { it.copy(background = background) }
        },
        onDividerToggle = {
            updateStory { it.copy(divider = !it.divider) }
        },
        onInnerPaddingsToggle = {
            updateStory { it.copy(includeInnerPaddings = !it.includeInnerPaddings) }
        },
        onClickableToggle = {
            updateStory { it.copy(isClickable = !it.isClickable) }
        },
        onStartSlotToggle = {
            updateStory { it.copy(hasStartSlot = !it.hasStartSlot) }
        },
        onEndSlotToggle = {
            updateStory { it.copy(hasEndSlot = !it.hasEndSlot) }
        },
        onSubtitleToggle = {
            updateStory { it.copy(hasSubtitle = !it.hasSubtitle) }
        },
        onValueToggle = {
            updateStory { it.copy(hasValue = !it.hasValue) }
        },
        onSubvalueToggle = {
            updateStory { it.copy(hasSubvalue = !it.hasSubvalue) }
        },
        onExtraBottomToggle = {
            updateStory { it.copy(hasExtraBottom = !it.hasExtraBottom) }
        },
        onLongTitleToggle = {
            updateStory { it.copy(longTitle = !it.longTitle) }
        },
        onTextScaleChange = { scale ->
            updateStory { it.copy(textScale = scale) }
        },
    )
}

internal val tangemRowStoryFactory
    get() = storyPageFactory(StateUpdater<TangemRowStory>::build)