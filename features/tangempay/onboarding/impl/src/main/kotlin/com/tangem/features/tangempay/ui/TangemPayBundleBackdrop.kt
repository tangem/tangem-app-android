package com.tangem.features.tangempay.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.tangem.core.media.VideoSurfaceState
import com.tangem.core.media.ui.VideoSurface
import com.tangem.features.tangempay.onboarding.impl.R

private const val BACKDROP_ASPECT_RATIO = 9f / 16f

private const val ENTER_TRANSITION_MILLIS = 500

@Composable
internal fun TangemPayBundleBackdrop(video: VideoSurfaceState?) {
    val modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(align = Alignment.Top, unbounded = true)
        .aspectRatio(BACKDROP_ASPECT_RATIO)

    if (video == null || !rememberIsEnterTransitionOver()) {
        Poster(modifier = modifier)
    } else {
        VideoSurface(state = video, modifier = modifier) {
            Poster(modifier = Modifier.matchParentSize())
        }
    }
}

/**
 * The replaced screen stays drawn until the routing transition ends, and two SurfaceViews in one window have no
 * defined order: this video could show through the hole of that screen's own.
 */
@Composable
private fun rememberIsEnterTransitionOver(): Boolean = produceState(initialValue = false) {
    animate(initialValue = 0f, targetValue = 1f, animationSpec = tween(ENTER_TRANSITION_MILLIS)) { _, _ -> }
    value = true
}.value

@Composable
private fun Poster(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(R.drawable.ill_tangem_pay_bundle_poster),
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
}