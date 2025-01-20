package com.tangem.core.ui.components.stories.inner

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import kotlinx.coroutines.delay

private const val STORIES_ANIMATION_SPEED_ZERO_DURATION = 3000L
const val STORY_DURATION = 8_000

private suspend fun animationProgress(
    paused: Boolean,
    animatorSpeed: Float,
    stepDuration: Int,
    progress: Animatable<Float, AnimationVector1D>,
    onStepFinish: () -> Unit,
) {
    if (paused) {
        progress.stop()
    } else {
        if (animatorSpeed == 0f) {
            progress.snapTo(1f)
            delay(STORIES_ANIMATION_SPEED_ZERO_DURATION)
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (stepDuration * (1f - progress.value)).toInt(),
                    easing = LinearEasing,
                ),
            )
            progress.snapTo(0f)
        }
        onStepFinish()
    }
}

@Composable
fun StoriesProgressBar(
    steps: Int,
    currentStep: Int,
    paused: Boolean = false,
    stepDuration: Int = STORY_DURATION,
    onStepFinish: () -> Unit = {},
) {
    val progress = remember(currentStep) { Animatable(initialValue = 0f) }

    val context = LocalContext.current
    val animatorSpeed = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )

    LaunchedEffect(paused, currentStep, animatorSpeed) {
        animationProgress(
            paused = paused,
            animatorSpeed = animatorSpeed,
            stepDuration = stepDuration,
            progress = progress,
            onStepFinish = onStepFinish,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .statusBarsPadding()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
            ),
    ) {
        for (index in 0..steps) {
            Row(
                modifier = Modifier
                    .height(2.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TangemColorPalette.White.copy(alpha = .2f)),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(TangemColorPalette.White)
                        .fillMaxHeight()
                        .let {
                            when (index) {
                                currentStep -> it.fillMaxWidth(progress.value)
                                in 0..currentStep -> it.fillMaxWidth(fraction = 1f)
                                else -> it
                            }
                        },
                )
            }
            SpacerW4()
        }
    }
}

@Preview
@Composable
private fun StoriesProgressBarPreview() {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .background(TangemColorPalette.Black)
            .padding(vertical = TangemTheme.dimens.spacing16),
    ) {
        StoriesProgressBar(steps = 5, currentStep = 3, paused = false)
    }
}