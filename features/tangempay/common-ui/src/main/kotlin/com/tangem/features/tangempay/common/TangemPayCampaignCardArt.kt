package com.tangem.features.tangempay.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

private val CardArtWidth: Dp = 266.dp
private val CardArtHeight: Dp = 172.dp
private val CardArtCornerRadius: Dp = 16.dp
private val CardArtShape = RoundedCornerShape(CardArtCornerRadius)

@Composable
fun TangemPayCampaignCardArt(cardArtUrl: String?, modifier: Modifier = Modifier) {
    CardArtSurface(modifier = modifier) {
        if (cardArtUrl == null) {
            CardArtPlaceholder()
        } else {
            CardArtImage(cardArtUrl = cardArtUrl)
        }
    }
}

@Composable
private fun CardArtImage(cardArtUrl: String) {
    var retryCount by remember { mutableIntStateOf(0) }

    SubcomposeAsyncImage(
        modifier = Modifier.fillMaxSize(),
        model = rememberCardArtRequest(url = cardArtUrl, retryCount = retryCount),
        loading = { CardArtLoading() },
        error = { TangemPayCardArtError(onRetryClick = { retryCount++ }) },
        success = {
            SubcomposeAsyncImageContent(
                modifier = Modifier.border(
                    width = 1.dp,
                    color = TangemTheme.colors3.border.secondary,
                    shape = CardArtShape,
                ),
            )
        },
        contentScale = ContentScale.Crop,
        contentDescription = null,
    )
}

@Composable
fun TangemPayCampaignCardArtPlaceholder(modifier: Modifier = Modifier) {
    CardArtSurface(modifier = modifier) { CardArtLoading() }
}

@Composable
private fun CardArtSurface(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .width(CardArtWidth)
            .aspectRatio(CardArtWidth / CardArtHeight)
            .clip(CardArtShape),
        content = content,
    )
}

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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCampaignCardArtPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TangemPayCampaignCardArt(cardArtUrl = "https://app.tangem.com/cards/card_default.png")
            TangemPayCampaignCardArt(cardArtUrl = null)
            TangemPayCampaignCardArtPlaceholder()
            CardArtSurface { TangemPayCardArtError(onRetryClick = {}) }
        }
    }
}