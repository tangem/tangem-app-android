@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.placeholder

import com.tangem.feature.tester.presentation.storybook.entity.PlaceholderStory
import com.tangem.feature.tester.presentation.storybook.entity.StoryPageFactory

internal val placeholderStoryFactory: StoryPageFactory =
    StoryPageFactory { PlaceholderStory }