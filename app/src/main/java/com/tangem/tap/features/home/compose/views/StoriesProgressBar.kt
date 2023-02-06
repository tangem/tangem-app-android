package com.tangem.tap.features.home.compose.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerW4

@Composable
fun StoriesProgressBar(
    steps: Int,
    currentStep: Int,
    paused: Boolean = false,
    stepDuration: Int = 8_000,
    onStepFinish: () -> Unit = {},
) {
    val progress = remember(currentStep) { Animatable(0f) }

    LaunchedEffect(paused, currentStep) {
        if (paused) {
            progress.stop()
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (stepDuration * (1f - progress.value)).toInt(),
                    easing = LinearEasing
                )
            )
            progress.snapTo(0f)
            onStepFinish()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
//            .height()
            .padding(start = 9.dp, end = 9.dp, top = 16.dp),
    ) {
        for (index in 1..steps) {
            Row(
                modifier = Modifier
                    .height(2.dp)
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White)
                        .fillMaxHeight().let {
                            when (index) {
                                currentStep -> it.fillMaxWidth(progress.value)
                                in 0..currentStep -> it.fillMaxWidth(1f)
                                else -> it
                            }
                        },
                ) {}
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
    StoriesProgressBar(steps = 3, currentStep = 2, paused = false) { }
}
