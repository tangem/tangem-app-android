@file:Suppress("MagicNumber")

package com.tangem.tap.features.home.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.learn2earn.presentation.ui.Learn2earnStoriesScreen
import com.tangem.tap.features.home.compose.content.*
import com.tangem.tap.features.home.compose.views.HomeButtons
import com.tangem.tap.features.home.compose.views.StoriesProgressBar
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.home.redux.Stories
import com.tangem.wallet.R
import kotlin.math.max

@Suppress("LongMethod", "ComplexMethod")
@Composable
fun StoriesScreen(
    homeState: MutableState<HomeState>,
    onLearn2earnClick: () -> Unit,
    onScanButtonClick: () -> Unit,
    onShopButtonClick: () -> Unit,
    onSearchTokensClick: () -> Unit,
) {
    val systemUiController = rememberSystemUiController()
    val state = homeState.value

    var currentStory by remember { mutableStateOf(state.firstStory) }
    val currentStep = { state.stepOf(currentStory) }

    val goToPreviousScreen = {
        currentStory = state.stories[max(0, currentStep() - 1)]
    }
    val goToNextScreen = {
        currentStory = if (currentStep() < state.stories.lastIndex) {
            state.stories[currentStep() + 1]
        } else {
            state.firstStory
        }
    }

    val isPressed = remember { mutableStateOf(false) }
    val isPaused = isPressed.value || homeState.value.scanInProgress

    val hideContent = remember { mutableStateOf(true) }

    LaunchedEffect(key1 = currentStory.isDarkBackground) {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = !currentStory.isDarkBackground,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090E13)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(isPressed) {
                        detectTapGestures(
                            onPress = {
                                val pressStartTime = System.currentTimeMillis()
                                isPressed.value = true
                                this.tryAwaitRelease()
                                val pressEndTime = System.currentTimeMillis()
                                val totalPressTime = pressEndTime - pressStartTime
                                if (totalPressTime < 200) goToPreviousScreen()
                                isPressed.value = false
                            },
                        )
                    },
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(isPressed) {
                        detectTapGestures(
                            onPress = {
                                val pressStartTime = System.currentTimeMillis()
                                isPressed.value = true
                                this.tryAwaitRelease()
                                val pressEndTime = System.currentTimeMillis()
                                val totalPressTime = pressEndTime - pressStartTime
                                if (totalPressTime < 200) goToNextScreen()
                                isPressed.value = false
                            },
                        )
                    },
            )
        }
        if (!currentStory.isDarkBackground) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.ic_overlay),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
            )
        }

        val statusBarInsets = WindowInsets.statusBars
            .union(WindowInsets(top = 32.dp))

        Column(
            modifier = Modifier
                .windowInsetsPadding(statusBarInsets)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StoriesProgressBar(
                steps = state.stories.lastIndex,
                currentStep = currentStep(),
                stepDuration = currentStory.duration,
                paused = isPaused,
                onStepFinish = goToNextScreen,
            )
            Image(
                painter = painterResource(id = R.drawable.ic_tangem_logo),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .padding(start = 16.dp, top = 10.dp)
                    .height(17.dp)
                    .alpha(if (hideContent.value) 0f else 1f)
                    .align(Alignment.Start),
                colorFilter = if (currentStory.isDarkBackground) null else ColorFilter.tint(Color.Black),
            )
            when (currentStory) {
                Stories.OneInchPromo -> Learn2earnStoriesScreen(onLearn2earnClick)
                Stories.TangemIntro -> FirstStoriesContent(isPaused, currentStory.duration) { hideContent.value = it }
                Stories.RevolutionaryWallet -> StoriesRevolutionaryWallet(currentStory.duration)
                Stories.UltraSecureBackup -> StoriesUltraSecureBackup(isPaused, currentStory.duration)
                Stories.Currencies -> StoriesCurrencies(isPaused, currentStory.duration)
                Stories.Web3 -> StoriesWeb3(isPaused, currentStory.duration)
                Stories.WalletForEveryone -> StoriesWalletForEveryone(currentStory.duration)
            }
        }
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            if (currentStory == Stories.Currencies) {
                Button(
                    onClick = onSearchTokensClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .heightIn(48.dp),
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = Color.White,
                        contentColor = Color(0xFF080C10),
                    ),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(id = R.string.common_search_tokens),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (currentStory != Stories.OneInchPromo) {
                HomeButtons(
                    modifier = Modifier
                        .padding(
                            start = TangemTheme.dimens.size16,
                            end = TangemTheme.dimens.size16,
                            bottom = TangemTheme.dimens.size36,
                        )
                        .fillMaxWidth(),
                    isDarkBackground = currentStory.isDarkBackground,
                    btnScanStateInProgress = homeState.value.btnScanStateInProgress,
                    onScanButtonClick = onScanButtonClick,
                    onShopButtonClick = onShopButtonClick,
                )
            }
        }
    }
}

@Preview
@Composable
private fun StoriesScreenPreview() {
    StoriesScreen(
        onLearn2earnClick = {},
        onScanButtonClick = {},
        onShopButtonClick = {},
        onSearchTokensClick = {},
        homeState = remember { mutableStateOf(HomeState()) },
    )
}