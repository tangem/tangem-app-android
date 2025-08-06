package com.tangem.features.home.impl.ui.compose.views

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import kotlinx.coroutines.delay

private const val STORIES_ANIMATION_SPEED_ZERO_DURATION = 3000L

@Composable
fun StoriesProgressBar(
    steps: Int,
    currentStep: Int,
    paused: Boolean = false,
    stepDuration: Int = 8_000,
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                top = TangemTheme.dimens.spacing16,
            ),
    ) {
        for (index in 0..steps) {
            Row(
                modifier = Modifier
                    .height(TangemTheme.dimens.size2)
                    .weight(1f)
                    .clip(RoundedCornerShape(TangemTheme.dimens.radius2))
                    .background(TangemColorPalette.White.copy(alpha = .2f)),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(TangemTheme.dimens.radius2))
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
            if (index != steps) {
                SpacerW4()
            }
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