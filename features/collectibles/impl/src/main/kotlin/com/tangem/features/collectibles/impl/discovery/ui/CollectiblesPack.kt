package com.tangem.features.collectibles.impl.discovery.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_20_filled
import com.tangem.features.collectibles.impl.discovery.ui.preview.CollectiblesDiscoveryPreviewData
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM

internal const val PACK_CARD_ASPECT_RATIO = 180f / 228f

private val GLOW_BLUR_RADIUS = 24.dp

@Composable
internal fun CollectiblesPackCard(state: CollectiblesDiscoveryUM.PackUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickableSingle(onClick = state.onClick),
    ) {
        PackImage(imageUrl = state.imageUrl)
        SpacerH(8.dp)
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SpacerH(2.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.price.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
            )
            if (state.badge != null) {
                TangemBadge(text = state.badge, size = TangemBadge.Size.X4)
            }
        }
    }
}

@Composable
private fun PackImage(imageUrl: String, modifier: Modifier = Modifier) {
    val model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .build()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(PACK_CARD_ASPECT_RATIO)
            .clip(RoundedCornerShape(20.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .border(
                width = 1.dp,
                color = TangemTheme.colors3.border.primary,
                shape = RoundedCornerShape(20.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = GLOW_BLUR_RADIUS, edgeTreatment = BlurredEdgeTreatment.Unbounded),
            model = model,
            contentScale = ContentScale.Fit,
            contentDescription = null,
        )
        SubcomposeAsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = model,
            error = { PackImagePlaceholder() },
            contentScale = ContentScale.Fit,
            contentDescription = null,
        )
    }
}

@Composable
private fun PackImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        TangemIcon(
            tangemIconUM = TangemIconUM.Icon(
                imageVector = Icons.ic_error_20_filled,
                tintReference = { TangemTheme.colors3.icon.tertiary },
            ),
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(TangemTheme.colors3.bg.opaque.primary)
                .padding(10.dp)
                .size(20.dp),
        )
    }
}

@Preview(widthDp = 180, showBackground = true)
@Preview(widthDp = 180, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesPackCardPreview() {
    TangemThemePreviewRedesign {
        CollectiblesPackCard(state = CollectiblesDiscoveryPreviewData.pack)
    }
}