package com.tangem.tap.features.home.compose.content

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.compose.FontSizeRange
import com.tangem.tap.common.compose.TextAutoSize
import com.tangem.tap.features.home.compose.StoriesBottomImageAnimation
import com.tangem.tap.features.home.compose.StoriesTextAnimation
import com.tangem.wallet.R

@Suppress("LongMethod", "ComplexMethod", "MagicNumber")
@Composable
fun FirstStoriesContent(
    isPaused: Boolean,
    duration: Int,
    isNewWalletAvailable: MutableState<Boolean>,
    onHideContent: (Boolean) -> Unit,
) {
    val bottomInsetsPx = WindowInsets.navigationBars.getBottom(LocalDensity.current)

    val screenState = remember { mutableStateOf(StartingScreenState.INIT) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isPaused) {
        if (isPaused) {
            progress.stop()
        } else {
            progress.animateTo(
                targetValue = 2f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = LinearEasing,
                ),
            )
        }
    }

    when (progress.value) {
        in 0f..0.2f -> screenState.value = StartingScreenState.INIT
        in 0.2f..0.3f -> screenState.value = StartingScreenState.BUY
        in 0.3f..0.4f -> screenState.value = StartingScreenState.STORE
        in 0.4f..0.5f -> screenState.value = StartingScreenState.SEND
        in 0.5f..0.6f -> screenState.value = StartingScreenState.PAY
        in 0.6f..0.7f -> screenState.value = StartingScreenState.EXCHANGE
        in 0.7f..0.8f -> screenState.value = StartingScreenState.BORROW
        in 0.8f..1f -> screenState.value = StartingScreenState.LEND
        in 1f..1.2f -> screenState.value = StartingScreenState.SHOW_CARD
        in 1.2f..2f -> screenState.value = StartingScreenState.MEET_TANGEM
    }

    if (screenState.value == StartingScreenState.INIT) onHideContent(true)
    if (screenState.value == StartingScreenState.BUY) onHideContent(false)

    val style = TextStyle(
        fontSize = 60.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        textAlign = TextAlign.Center,
    )
    val textId = screenState.textId()

    Box(modifier = Modifier.fillMaxSize()) {
        if (screenState.isSplashingTextDisplaying()) {
            TextAutoSize(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(start = 20.dp, end = 20.dp, bottom = 100.dp),
                text = textId?.let { stringResource(textId) } ?: "",
                textStyle = style,
                fontSizeRange = FontSizeRange(20.sp, 60.sp),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.7f),
                ) {
                    if (screenState.isMeetTangemDisplaying()) {
                        StoriesTextAnimation(
                            slideInDelay = 0,
                        ) { modifier ->
                            TextAutoSize(
                                modifier = modifier
                                    .padding(start = 20.dp, end = 20.dp, top = 50.dp)
                                    .alpha(if (screenState.value == StartingScreenState.SHOW_CARD) 0f else 1f),
                                text = textId?.let { stringResource(textId) } ?: "",
                                textStyle = style,
                                fontSizeRange = FontSizeRange(30.sp, 50.sp),
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .wrapContentSize(),
                ) {
                    val painter = if (isNewWalletAvailable.value) {
                        painterResource(id = R.drawable.img_meet_tangem2)
                    } else {
                        painterResource(id = R.drawable.img_meet_tangem)
                    }
                    StoriesBottomImageAnimation(
                        totalDuration = duration,
                        firstStepDuration = 400,
                    ) { modifier ->
                        Image(
                            modifier = modifier.fillMaxWidth(),
                            painter = painter,
                            contentDescription = "Tangem Wallet card",
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(TangemTheme.dimens.size72 + bottomInsetsPx.dp)
                .background(BottomGradient),
        )
    }
}

private val BottomGradient: Brush = Brush.verticalGradient(
    colors = listOf(
        TangemColorPalette.Black.copy(alpha = 0f),
        TangemColorPalette.Black.copy(alpha = 0.75f),
        TangemColorPalette.Black.copy(alpha = 0.95f),
        TangemColorPalette.Black,
    ),
)

private enum class StartingScreenState {
    INIT, BUY, STORE, SEND, PAY, EXCHANGE, BORROW, LEND, SHOW_CARD, MEET_TANGEM
}

@StringRes
private fun MutableState<StartingScreenState>.textId(): Int? = when (this.value) {
    StartingScreenState.INIT -> null
    StartingScreenState.BUY -> R.string.story_meet_buy
    StartingScreenState.STORE -> R.string.story_meet_store
    StartingScreenState.SEND -> R.string.story_meet_send
    StartingScreenState.PAY -> R.string.story_meet_pay
    StartingScreenState.EXCHANGE -> R.string.story_meet_exchange
    StartingScreenState.BORROW -> R.string.story_meet_borrow
    StartingScreenState.LEND -> R.string.story_meet_lend
    StartingScreenState.SHOW_CARD -> R.string.story_meet_title
    StartingScreenState.MEET_TANGEM -> R.string.story_meet_title
}

private fun MutableState<StartingScreenState>.isSplashingTextDisplaying(): Boolean {
    return this.value != StartingScreenState.MEET_TANGEM &&
        this.value != StartingScreenState.SHOW_CARD
}

private fun MutableState<StartingScreenState>.isMeetTangemDisplaying(): Boolean {
    return this.value == StartingScreenState.MEET_TANGEM
}

@Preview
@Composable
private fun FirstStoriesPreview() {
    FirstStoriesContent(
        false,
        8000,
        remember {
            mutableStateOf(false)
        },
    ) {}
}