package com.tangem.features.tangempay.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

private val CardArtWidth: Dp = 264.dp
private val CardArtHeight: Dp = 172.dp
private val CardArtCornerRadius: Dp = 16.dp
private const val RETRY_PARAMETER = "campaign_card_art_retry"

@Composable
fun TangemPayCampaignCardArt(cardArtUrl: String?, modifier: Modifier = Modifier) {
    if (cardArtUrl == null) {
        Box(
            modifier = modifier
                .cardArtSize()
                .clip(RoundedCornerShape(CardArtCornerRadius)),
        ) {
            CardArtPlaceholder()
        }
        return
    }

    var retryCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val request = remember(cardArtUrl, retryCount) {
        ImageRequest.Builder(context)
            .data(cardArtUrl)
            .crossfade(true)
            .setParameter(RETRY_PARAMETER, retryCount)
            .build()
    }

    Box(
        modifier = modifier
            .cardArtSize()
            .clip(RoundedCornerShape(CardArtCornerRadius)),
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = request,
            loading = { CardArtLoading() },
            error = { CardArtError(onRetryClick = { retryCount++ }) },
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
    }
}

@Composable
fun TangemPayCampaignCardArtPlaceholder(modifier: Modifier = Modifier) {
    TangemShimmer(modifier = modifier.cardArtSize(), radius = CardArtCornerRadius)
}

private fun Modifier.cardArtSize(): Modifier = width(CardArtWidth).aspectRatio(CardArtWidth / CardArtHeight)

@Composable
private fun CardArtLoading(modifier: Modifier = Modifier) {
    TangemShimmer(modifier = modifier.fillMaxSize(), radius = CardArtCornerRadius)
}

@Composable
private fun CardArtPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.opaque.secondary),
    )
}

@Composable
private fun CardArtError(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCampaignCardArtPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TangemPayCampaignCardArt(cardArtUrl = "https://app.tangem.com/cards/card_default.png")
            TangemPayCampaignCardArtPlaceholder()
            Box(modifier = Modifier.cardArtSize().clip(RoundedCornerShape(CardArtCornerRadius))) {
                CardArtError(onRetryClick = {})
            }
        }
    }
}