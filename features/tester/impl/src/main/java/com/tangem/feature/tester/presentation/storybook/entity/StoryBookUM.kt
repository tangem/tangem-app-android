package com.tangem.feature.tester.presentation.storybook.entity

import com.tangem.core.ui.ds2.DsVersion

internal data class StoryBookUM(
    val currentPage: StoryBookPage = StoryList,
    val currentVersion: DsVersion? = null,
    val onBackClick: () -> Unit,
    val onStoryClick: (StoryPageFactory, DsVersion?) -> Unit,
)