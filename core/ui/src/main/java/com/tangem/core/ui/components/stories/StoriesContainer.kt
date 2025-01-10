package com.tangem.core.ui.components.stories

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.stories.inner.StoriesClickableArea
import com.tangem.core.ui.components.stories.inner.StoriesProgressBar
import com.tangem.core.ui.components.stories.inner.StoriesStepStateMachine
import com.tangem.core.ui.components.stories.model.StoriesContentConfig
import com.tangem.core.ui.components.stories.model.StoryConfig
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Stories container component.
 * Implements base logic for switching story pages and handling clicks
 *
 * @param config stories configuration
 * @param modifier composable modifier
 * @param isPauseStories pause stories progression externally
 * @param currentStoryContent content of current displaying story
 */
@Composable
inline fun <reified T : StoryConfig> StoriesContainer(
    config: StoriesContentConfig<T>,
    modifier: Modifier = Modifier,
    isPauseStories: Boolean = false,
    crossinline currentStoryContent: @Composable BoxScope.(T, Boolean) -> Unit,
) {
    var isPressed by remember { mutableStateOf(value = false) }
    val storyState by remember(config) {
        mutableStateOf(
            StoriesStepStateMachine(
                stories = config.stories,
                isRepeatable = config.isRestartable,
            ),
        )
    }

    val isPaused = isPressed || isPauseStories

    Box(modifier = modifier) {
        StoriesClickableArea(
            onPress = { isPressed = it },
            onPreviousStory = storyState::prevStory,
            onNextStory = storyState::nextStory,
        )

        currentStoryContent(storyState.currentStory, isPaused)

        StoriesProgressBar(
            steps = storyState.steps,
            currentStep = storyState.currentIndex.intValue,
            stepDuration = storyState.currentStory.duration,
            paused = isPaused,
            onStepFinish = {
                if (storyState.nextStory()) {
                    config.onClose()
                }
            },
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StoriesContainer_Preview() {
    TangemThemePreview {
        StoriesContainer(
            config = StoryContainerPreviewProviderData(),
            modifier = Modifier.background(Color(0xFF010101)),
            isPauseStories = false,
            currentStoryContent = { pageConfig, _ ->
                Text(
                    text = pageConfig.title,
                    color = TangemColorPalette.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                )
            },
        )
    }
}

private data class StoryContainerPreviewProviderData(
    override val stories: ImmutableList<StoryConfigPreviewProviderData> = persistentListOf(
        StoryConfigPreviewProviderData(title = "Tangem"),
        StoryConfigPreviewProviderData(title = "Cold"),
        StoryConfigPreviewProviderData(title = "Wallet"),
    ),
    override val isRestartable: Boolean = true,
    override val onClose: () -> Unit = {},
) : StoriesContentConfig<StoryConfigPreviewProviderData>

private data class StoryConfigPreviewProviderData(
    override val duration: Int = 3_000,
    val title: String,
) : StoryConfig
// endregion