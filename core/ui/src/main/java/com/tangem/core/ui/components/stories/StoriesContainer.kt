package com.tangem.core.ui.components.stories

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.stories.inner.StoriesClickableArea
import com.tangem.core.ui.components.stories.inner.StoriesProgressBar
import com.tangem.core.ui.components.stories.inner.StoriesStepStateMachine
import com.tangem.core.ui.components.stories.model.StoriesContentConfig
import com.tangem.core.ui.components.stories.model.StoryConfig
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
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
    var watchedCounter by remember { mutableIntStateOf(1) }
    var isPressed by remember { mutableStateOf(value = false) }
    val storyState by remember(config) {
        mutableStateOf(
            StoriesStepStateMachine(
                stories = config.stories,
                isRepeatable = config.isRestartable,
            ),
        )
    }
    BackHandler(onBack = { config.onClose(watchedCounter) })

    val isPaused = isPressed || isPauseStories

    val onNextClick = {
        storyState.nextStory()
        watchedCounter = (watchedCounter + 1).coerceAtMost(config.stories.size)

        if (!storyState.hasNext()) { // Finish stories if nothing left to show
            config.onClose(watchedCounter)
        }
    }
    Box(modifier = modifier) {
        StoriesClickableArea(
            onPress = { isPressed = it },
            onPreviousStory = storyState::prevStory,
            onNextStory = onNextClick,
        )

        currentStoryContent(storyState.currentStory, isPaused)

        Column(
            modifier = Modifier.statusBarsPadding(),
        ) {
            StoriesProgressBar(
                steps = storyState.steps,
                currentStep = storyState.currentIndex.intValue,
                stepDuration = storyState.currentStory.duration,
                paused = isPaused,
                onStepFinish = onNextClick,
            )
            Icon(
                painter = rememberVectorPainter(
                    image = ImageVector.vectorResource(R.drawable.ic_close_24),
                ),
                tint = TangemTheme.colors.icon.constant,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 14.dp, end = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = { config.onClose(watchedCounter) },
                    ),
            )
        }
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
    override val onClose: (Int) -> Unit = {},
) : StoriesContentConfig<StoryConfigPreviewProviderData>

private data class StoryConfigPreviewProviderData(
    override val duration: Int = 3_000,
    val title: String,
) : StoryConfig
// endregion