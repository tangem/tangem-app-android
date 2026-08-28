package com.tangem.features.collectibles.impl.main.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_card_24
import com.tangem.features.collectibles.impl.main.ui.preview.CollectiblesMainPreviewData
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM

/** Aspect ratio of the pack artwork, taken from the 179x228 cell in the design. */
@Suppress("TopLevelPropertyNaming")
internal const val PackImageAspectRatio = 179f / 228f

private val PackImageShape = RoundedCornerShape(20.dp)

@Composable
internal fun CollectiblesPackCard(state: CollectiblesMainUM.PackUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickableSingle(onClick = state.onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PackImage(imageUrl = state.imageUrl)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.price.resolveReference(),
                    style = TangemTheme.typography3.subheading.medium,
                    color = TangemTheme.colors3.text.primary,
                    maxLines = 1,
                )
                state.badge?.let { badge ->
                    TangemBadge(text = badge, size = TangemBadge.Size.X4)
                }
            }
        }
    }
}

@Composable
private fun PackImage(imageUrl: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(PackImageAspectRatio)
            .clip(PackImageShape)
            .background(TangemTheme.colors3.bg.secondary),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 7.dp),
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            loading = { PackImagePlaceholder() },
            error = { PackImagePlaceholder() },
            contentScale = ContentScale.Fit,
            contentDescription = null,
        )
    }
}

@Composable
private fun PackImagePlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TangemIcon(
            tangemIconUM = TangemIconUM.Icon(
                imageVector = Icons.ic_card_24,
                tintReference = { TangemTheme.colors3.icon.tertiary },
            ),
            modifier = Modifier.size(40.dp),
        )
    }
}

@Preview(widthDp = 179, showBackground = true)
@Preview(widthDp = 179, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesPackCardPreview() {
    TangemThemePreviewRedesign {
        CollectiblesPackCard(state = CollectiblesMainPreviewData.pack)
    }
}