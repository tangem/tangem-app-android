package com.tangem.tap.features.home.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.tap.common.compose.SpacerS24
import androidx.compose.ui.unit.sp
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.wallet.R
import kotlin.math.max

@Composable
fun StoriesScreen(
    homeState: MutableState<HomeState> = mutableStateOf(HomeState()),
    onScanButtonClick: () -> Unit,
    onShopButtonClick: () -> Unit,
    onSearchTokensClick: () -> Unit,
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
    ) {
        Row(
            Modifier.fillMaxSize()
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
                    }
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
                    }
            )
        }
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
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            SpacerS24()
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
            when (currentStep.value) {
                1 -> FirstStoriesContent(pause, stepDuration) { hideContent.value = it }
                2 -> StoriesRevolutionaryWallet()
                3 -> StoriesUltraSecureBackup()
                4 -> StoriesThousandsOfCurrencies()
                5 -> StoriesWeb3()
                6 -> StoriesWalletForEveryone()
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (currentStep.value == 4) Button(
                onClick = onSearchTokensClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .height(48.dp)
                ,
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = Color.White,
                    contentColor = Color(0xFF080C10)
                )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = null
                )
                Text(
                    text = stringResource(id = R.string.search_tokens_title),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
            HomeButtons(
                isDarkBackground = isDarkBackground,
                modifier = Modifier
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 37.dp)
                    .fillMaxWidth(),
                onScanButtonClick = onScanButtonClick,
                onShopButtonClick = onShopButtonClick
            )
        }

    }
}


@Preview
@Composable
fun InstagramScreenPreview() {
    StoriesScreen(onScanButtonClick = {}, onShopButtonClick = {}, onSearchTokensClick = {})
}