package com.tangem.feature.tester.presentation.storybook.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.feature.tester.presentation.storybook.entity.ButtonsStory
import com.tangem.feature.tester.presentation.storybook.entity.NorthernLightsStory
import com.tangem.feature.tester.presentation.storybook.entity.OpportunitiesBGStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemBadgeStory
import com.tangem.feature.tester.presentation.storybook.entity.StoryBookUM
import com.tangem.feature.tester.presentation.storybook.entity.StoryList
import com.tangem.feature.tester.presentation.storybook.entity.TangemCheckboxStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemHeaderRowStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemContextMenuStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemSearchFieldStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemSegmentedPickerStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemTokenRowStory
import com.tangem.feature.tester.presentation.storybook.entity.TypographyStory
import com.tangem.feature.tester.presentation.storybook.page.background.NorthernLightsStory
import com.tangem.feature.tester.presentation.storybook.page.badge.TangemBadgeStory
import com.tangem.feature.tester.presentation.storybook.page.buttons.ButtonsStory
import com.tangem.feature.tester.presentation.storybook.page.opportunities.OpportunitiesBGStory
import com.tangem.feature.tester.presentation.storybook.page.checkbox.TangemCheckboxStory
import com.tangem.feature.tester.presentation.storybook.page.message.TangemMessageStory
import com.tangem.feature.tester.presentation.storybook.page.tabs.TangemSegmentedPickerStory
import com.tangem.feature.tester.presentation.storybook.page.tokenrow.TangemTokenRowStory
import com.tangem.feature.tester.presentation.storybook.page.headerrow.TangemHeaderRowStory
import com.tangem.feature.tester.presentation.storybook.page.contextmenu.TangemContextMenuStory
import com.tangem.feature.tester.presentation.storybook.page.searchfield.TangemSearchFieldStory
import com.tangem.feature.tester.presentation.storybook.page.typography.TypographyStory

@Composable
internal fun StoryBookScreen(state: StoryBookUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    AnimatedContent(
        targetState = state.currentPage,
        contentKey = { it::class },
        modifier = modifier,
    ) { storyState ->
        when (storyState) {
            StoryList -> StoryBookListScreen(state = state)
            is NorthernLightsStory -> NorthernLightsStory(state = storyState)
            ButtonsStory -> ButtonsStory()
            is TangemBadgeStory -> TangemBadgeStory(state = storyState)
            OpportunitiesBGStory -> OpportunitiesBGStory()
            is TangemMessageStory -> TangemMessageStory(state = storyState)
            is TangemCheckboxStory -> TangemCheckboxStory(state = storyState)
            TangemSegmentedPickerStory -> TangemSegmentedPickerStory()
            is TangemTokenRowStory -> TangemTokenRowStory(state = storyState)
            is TangemHeaderRowStory -> TangemHeaderRowStory(state = storyState)
            is TangemContextMenuStory -> TangemContextMenuStory(state = storyState)
            is TangemSearchFieldStory -> TangemSearchFieldStory(state = storyState)
            is TypographyStory -> TypographyStory(state = storyState)
        }
    }
}