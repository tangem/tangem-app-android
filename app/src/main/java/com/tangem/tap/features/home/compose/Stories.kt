package com.tangem.tap.features.home.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.wallet.R
import kotlin.math.max

@Composable
fun StoriesScreen(
    homeState: MutableState<HomeState> = mutableStateOf(HomeState()),
    onScanButtonClick: () -> Unit,
    onShopButtonClick: () -> Unit
) {
    val steps = 6
    val stepDuration = 8_000

    val currentStep = remember { mutableStateOf(1) }

    val isDarkBackground = currentStep.value !in 3..5

    val goToPreviousScreen = {
        currentStep.value = max(1, currentStep.value - 1)
    }
    val goToNextScreen = {
        currentStep.value = if (currentStep.value < steps) currentStep.value + 1 else 1
    }

    val isPressed = remember { mutableStateOf(false) }
    val needsToBePaused =
        remember(homeState) { mutableStateOf(homeState.value.btnScanState.progressState == ProgressState.Loading) }
    val pause = isPressed.value || needsToBePaused.value

    val hideContent = remember { mutableStateOf(true) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
            .pointerInput(Unit) {
                val maxWidth = this.size.width
                detectTapGestures(
                    onPress = {
                        val pressStartTime = System.currentTimeMillis()
                        isPressed.value = true
                        this.tryAwaitRelease()
                        val pressEndTime = System.currentTimeMillis()
                        val totalPressTime = pressEndTime - pressStartTime
                        if (totalPressTime < 200) {
                            val isTapOnRightTwoTiers = (it.x > (maxWidth / 2))
                            if (isTapOnRightTwoTiers) {
                                goToNextScreen()
                            } else {
                                goToPreviousScreen()
                            }
                        }
                        isPressed.value = false
                    },
                )
            }
    ) {
        if (!isDarkBackground) {
            Image(
                painter = painterResource(id = R.drawable.ic_overlay),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
            Modifier
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.size(24.dp))
            StoriesProgressBar(
                steps = steps,
                currentStep = currentStep.value,
//                paused = isPressed.value,
                stepDuration = stepDuration,
                paused = pause,
                onStepFinished = goToNextScreen,
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
                colorFilter = if (isDarkBackground) null else ColorFilter.tint(Color.Black)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
        ) {
            when (currentStep.value) {
                1 -> FirstStoriesContent(pause, stepDuration) { hideContent.value = it }
                2 -> StoriesRevolutionaryWallet()
                3 -> StoriesUltraSecureBackup()
                4 -> StoriesThousandsOfCurrencies()
                5 -> StoriesWeb3()
                6 -> StoriesWalletForEveryone()
            }
        }
        HomeButtons(
            isDarkBackground = isDarkBackground,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(37.dp),
            onScanButtonClick = onScanButtonClick,
            onShopButtonClick = onShopButtonClick
        )
    }
}


@Preview
@Composable
fun InstagramScreenPreview() {
    StoriesScreen(onScanButtonClick = {}, onShopButtonClick = {})
}