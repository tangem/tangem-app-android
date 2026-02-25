package com.tangem.feature.tester.presentation.storybook.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.feature.tester.presentation.storybook.entity.NorthernLightsStory
import com.tangem.feature.tester.presentation.storybook.entity.StoryBookUM
import com.tangem.feature.tester.presentation.storybook.entity.StoryList
import com.tangem.feature.tester.presentation.storybook.page.background.NorthernLightsStory

@Composable
internal fun StoryBookScreen(state: StoryBookUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    AnimatedContent(
        targetState = state.currentPage,
        modifier = modifier,
    ) { storyState ->
        when (storyState) {
            StoryList -> StoryBookListScreen(state = state)
            is NorthernLightsStory -> NorthernLightsStory(state = storyState)
        }
    }
}