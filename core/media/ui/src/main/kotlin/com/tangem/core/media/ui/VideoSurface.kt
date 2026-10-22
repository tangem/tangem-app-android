package com.tangem.core.media.ui

import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import com.tangem.core.media.VideoSurfaceState
import kotlin.math.roundToInt

/**
 * A video filling its bounds like [ContentScale.Crop], covered by [poster] until its first frame is on screen.
 *
 * @param poster stays for good if the video fails before its first frame; ideally that frame itself
 */
@Composable
fun VideoSurface(
    state: VideoSurfaceState,
    modifier: Modifier = Modifier,
    poster: @Composable BoxScope.() -> Unit = {},
) {
    val frameSize = state.frameSize
    Box(modifier = modifier) {
        // A controller swapped in gets a surface only from a new factory call.
        key(state.onAvailable, state.key) {
            AndroidView(
                modifier = Modifier.coverSize(aspectRatio = frameSize.aspectRatio),
                factory = { context ->
                    SurfaceView(context).also { surfaceView ->
                        // A layout pass must not resize the surface under the decoder ([REDACTED_TASK_KEY]).
                        surfaceView.holder.setFixedSize(frameSize.width, frameSize.height)
                        state.onAvailable(surfaceView)
                    }
                },
                onRelease = state.onRelease,
            )
        }
        // Outside the key, so a replacement surface comes up under the poster.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = if (state.isFirstFrameShown) 0f else 1f },
            content = poster,
        )
    }
}

/** In the frame's own shape: some vendor decoders ignore media3's crop and stretch the frame to the surface. */
private fun Modifier.coverSize(aspectRatio: Float): Modifier = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val isCoveredByHeight = width < height * aspectRatio
    val coverWidth = if (isCoveredByHeight) (height * aspectRatio).roundToInt() else width
    val coverHeight = if (isCoveredByHeight) height else (width / aspectRatio).roundToInt()
    val placeable = measurable.measure(Constraints.fixed(width = coverWidth, height = coverHeight))
    layout(width = width, height = height) {
        placeable.place(x = (width - coverWidth) / 2, y = (height - coverHeight) / 2)
    }
}