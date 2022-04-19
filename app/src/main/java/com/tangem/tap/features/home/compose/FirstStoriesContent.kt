package com.tangem.tap.features.home.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.wallet.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FirstStoriesContent(
    paused: Boolean, duration: Int = 8_000,
    hideContent: (Boolean) -> Unit
) {
    val screenState = remember { mutableStateOf(StartingScreenState.INIT) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(paused) {
        if (paused) {
            progress.stop()
        } else {
            progress.animateTo(
                targetValue = 2f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = LinearEasing
                )
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

    if (screenState.value == StartingScreenState.INIT) hideContent(true)
    if (screenState.value == StartingScreenState.BUY) hideContent(false)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
        Modifier
            .fillMaxSize(),
    ) {

        val text = when (screenState.value) {
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

        val style = TextStyle(
            fontSize = 60.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        if (screenState.value != StartingScreenState.MEET_TANGEM) {
            TextAutoSize(
                modifier = Modifier
                    .padding(start = 40.dp, end = 40.dp, bottom = 100.dp)
                    .alpha(if (screenState.value == StartingScreenState.SHOW_CARD) 0f else 1f),
                text = text?.let { stringResource(text) } ?: "",
                textStyle = style,
                fontSizeRange = FontSizeRange(40.sp, 60.sp)
            )
        }

        AnimatedVisibility(
            visible = screenState.value == StartingScreenState.MEET_TANGEM,
            enter = slideInVertically() { it / 2 }
        ) {
            TextAutoSize(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 40.dp),
                text = text?.let { stringResource(text) } ?: "",
                textStyle = style,
                fontSizeRange = FontSizeRange(20.sp, 60.sp)
            )
        }

        AnimatedVisibility(
            visible = screenState.value == StartingScreenState.SHOW_CARD ||
                screenState.value == StartingScreenState.MEET_TANGEM,
            enter = scaleIn(initialScale = 3f)
        ) {
            Image(
                painter = painterResource(
                    id = R.drawable.meet_tangem
                ),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

enum class StartingScreenState {
    INIT, BUY, STORE, SEND, PAY, EXCHANGE, BORROW, LEND, SHOW_CARD, MEET_TANGEM
}


@Preview
@Composable
fun Stories1Preview() {
    FirstStoriesContent(false, 7_000) {}
}