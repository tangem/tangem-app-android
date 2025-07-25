@file:Suppress("MagicNumber")

package com.tangem.tap.features.home.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.StoriesScreenTestTags
import com.tangem.tap.features.home.compose.content.*
import com.tangem.tap.features.home.compose.views.HomeButtonsV2
import com.tangem.tap.features.home.compose.views.StoriesProgressBar
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.home.redux.Stories
import com.tangem.wallet.R
import kotlin.math.max

@Composable
internal fun StoriesScreenV2(
    homeState: MutableState<HomeState>,
    onCreateNewWalletButtonClick: () -> Unit,
    onAddExistingWalletButtonClick: () -> Unit,
    onScanButtonClick: () -> Unit,
) {
    val state = homeState.value

    var currentStory by remember { mutableStateOf(state.firstStory) }
    val currentStoryIndex by rememberUpdatedState(newValue = state.stepOf(currentStory))

    val goToPreviousStory = remember(currentStory, currentStoryIndex) {
        { currentStory = state.stories[max(0, currentStoryIndex - 1)] }
    }
    val goToNextStory = remember(currentStory, currentStoryIndex) {
        {
            currentStory = if (currentStoryIndex < state.stories.lastIndex) {
                state.stories[currentStoryIndex + 1]
            } else {
                state.firstStory
            }
        }
    }

    // todo refactor [REDACTED_TASK_KEY]
    StoriesScreenContentV2(
        modifier = Modifier
            .fillMaxSize()
            .testTag(StoriesScreenTestTags.SCREEN_CONTAINER),
        config = StoriesScreenContentV2Config(
            storiesSize = state.stories.lastIndex,
            currentStoryIndex = currentStoryIndex,
            currentStory = currentStory,
            isScanInProgress = homeState.value.scanInProgress,
            onGoToPreviousStory = goToPreviousStory,
            onGoToNextStory = goToNextStory,
            onCreateNewWalletButtonClick = onCreateNewWalletButtonClick,
            onAddExistingWalletButtonClick = onAddExistingWalletButtonClick,
            onScanButtonClick = onScanButtonClick,
        ),
    )
}

@Deprecated("Use StoriesContainer from core/ui")
@Suppress("LongMethod")
@Composable
private fun StoriesScreenContentV2(config: StoriesScreenContentV2Config, modifier: Modifier = Modifier) {
    var isPressed by remember { mutableStateOf(value = false) }

    val isPaused = isPressed || config.isScanInProgress
    val currentStoryDuration = config.currentStory.duration

    Box(
        modifier = modifier.background(Color(0xFF010101)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                val pressStartTime = System.currentTimeMillis()
                                isPressed = true
                                this.tryAwaitRelease()
                                val pressEndTime = System.currentTimeMillis()
                                val totalPressTime = pressEndTime - pressStartTime
                                if (totalPressTime < 200) config.onGoToPreviousStory()
                                isPressed = false
                            },
                        )
                    },
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                val pressStartTime = System.currentTimeMillis()
                                isPressed = true
                                this.tryAwaitRelease()
                                val pressEndTime = System.currentTimeMillis()
                                val totalPressTime = pressEndTime - pressStartTime
                                if (totalPressTime < 200) config.onGoToNextStory()
                                isPressed = false
                            },
                        )
                    },
            )
        }

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StoriesProgressBar(
                steps = config.storiesSize,
                currentStep = config.currentStoryIndex,
                stepDuration = currentStoryDuration,
                paused = isPaused,
                onStepFinish = config.onGoToNextStory,
            )
            Image(
                painter = painterResource(id = R.drawable.ic_tangem_logo),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .padding(
                        start = TangemTheme.dimens.spacing16,
                        top = TangemTheme.dimens.spacing16,
                    )
                    .height(TangemTheme.dimens.size18)
                    .align(Alignment.Start),
            )
            when (config.currentStory) {
                Stories.TangemIntro -> FirstStoriesContent(
                    isPaused = isPaused,
                    duration = currentStoryDuration,
                )
                Stories.RevolutionaryWallet -> StoriesRevolutionaryWallet()
                Stories.UltraSecureBackup -> StoriesUltraSecureBackup(
                    isPaused = isPaused,
                    stepDuration = currentStoryDuration,
                )
                Stories.Currencies -> StoriesCurrencies(isPaused, currentStoryDuration)
                Stories.Web3 -> StoriesWeb3(isPaused, currentStoryDuration)
                Stories.WalletForEveryone -> StoriesWalletForEveryone(currentStoryDuration)
            }
        }
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = TangemTheme.dimens.spacing16)
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            HomeButtonsV2(
                modifier = Modifier.fillMaxWidth(),
                btnScanStateInProgress = config.isScanInProgress,
                onScanButtonClick = config.onScanButtonClick,
                onCreateNewWalletButtonClick = config.onCreateNewWalletButtonClick,
                onAddExistingWalletButtonClick = config.onAddExistingWalletButtonClick,
            )
        }
    }
}

private data class StoriesScreenContentV2Config(
    val storiesSize: Int,
    val currentStoryIndex: Int,
    val currentStory: Stories,
    val isScanInProgress: Boolean,
    val onGoToPreviousStory: () -> Unit = {},
    val onGoToNextStory: () -> Unit = {},
    val onCreateNewWalletButtonClick: () -> Unit = {},
    val onAddExistingWalletButtonClick: () -> Unit = {},
    val onScanButtonClick: () -> Unit = {},
)

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StoriesScreenContentV2Preview(
    @PreviewParameter(StoriesScreenContentV2ConfigProvider::class) config: StoriesScreenContentV2Config,
) {
    TangemThemePreview {
        StoriesScreenContentV2(config = config)
    }
}

private class StoriesScreenContentV2ConfigProvider : CollectionPreviewParameterProvider<StoriesScreenContentV2Config>(
    collection = listOf(
        StoriesScreenContentV2Config(
            storiesSize = 6,
            currentStoryIndex = 0,
            currentStory = Stories.TangemIntro,
            isScanInProgress = true,
        ),
        StoriesScreenContentV2Config(
            storiesSize = 6,
            currentStoryIndex = 1,
            currentStory = Stories.RevolutionaryWallet,
            isScanInProgress = false,
        ),
        StoriesScreenContentV2Config(
            storiesSize = 6,
            currentStoryIndex = 2,
            currentStory = Stories.UltraSecureBackup,
            isScanInProgress = false,
        ),
        StoriesScreenContentV2Config(
            storiesSize = 6,
            currentStoryIndex = 3,
            currentStory = Stories.Currencies,
            isScanInProgress = false,
        ),
        StoriesScreenContentV2Config(
            storiesSize = 6,
            currentStoryIndex = 4,
            currentStory = Stories.Web3,
            isScanInProgress = false,
        ),
        StoriesScreenContentV2Config(
            storiesSize = 6,
            currentStoryIndex = 5,
            currentStory = Stories.WalletForEveryone,
            isScanInProgress = false,
        ),
    ),
)
// endregion Preview