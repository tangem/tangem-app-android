package com.tangem.feature.tester.presentation.storybook.page.ds

import com.tangem.feature.tester.presentation.storybook.entity.DsComponentsListStory
import com.tangem.feature.tester.presentation.storybook.entity.StoryPageFactory

internal val dsComponentsListStoryFactory: StoryPageFactory = StoryPageFactory { updatePage ->
    DsComponentsListStory(
        onStoryClick = { factory ->
            updatePage { factory.create(updatePage) }
        },
    )
}