package com.tangem.feature.tester.presentation.storybook.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.feature.tester.presentation.storybook.entity.ButtonsStory
import com.tangem.feature.tester.presentation.storybook.entity.DeviceIconStory
import com.tangem.feature.tester.presentation.storybook.entity.DsComponentsListStory
import com.tangem.feature.tester.presentation.storybook.entity.NorthernLightsStory
import com.tangem.feature.tester.presentation.storybook.entity.OpportunitiesBGStory
import com.tangem.feature.tester.presentation.storybook.entity.PlaceholderStory
import com.tangem.feature.tester.presentation.storybook.entity.ProgressIndicatorStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemBadgeStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemBadgeV2Story
import com.tangem.feature.tester.presentation.storybook.entity.TangemButtonStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemFadeStory
import com.tangem.feature.tester.presentation.storybook.entity.StoryBookUM
import com.tangem.feature.tester.presentation.storybook.entity.StoryList
import com.tangem.feature.tester.presentation.storybook.entity.TangemCheckboxStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemHeaderRowStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemLoaderStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemContextMenuStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemPagerIndicatorStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemSearchFieldStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemSegmentedPickerStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemTabStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemTokenRowStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemTopBarStory
import com.tangem.feature.tester.presentation.storybook.entity.TypographyStory
import com.tangem.feature.tester.presentation.storybook.entity.*
import com.tangem.feature.tester.presentation.storybook.page.background.NorthernLightsStory
import com.tangem.feature.tester.presentation.storybook.page.badge.TangemBadgeStory
import com.tangem.feature.tester.presentation.storybook.page.buttons.ButtonsStory
import com.tangem.feature.tester.presentation.storybook.page.checkbox.TangemCheckboxStory
import com.tangem.feature.tester.presentation.storybook.page.contextmenu.TangemContextMenuStory
import com.tangem.feature.tester.presentation.storybook.page.deviceicon.DeviceIconStory
import com.tangem.feature.tester.presentation.storybook.page.ds.DsComponentsListStory
import com.tangem.feature.tester.presentation.storybook.page.ds.badge.TangemBadgeV2Story
import com.tangem.feature.tester.presentation.storybook.page.ds.button.TangemButtonStory
import com.tangem.feature.tester.presentation.storybook.page.ds.checkbox.TangemCheckboxV2Story
import com.tangem.feature.tester.presentation.storybook.page.ds.checkmark.TangemCheckmarkStory
import com.tangem.feature.tester.presentation.storybook.page.ds.fade.TangemFadeStory
import com.tangem.feature.tester.presentation.storybook.page.ds.loader.TangemLoaderStory
import com.tangem.feature.tester.presentation.storybook.page.ds.row.TangemRowStory
import com.tangem.feature.tester.presentation.storybook.page.ds.search.TangemSearchStory
import com.tangem.feature.tester.presentation.storybook.page.ds.shimmer.TangemShimmerStory
import com.tangem.feature.tester.presentation.storybook.page.ds.topnavigation.TangemTopNavigationStory
import com.tangem.feature.tester.presentation.storybook.page.headerrow.TangemHeaderRowStory
import com.tangem.feature.tester.presentation.storybook.page.message.TangemMessageStory
import com.tangem.feature.tester.presentation.storybook.page.opportunities.OpportunitiesBGStory
import com.tangem.feature.tester.presentation.storybook.page.pagerindicator.TangemPagerIndicatorStory
import com.tangem.feature.tester.presentation.storybook.page.placeholder.PlaceholderStory
import com.tangem.feature.tester.presentation.storybook.page.progress.ProgressIndicatorStory
import com.tangem.feature.tester.presentation.storybook.page.searchfield.TangemSearchFieldStory
import com.tangem.feature.tester.presentation.storybook.page.tab.TangemTabStory
import com.tangem.feature.tester.presentation.storybook.page.tabs.TangemSegmentedPickerStory
import com.tangem.feature.tester.presentation.storybook.page.tokenrow.TangemTokenRowStory
import com.tangem.feature.tester.presentation.storybook.page.topbar.TangemTopBarStory
import com.tangem.feature.tester.presentation.storybook.page.typography.TypographyStory

@Suppress("CyclomaticComplexMethod")
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
            is TangemTopBarStory -> TangemTopBarStory(state = storyState)
            is TangemTabStory -> TangemTabStory(state = storyState)
            TangemPagerIndicatorStory -> TangemPagerIndicatorStory()
            PlaceholderStory -> PlaceholderStory()
            ProgressIndicatorStory -> ProgressIndicatorStory()
            DeviceIconStory -> DeviceIconStory()
            is DsComponentsListStory -> DsComponentsListStory(state = storyState)
            is TangemLoaderStory -> TangemLoaderStory(state = storyState)
            is TangemButtonStory -> TangemButtonStory(state = storyState)
            is TangemBadgeV2Story -> TangemBadgeV2Story(state = storyState)
            is TangemCheckboxV2Story -> TangemCheckboxV2Story(state = storyState)
            is TangemCheckmarkStory -> TangemCheckmarkStory(state = storyState)
            is TangemRowStory -> TangemRowStory(state = storyState)
            is TangemSearchStory -> TangemSearchStory(state = storyState)
            is TangemShimmerStory -> TangemShimmerStory(state = storyState)
            is TangemFadeStory -> TangemFadeStory(state = storyState)
            is TangemTopNavigationStory -> TangemTopNavigationStory(state = storyState)
        }
    }
}