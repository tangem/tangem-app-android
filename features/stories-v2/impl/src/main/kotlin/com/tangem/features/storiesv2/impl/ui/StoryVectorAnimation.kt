package com.tangem.features.storiesv2.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LongState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.tangem.features.storiesv2.impl.content.StoryV2ContentMode
import com.tangem.features.storiesv2.impl.content.StoryV2MediaRef

/**
 * A Lottie slide, driven by the story's clock rather than by an animation of its own: an animation shorter than its
 * segment holds its last frame, a longer one never reaches it. A self-driven Lottie would drift from the bar.
 */
@Composable
internal fun StoryVectorAnimation(
    source: StoryV2MediaRef,
    positionMs: LongState,
    durationMs: Long,
    contentMode: StoryV2ContentMode,
    modifier: Modifier = Modifier,
) {
    val spec = when (source) {
        is StoryV2MediaRef.RawResource -> LottieCompositionSpec.RawRes(source.id)
        is StoryV2MediaRef.LocalFile -> LottieCompositionSpec.File(source.path)
        is StoryV2MediaRef.DrawableResource -> return
    }
    val composition by rememberLottieComposition(spec)
    val compositionDurationMs = composition?.duration ?: 0f

    LottieAnimation(
        composition = composition,
        progress = {
            when {
                compositionDurationMs > 0f -> (positionMs.longValue / compositionDurationMs).coerceIn(0f, 1f)
                durationMs > 0L -> (positionMs.longValue.toFloat() / durationMs).coerceIn(0f, 1f)
                else -> 0f
            }
        },
        contentScale = when (contentMode) {
            StoryV2ContentMode.COVER -> ContentScale.Crop
            StoryV2ContentMode.CONTAIN -> ContentScale.Fit
        },
        modifier = modifier,
    )
}