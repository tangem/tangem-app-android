package com.tangem.features.tangempay.campaign.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun TangemPayCampaignCardArt(cardArtUrl: String, modifier: Modifier = Modifier) {
    var loadAttempt by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .width(CampaignCardArtWidth)
            .aspectRatio(CAMPAIGN_CARD_ART_ASPECT_RATIO)
            .clip(RoundedCornerShape(CampaignCardArtCornerRadius)),
    ) {
        key(loadAttempt) {
            SubcomposeAsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(cardArtUrl)
                    .crossfade(true)
                    .build(),
                loading = { TangemPayCampaignCardArtLoading() },
                error = { TangemPayCampaignCardArtError(onRetryClick = { loadAttempt++ }) },
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
        }
    }
}

@Composable
internal fun TangemPayCampaignCardArtLoading(modifier: Modifier = Modifier) {
    TangemShimmer(modifier = modifier.fillMaxSize(), radius = CampaignCardArtCornerRadius)
}

@Composable
internal fun TangemPayCampaignCardArtError(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.opaque.secondary),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.inverse)
                .clickable(role = Role.Button, onClick = onRetryClick)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_refresh_24),
                contentDescription = stringResourceSafe(R.string.common_reload),
                tint = TangemTheme.colors3.icon.inverse,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}