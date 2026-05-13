package com.tangem.feature.tester.presentation.storybook.page.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.DsComponentsListStory
import com.tangem.feature.tester.presentation.storybook.entity.StoryPageFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.button.tangemButtonStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.loader.tangemLoaderStoryFactory

private data class DsStoryItem(val title: String, val factory: StoryPageFactory)

private fun buildDsStories() = listOf(
    DsStoryItem(title = "⏳ TangemLoader", factory = tangemLoaderStoryFactory),
    DsStoryItem(title = "🔘 TangemButton", factory = tangemButtonStoryFactory),
)

@Composable
internal fun DsComponentsListStory(state: DsComponentsListStory, modifier: Modifier = Modifier) {
    val stories = remember { buildDsStories() }

    LazyColumn(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary),
    ) {
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