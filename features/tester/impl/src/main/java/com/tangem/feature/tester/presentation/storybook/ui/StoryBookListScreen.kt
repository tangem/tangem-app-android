package com.tangem.feature.tester.presentation.storybook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.StoryBookUM
import com.tangem.feature.tester.presentation.storybook.entity.StoryPageFactory
import com.tangem.feature.tester.presentation.storybook.page.background.northernLightsStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.badge.tangemBadgeStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.buttons.buttonsStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.checkbox.tangemCheckboxStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.contextmenu.tangemContextMenuStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.headerrow.tangemHeaderRowStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.message.tangemMessageStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.opportunities.opportunitiesBGStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.tabs.tangemSegmentedPickerStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.tokenrow.tangemTokenRowStoryFactory

private data class StoryItem(val title: String, val factory: StoryPageFactory)

private fun buildStories() = listOf(
    StoryItem(title = "🔘 Buttons", factory = buttonsStoryFactory),
    StoryItem(title = "🏷️ Badge", factory = tangemBadgeStoryFactory),
    StoryItem(title = "✨ Opportunities BG", factory = opportunitiesBGStoryFactory),
    StoryItem(title = "🌌 Northern Lights Background", factory = northernLightsStoryFactory),
    StoryItem(title = "💬 Message", factory = tangemMessageStoryFactory),
    StoryItem(title = "🗂️ Segmented Picker", factory = tangemSegmentedPickerStoryFactory),
    StoryItem(title = "☑️ Checkbox", factory = tangemCheckboxStoryFactory),
    StoryItem(title = "🪙 Token Row", factory = tangemTokenRowStoryFactory),
    StoryItem(title = "📑 Header Row", factory = tangemHeaderRowStoryFactory),
    StoryItem(title = "📋 Context Menu", factory = tangemContextMenuStoryFactory),
)

@Composable
internal fun StoryBookListScreen(state: StoryBookUM, modifier: Modifier = Modifier) {
    val stories = remember { buildStories() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary),
    ) {
        stickyHeader {
            AppBarWithBackButton(
                onBackClick = state.onBackClick,
                text = "Storybook",
                containerColor = TangemTheme.colors.background.primary,
            )
        }

        items(items = stories, key = { it.title }) { item ->
            PrimaryButton(
                text = item.title,
                onClick = { state.onStoryClick(item.factory) },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
            )
        }
    }
}