package com.tangem.tap.features.home.compose.uiTools

import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

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
    val offsetX = remember { Animatable(startOffset * -1f) }

    Image(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.toInt(), 0) }
            .requiredWidth(itemSize.width)
            .requiredHeight(itemSize.height),
        alignment = Alignment.TopStart,
        contentScale = ContentScale.FillBounds,
        painter = painter,
        contentDescription = contentDescription,
    )

    LaunchedEffect(paused) {
        if (paused) {
            offsetX.stop()
        } else {
            offsetX.animateTo(
                targetValue = (startOffset + targetOffset) * -1f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = LinearEasing,
                ),
            )
        }
    }
}

@Composable
fun StoriesTextAnimation(
    slideInDuration: Int = 500,
    slideInDelay: Int = 200,
    slideDistance: Dp = 60.dp,
    label: String = "",
    content: @Composable (Modifier) -> Unit
) {
    val isLaunched = remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = isLaunched.value, label = label)

    val offsetY = transition.animateDp(
        transitionSpec = {
            tween(
                durationMillis = slideInDuration,
                delayMillis = slideInDelay,
                easing = FastOutSlowInEasing
            )
        },
        label = "Slide in"
    ) { value -> if (value) 0.dp else slideDistance }

    val alpha = transition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = slideInDuration * 2,
                delayMillis = slideInDelay,
                easing = FastOutSlowInEasing
            )
        },
        label = "Visibility"
    ) { value -> if (value) 1f else 0f }

    content(
        Modifier
            .absoluteOffset(y = offsetY.value)
            .alpha(alpha.value)
    )

    LaunchedEffect(Unit) { isLaunched.value = true }
}

@Composable
fun StoriesBottomImageAnimation(
    initialScale: Float = 2f,
    firstStepDuration: Int,
    totalDuration: Int,
    content: @Composable (Modifier) -> Unit
) {
    val scaleSwitchBarrier = remember { 1.15f }
    val secondStepDuration = remember { totalDuration - firstStepDuration }

    val isFirstStepLaunched = remember { mutableStateOf(false) }
    val isSecondStepLaunched = remember { mutableStateOf(false) }

    val firstTransition = updateTransition(targetState = isFirstStepLaunched.value, label = "Bottom image animation")
    val secondTransition = updateTransition(targetState = isSecondStepLaunched.value, label = "Bottom image animation")

    val fadeIn = firstTransition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = 400,
            )
        },
        label = "Fade in animation"
    ) { value -> if (value) 1f else 0f }

    val firstScaleStep = firstTransition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = firstStepDuration,
                easing = FastOutLinearInEasing,
            )
        },
        label = "Scale"
    ) { value -> if (value) scaleSwitchBarrier else initialScale }


    val secondScaleStep = secondTransition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = secondStepDuration,
                easing = LinearEasing,
            )
        },
        label = "Scale"
    ) { value -> if (value) 1f else scaleSwitchBarrier }

    if (firstScaleStep.value == scaleSwitchBarrier) isSecondStepLaunched.value = true

    val modifier = if (!isSecondStepLaunched.value) {
        Modifier.scale(firstScaleStep.value)
    } else {
        Modifier.scale(secondScaleStep.value)
    }.alpha(fadeIn.value)

    content(modifier)

    LaunchedEffect(Unit) { isFirstStepLaunched.value = true }
}

@Composable
fun asImageBitmap(@DrawableRes drawableId: Int): ImageBitmap {
    val drawable = AppCompatResources.getDrawable(LocalContext.current, drawableId)
        ?: throw NullPointerException()
    return drawable.toBitmap().asImageBitmap()
}

