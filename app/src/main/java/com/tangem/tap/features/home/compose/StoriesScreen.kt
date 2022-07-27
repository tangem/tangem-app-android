package com.tangem.tap.features.home.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.tap.features.home.compose.content.FirstStoriesContent
import com.tangem.tap.features.home.compose.content.StoriesCurrencies
import com.tangem.tap.features.home.compose.content.StoriesRevolutionaryWallet
import com.tangem.tap.features.home.compose.content.StoriesUltraSecureBackup
import com.tangem.tap.features.home.compose.content.StoriesWalletForEveryone
import com.tangem.tap.features.home.compose.content.StoriesWeb3
import com.tangem.tap.features.home.compose.views.HomeButtons
import com.tangem.tap.features.home.compose.views.StoriesProgressBar
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
    val currentStep = remember { mutableStateOf(1) }
    val systemUiController = rememberSystemUiController()

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
    val isPaused = isPressed.value || needsToBePaused.value

    val hideContent = remember { mutableStateOf(true) }

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = false,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
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
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.ic_overlay),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
        }

        val insets = WindowInsets.systemBars
            .union(WindowInsets(top = 32.dp))

        Column(
            modifier = Modifier
                .windowInsetsPadding(insets)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StoriesProgressBar(
                steps = steps,
                currentStep = currentStep.value,
                stepDuration = currentStep.duration(),
                paused = isPaused,
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
                1 -> FirstStoriesContent(isPaused, currentStep.duration()) { hideContent.value = it }
                2 -> StoriesRevolutionaryWallet(currentStep.duration())
                3 -> StoriesUltraSecureBackup(isPaused, currentStep.duration())
                4 -> StoriesCurrencies(isPaused, currentStep.duration())
                5 -> StoriesWeb3(isPaused, currentStep.duration())
                6 -> StoriesWalletForEveryone(currentStep.duration())
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
                    .height(48.dp),
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

private fun MutableState<Int>.duration(): Int = when (this.value) {
    1 -> 8000
    else -> 6000
}

@Preview
@Composable
fun StoriesScreenPreview() {
    StoriesScreen(onScanButtonClick = {}, onShopButtonClick = {}, onSearchTokensClick = {})
}
