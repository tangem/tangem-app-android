package com.tangem.features.storiesv2.impl.ui

import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.tangem.features.storiesv2.impl.content.StoryV2ContentMode
import com.tangem.features.storiesv2.impl.content.StoryV2MediaRef
import com.tangem.features.storiesv2.impl.ui.state.StoriesV2UM
import java.io.File

/**
 * The picture half of a slide.
 *

 * putting a new one up is itself a black flash.
 *
 * The poster sits *above* the surface, not below it. A SurfaceView punches a transparent hole through its window, so
 * anything drawn before it is erased in that region and only what is drawn after survives. It doubles as the shutter,
 * and because a poster is the asset's own first frame, swapping it for the video is a change of nothing at all.
 */
@Composable
internal fun StorySlideContent(
    slide: StoriesV2UM.SlideUM,
    handle: StoryPlayerHandle,
    videoAspectRatio: Float?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (handle.hasVideo) {
            VideoSurface(handle = handle, contentMode = slide.contentMode, aspectRatio = videoAspectRatio)
        }

        val isCoveredByAsset = slide.content is StoriesV2UM.SlideUM.Content.Video && slide.isAssetReady
        StoryMedia(
            ref = slide.poster,
            contentMode = slide.contentMode,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (isCoveredByAsset) 0f else 1f },
        )

        val content = slide.content
        if (content is StoriesV2UM.SlideUM.Content.VectorAnimation) {
            StoryVectorAnimation(
                source = content.source,
                positionMs = handle.slidePositionMs,
                durationMs = slide.durationMs,
                contentMode = slide.contentMode,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BoxScope.VideoSurface(handle: StoryPlayerHandle, contentMode: StoryV2ContentMode, aspectRatio: Float?) {
    val sizeModifier = if (contentMode == StoryV2ContentMode.CONTAIN && aspectRatio != null) {
        Modifier.aspectRatio(aspectRatio).align(Alignment.Center)
    } else {
        Modifier.fillMaxSize()
    }

    AndroidView(
        factory = { context -> SurfaceView(context).also(handle::attachSurface) },
        modifier = sizeModifier,
        onRelease = { surfaceView -> handle.detachSurface(surfaceView) },
    )
}

@Composable
internal fun StoryMedia(ref: StoryV2MediaRef, contentMode: StoryV2ContentMode, modifier: Modifier = Modifier) {
    val contentScale = when (contentMode) {
        StoryV2ContentMode.COVER -> ContentScale.Crop
        StoryV2ContentMode.CONTAIN -> ContentScale.Fit
    }

    when (ref) {
        // Decoded synchronously by the resource system, which is the point: an asynchronous poster would flash the
        // very frame it exists to prevent.
        is StoryV2MediaRef.DrawableResource -> Image(
            painter = painterResource(id = ref.id),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier,
        )
        is StoryV2MediaRef.LocalFile -> AsyncImage(
            model = File(ref.path),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier,
        )
        is StoryV2MediaRef.RawResource -> Unit
    }
}