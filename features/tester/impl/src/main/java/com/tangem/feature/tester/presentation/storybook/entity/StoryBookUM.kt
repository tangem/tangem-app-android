package com.tangem.feature.tester.presentation.storybook.entity

internal data class StoryBookUM(
    val currentPage: StoryBookPage = StoryList,
    val onBackClick: () -> Unit,
    val onStoryClick: (StoryPageFactory) -> Unit,
)