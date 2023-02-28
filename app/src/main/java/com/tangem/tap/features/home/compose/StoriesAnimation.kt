package com.tangem.tap.features.home.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.tangem.tap.common.compose.extensions.AnimatedValue
import com.tangem.tap.common.compose.extensions.toAnimatable

private const val SCALE_SWITCH_BARRIER = 1.15f

@Suppress("LongParameterList")
@Composable
fun HorizontalSlidingImage(
    painter: Painter,
    paused: Boolean,
    duration: Int,
    itemSize: DpSize,
    startOffset: Float,
    targetOffset: Float,
    contentDescription: String,
) {
    val translateX = AnimatedValue(startOffset * -1f, (startOffset + targetOffset) * -1f)

    Image(
        modifier = Modifier
            .requiredWidth(itemSize.width)
            .requiredHeight(itemSize.height)
            .graphicsLayer(
                translationX = translateX.toAnimatable(isPaused = paused, duration = duration).value,
            ),
        alignment = Alignment.TopStart,
        contentScale = ContentScale.FillBounds,
        painter = painter,
        contentDescription = contentDescription,
    )
}

@Composable
fun StoriesTextAnimation(
    slideInDuration: Int = 500,
    slideInDelay: Int = 200,
    slideDistance: Dp = 60.dp,
    label: String = "",
    content: @Composable (Modifier) -> Unit,
) {
    val isLaunched = remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = isLaunched.value, label = label)

    val offsetY = transition.animateDp(
        transitionSpec = {
            tween(
                durationMillis = slideInDuration,
                delayMillis = slideInDelay,
                easing = FastOutSlowInEasing,
            )
        },
        label = "Slide in",
    ) { value -> if (value) 0.dp else slideDistance }

    val alpha = transition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = slideInDuration * 2,
                delayMillis = slideInDelay,
                easing = FastOutSlowInEasing,
            )
        },
        label = "Visibility",
    ) { value -> if (value) 1f else 0f }

    content(
        Modifier
            .absoluteOffset(y = offsetY.value)
            .alpha(alpha.value),
    )

    LaunchedEffect(Unit) { isLaunched.value = true }
}

@Composable
fun StoriesBottomImageAnimation(
    initialScale: Float = 2.5f,
    firstStepDuration: Int,
    totalDuration: Int,
    content: @Composable (Modifier) -> Unit,
) {
    val secondStepDuration = totalDuration - firstStepDuration

    val isFirstStepLaunched = remember { mutableStateOf(false) }
    val isSecondStepLaunched = remember { mutableStateOf(false) }

    val firstTransition = updateTransition(
        targetState = isFirstStepLaunched.value,
        label = "Image appearing",
    )
    val firstScaleStep = firstTransition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = firstStepDuration,
                easing = FastOutLinearInEasing,
            )
        },
        label = "Appearing scale",
    ) { value -> if (value) SCALE_SWITCH_BARRIER else initialScale }

    val secondTransition = updateTransition(
        targetState = isSecondStepLaunched.value,
        label = "Image slow outgoing",
    )
    val secondScaleStep = secondTransition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = secondStepDuration,
                easing = LinearEasing,
            )
        },
        label = "Outgoing scale",
    ) { value -> if (value) 1f else SCALE_SWITCH_BARRIER }

    val fadeIn = firstTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 400) },
        label = "Fade in on start",
    ) { value -> if (value) 1f else 0f }

    if (firstScaleStep.value == SCALE_SWITCH_BARRIER) {
        isSecondStepLaunched.value = true
    }

    val modifier = if (!isSecondStepLaunched.value) {
        Modifier.scale(firstScaleStep.value)
    } else {
        Modifier.scale(secondScaleStep.value)
    }.alpha(fadeIn.value)

    content(modifier)

    LaunchedEffect(Unit) { isFirstStepLaunched.value = true }
}
