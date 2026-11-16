package com.tangem.features.polymarket.impl.onboarding.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.tangem.core.media.VideoSurfaceState
import com.tangem.core.media.ui.VideoSurface
import com.tangem.features.polymarket.impl.R as PolymarketR

@Composable
internal fun BoxScope.PolymarketOnboardingHeroMedia(video: VideoSurfaceState?) {
    if (video == null) {
        HeroPoster(modifier = Modifier.matchParentSize())
    } else {
        VideoSurface(state = video, modifier = Modifier.matchParentSize()) {
            HeroPoster(modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
private fun HeroPoster(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(PolymarketR.drawable.ill_polymarket_welcome_poster),
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
}