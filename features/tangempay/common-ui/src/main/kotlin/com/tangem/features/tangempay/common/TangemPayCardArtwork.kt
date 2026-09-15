package com.tangem.features.tangempay.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cloud_12_filled

private const val CARD_ASPECT_RATIO = 1.585f
private val CardCornerRadius = 12.dp
private val CardShape = RoundedCornerShape(CardCornerRadius)

@Composable
fun TangemPayCardArtwork(imageUrl: String?, isVirtual: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT_RATIO)
            .clip(CardShape),
    ) {
        if (imageUrl == null) {
            CardArtworkShimmer()
        } else {
            CardImage(imageUrl = imageUrl, isVirtual = isVirtual)
        }
    }
}

@Composable
private fun CardImage(imageUrl: String, isVirtual: Boolean) {
    var retryCount by remember { mutableIntStateOf(0) }

    SubcomposeAsyncImage(
        modifier = Modifier.fillMaxSize(),
        model = rememberCardArtRequest(url = imageUrl, retryCount = retryCount),
        contentScale = ContentScale.Crop,
        contentDescription = null,
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> CardArtworkShimmer()
            is AsyncImagePainter.State.Error -> TangemPayCardArtError(onRetryClick = { retryCount++ })
            else -> {
                SubcomposeAsyncImageContent()
                if (isVirtual) VirtualBadge()
            }
        }
    }
}

@Composable
private fun CardArtworkShimmer() {
    TangemShimmer(modifier = Modifier.fillMaxSize(), radius = CardCornerRadius)
}

@Composable
private fun VirtualBadge() {
    Box(modifier = Modifier.fillMaxSize()) {
        Icon(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(20.dp),
            imageVector = Icons.ic_cloud_12_filled,
            tint = TangemTheme.colors3.icon.staticDark,
            contentDescription = null,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCardArtworkPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(horizontal = 48.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TangemPayCardArtwork(imageUrl = "https://app.tangem.com/cards/card_default.png", isVirtual = true)
            TangemPayCardArtwork(imageUrl = null, isVirtual = false)
            PreviewFrame { TangemPayCardArtError(onRetryClick = {}) }
        }
    }
}

@Composable
private fun PreviewFrame(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT_RATIO)
            .clip(CardShape),
        content = content,
    )
}