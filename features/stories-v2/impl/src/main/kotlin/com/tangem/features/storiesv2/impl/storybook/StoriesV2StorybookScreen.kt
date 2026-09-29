@file:Suppress("MagicNumber")

package com.tangem.features.storiesv2.impl.storybook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.storiesv2.StoryV2Type

@Composable
@Suppress("LongParameterList")
internal fun StoriesV2StorybookScreen(
    lastResult: String?,
    remoteStories: List<StoryV2Type>,
    onPresetClick: (StoryV2Preset) -> Unit,
    onRemoteStoryClick: (StoryV2Type) -> Unit,
    onPrefetchClick: () -> Unit,
    onClearRemoteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(all = 16.dp),
    ) {
        if (lastResult != null) {
            item(key = "result") {
                Text(
                    text = "Last result: $lastResult",
                    style = TangemTheme.typography3.subheading.medium,
                    color = TangemTheme.colors3.text.brand,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }

        items(items = StoryV2Preset.entries, key = { it.name }) { preset ->
            PresetRow(title = preset.title, description = preset.description, onClick = { onPresetClick(preset) })
        }

        item(key = "remoteHeader") {
            Text(
                text = "Remote stories",
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
        }
        items(items = remoteStories, key = { "remote_${it.name}" }) { type ->
            PresetRow(
                title = type.name.lowercase(),
                description = "Played from the current API environment, on the loading screen while it downloads",
                onClick = { onRemoteStoryClick(type) },
            )
        }
        item(key = "remotePrefetch") {
            PresetRow(
                title = "Prefetch",
                description = "Fetches the response and every file in the background, as opening this page does",
                onClick = onPrefetchClick,
            )
        }
        item(key = "remoteClear") {
            PresetRow(
                title = "Clear downloaded stories",
                description = "Forgets the saved response and the files, so the next open starts cold",
                onClick = onClearRemoteClick,
            )
        }
    }
}

@Composable
private fun PresetRow(title: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .clickable(onClick = onClick)
            .padding(all = 16.dp),
    ) {
        Text(
            text = title,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = description,
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}