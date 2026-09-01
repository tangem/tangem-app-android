package com.tangem.features.collectibles.impl.stories.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.stories.inner.StoriesClickableArea
import com.tangem.core.ui.components.stories.inner.StoriesProgressBar
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.stories.ui.preview.CollectiblesStoriesPreviewData
import com.tangem.features.collectibles.impl.stories.ui.state.CollectiblesStoriesUM
import com.tangem.features.collectibles.impl.stories.ui.state.CollectiblesStorySlideUM

private val PROGRESS_HEIGHT = 6.dp
private val PROGRESS_SEGMENT_WIDTH = 32.dp
private const val PROGRESS_INACTIVE_ALPHA = .1f

@Composable
internal fun CollectiblesStoriesScreen(state: CollectiblesStoriesUM, modifier: Modifier = Modifier) {
    var isPressed by remember { mutableStateOf(value = false) }
    val slide = state.slides.getOrNull(state.currentIndex) ?: return

    val isLastSlide = state.currentIndex == state.slides.lastIndex

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                fadeEnabled = false,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                contentColumn = {
                    StoriesProgressBar(
                        steps = state.slides.lastIndex,
                        currentStep = state.currentIndex,
                        paused = isPressed,
                        height = PROGRESS_HEIGHT,
                        segmentWidth = PROGRESS_SEGMENT_WIDTH,
                        inactiveAlpha = PROGRESS_INACTIVE_ALPHA,
                        contentPadding = PaddingValues(),
                        holdWhenFinished = isLastSlide,
                        onStepFinish = state.onNextSlideClick,
                    )
                },
                endButton = { TangemButton.Close(onClick = state.onCloseClick) },
            )
        },
    ) { contentPadding ->
        StoriesClickableArea(
            onPress = { isPressed = it },
            onPreviousStory = state.onPreviousSlideClick,
            onNextStory = state.onNextSlideClick,
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = contentPadding.calculateBottomPadding()),
        ) {
            SlideText(slide = slide)
            SlideFooter(state = state, slide = slide)
        }
    }
}

@Composable
private fun SlideText(slide: CollectiblesStorySlideUM) {
    Column(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = slide.title.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = slide.subtitle.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SlideFooter(state: CollectiblesStoriesUM, slide: CollectiblesStorySlideUM) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.legal.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.tertiary,
            textAlign = TextAlign.Center,
        )
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = slide.continueButtonText,
            onClick = state.onContinueClick,
        )
    }
}

@Preview(widthDp = 360, heightDp = 780, showBackground = true)
@Preview(widthDp = 360, heightDp = 780, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesStoriesScreenPreview() {
    TangemThemePreviewRedesign {
        CollectiblesStoriesScreen(state = CollectiblesStoriesPreviewData.state)
    }
}

@Preview(widthDp = 360, heightDp = 780, showBackground = true)
@Preview(widthDp = 360, heightDp = 780, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesStoriesScreenLastSlidePreview() {
    TangemThemePreviewRedesign {
        CollectiblesStoriesScreen(state = CollectiblesStoriesPreviewData.lastSlideState)
    }
}