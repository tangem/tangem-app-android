package com.tangem.core.ui.components.stories.inner

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.test.SwapStoriesScreenTestTags
import kotlinx.coroutines.delay

private const val STORIES_ANIMATION_SPEED_ZERO_DURATION = 3000L
const val STORY_DURATION = 8_000

private val DEFAULT_CONTENT_PADDING = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp)
private val DEFAULT_HEIGHT = 2.dp
private const val DEFAULT_INACTIVE_ALPHA = .2f

private suspend fun animationProgress(
    paused: Boolean,
    animatorSpeed: Float,
    stepDuration: Int,
    holdWhenFinished: Boolean = false,
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

            if (!holdWhenFinished) progress.snapTo(0f)
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
    height: Dp = DEFAULT_HEIGHT,
    segmentWidth: Dp? = null,
    inactiveAlpha: Float = DEFAULT_INACTIVE_ALPHA,
    contentPadding: PaddingValues = DEFAULT_CONTENT_PADDING,
    holdWhenFinished: Boolean = false,
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
            holdWhenFinished = holdWhenFinished,
            progress = progress,
            onStepFinish = onStepFinish,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(contentPadding),
    ) {
        for (index in 0..steps) {
            val sizeModifier = if (segmentWidth == null) Modifier.weight(1f) else Modifier.width(segmentWidth)
            Row(
                modifier = Modifier
                    .height(height)
                    .then(sizeModifier)
                    .clip(CircleShape)
                    .background(TangemColorPalette.White.copy(alpha = inactiveAlpha))
                    .testTag(SwapStoriesScreenTestTags.PROGRESS_BAR_ITEM),
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(TangemColorPalette.White)
                        .fillMaxHeight()
                        .let { modifier ->
                            when (index) {
                                currentStep -> modifier.fillMaxWidth(progress.value)
                                in 0..currentStep -> modifier.fillMaxWidth(fraction = 1f)
                                else -> modifier
                            }
                        },
                )
            }
            if (index != steps) SpacerW4()
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
            .padding(vertical = 16.dp),
    ) {
        StoriesProgressBar(steps = 5, currentStep = 3, paused = false)
    }
}

@Preview
@Composable
private fun StoriesProgressBarPillsPreview() {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .background(TangemColorPalette.Black)
            .padding(vertical = 16.dp),
    ) {
        StoriesProgressBar(
            steps = 4,
            currentStep = 1,
            paused = true,
            height = 6.dp,
            segmentWidth = 32.dp,
            inactiveAlpha = .1f,
            contentPadding = PaddingValues(),
        )
    }
}