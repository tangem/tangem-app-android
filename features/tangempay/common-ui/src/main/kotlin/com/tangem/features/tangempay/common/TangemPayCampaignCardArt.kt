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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cloud_20_filled

private val CardArtWidth: Dp = 264.dp
private val CardArtHeight: Dp = 172.dp
private val CardArtCornerRadius: Dp = 16.dp
private val CardArtBorderWidth: Dp = 1.dp
private const val RETRY_PARAMETER = "campaign_card_art_retry"

@Composable
fun TangemPayCampaignCardArt(cardArtUrl: String?, modifier: Modifier = Modifier, showVirtualChip: Boolean = false) {
    CardArtSurface(modifier = modifier) {
        if (cardArtUrl == null) {
            CardArtPlaceholder()
        } else {
            CardArtImage(cardArtUrl = cardArtUrl)
        }
        if (showVirtualChip) {
            TangemIcon(
                tangemIconUM = TangemIconUM.Icon(
                    imageVector = Icons.ic_cloud_20_filled,
                    tintReference = ColorReference2 { TangemTheme.colors3.icon.staticDark },
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(20.dp),
            )
        }
    }
}

@Composable
private fun CardArtImage(cardArtUrl: String) {
    var retryCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val request = remember(cardArtUrl, retryCount) {
        ImageRequest.Builder(context)
            .data(cardArtUrl)
            .crossfade(true)
            .setParameter(RETRY_PARAMETER, retryCount)
            .build()
    }

    SubcomposeAsyncImage(
        modifier = Modifier.fillMaxSize(),
        model = request,
        loading = { CardArtLoading() },
        error = { CardArtError(onRetryClick = { retryCount++ }) },
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
    val shape = RoundedCornerShape(CardArtCornerRadius)

    Box(
        modifier = modifier
            .width(CardArtWidth)
            .aspectRatio(CardArtWidth / CardArtHeight)
            .background(color = TangemTheme.colors3.bg.secondary, shape = shape)
            .border(width = CardArtBorderWidth, color = TangemTheme.colors3.border.secondary, shape = shape)
            .clip(shape),
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

@Composable
private fun CardArtError(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X10,
            iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_refresh_24),
            contentDescription = stringResourceSafe(R.string.common_reload),
            onClick = onRetryClick,
        )
    }
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
            TangemPayCampaignCardArt(
                cardArtUrl = "https://app.tangem.com/cards/card_default.png",
                showVirtualChip = true,
            )
            TangemPayCampaignCardArt(cardArtUrl = null)
            TangemPayCampaignCardArtPlaceholder()
            CardArtSurface { CardArtError(onRetryClick = {}) }
        }
    }
}