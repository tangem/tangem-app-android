@file:Suppress("MagicNumber")

package com.tangem.tap.features.home.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.tangem.tap.features.home.compose.views.HomeButtons
import com.tangem.tap.features.home.compose.views.SearchCurrenciesButton
import com.tangem.tap.features.home.compose.views.StoriesProgressBar
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.home.redux.Stories
import com.tangem.wallet.R
import kotlin.math.max

@Composable
internal fun StoriesScreen(
    homeState: MutableState<HomeState>,
    onScanButtonClick: () -> Unit,
    onShopButtonClick: () -> Unit,
    onSearchTokensClick: () -> Unit,
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
    StoriesScreenContent(
        modifier = Modifier
            .fillMaxSize()
            .testTag(StoriesScreenTestTags.SCREEN_CONTAINER),
        config = StoriesScreenContentConfig(
            storiesSize = state.stories.lastIndex,
            currentStoryIndex = currentStoryIndex,
            currentStory = currentStory,
            isScanInProgress = homeState.value.scanInProgress,
            onGoToPreviousStory = goToPreviousStory,
            onGoToNextStory = goToNextStory,
            onSearchTokensClick = onSearchTokensClick,
            onScanButtonClick = onScanButtonClick,
            onShopButtonClick = onShopButtonClick,
        ),
    )
}

@Deprecated("Use StoriesContainer from core/ui")
@Suppress("LongMethod")
@Composable
private fun StoriesScreenContent(config: StoriesScreenContentConfig, modifier: Modifier = Modifier) {
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
            AnimatedVisibility(
                visible = config.currentStory == Stories.Currencies,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SearchCurrenciesButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = config.onSearchTokensClick,
                )
            }

            HomeButtons(
                modifier = Modifier.fillMaxWidth(),
                btnScanStateInProgress = config.isScanInProgress,
                onScanButtonClick = config.onScanButtonClick,
                onShopButtonClick = config.onShopButtonClick,
            )
        }
    }
}

private data class StoriesScreenContentConfig(
    val storiesSize: Int,
    val currentStoryIndex: Int,
    val currentStory: Stories,
    val isScanInProgress: Boolean,
    val onGoToPreviousStory: () -> Unit = {},
    val onGoToNextStory: () -> Unit = {},
    val onSearchTokensClick: () -> Unit = {},
    val onScanButtonClick: () -> Unit = {},
    val onShopButtonClick: () -> Unit = {},
)

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StoriesScreenContentPreview(
    @PreviewParameter(StoriesScreenContentConfigProvider::class) config: StoriesScreenContentConfig,
) {
    TangemThemePreview {
        StoriesScreenContent(config = config)
    }
}

private class StoriesScreenContentConfigProvider : CollectionPreviewParameterProvider<StoriesScreenContentConfig>(
    collection = listOf(
        StoriesScreenContentConfig(
            storiesSize = 6,
            currentStoryIndex = 0,
            currentStory = Stories.TangemIntro,
            isScanInProgress = true,
        ),
        StoriesScreenContentConfig(
            storiesSize = 6,
            currentStoryIndex = 1,
            currentStory = Stories.RevolutionaryWallet,
            isScanInProgress = false,
        ),
        StoriesScreenContentConfig(
            storiesSize = 6,
            currentStoryIndex = 2,
            currentStory = Stories.UltraSecureBackup,
            isScanInProgress = false,
        ),
        StoriesScreenContentConfig(
            storiesSize = 6,
            currentStoryIndex = 3,
            currentStory = Stories.Currencies,
            isScanInProgress = false,
        ),
        StoriesScreenContentConfig(
            storiesSize = 6,
            currentStoryIndex = 4,
            currentStory = Stories.Web3,
            isScanInProgress = false,
        ),
        StoriesScreenContentConfig(
            storiesSize = 6,
            currentStoryIndex = 5,
            currentStory = Stories.WalletForEveryone,
            isScanInProgress = false,
        ),
    ),
)
// endregion Preview