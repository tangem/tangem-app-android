package com.tangem.features.feed.ui.news.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.news.details.state.RelatedArticleUM

@Composable
internal fun RelatedNewsItem(relatedArticle: RelatedArticleUM, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        RelatedNewsItemV2(relatedArticle, modifier)
    } else {
        RelatedNewsItemV1(relatedArticle, modifier)
    }
}

@Composable
private fun RelatedNewsItemV1(relatedArticle: RelatedArticleUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .sizeIn(maxWidth = 256.dp, minHeight = 132.dp)
            .background(color = TangemTheme.colors.background.action, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = relatedArticle.onClick)
            .padding(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_explore_16),
                        contentDescription = null,
                        tint = TangemTheme.colors.icon.informative,
                        modifier = Modifier.size(16.dp),
                    )
                    SpacerW(4.dp)
                    Text(
                        text = relatedArticle.media.name,
                        style = TangemTheme.typography.caption1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
                if (relatedArticle.title.isNotEmpty()) {
                    SpacerH(4.dp)
                    Text(
                        text = relatedArticle.title,
                        style = TangemTheme.typography.subtitle2,
                        color = TangemTheme.colors.text.primary1,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (relatedArticle.imageUrl != null) {
                SubcomposeAsyncImage(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                    model = ImageRequest.Builder(context = LocalContext.current)
                        .data(relatedArticle.imageUrl)
                        .crossfade(enable = false)
                        .allowHardware(true)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    loading = {
                        RectangleShimmer(
                            modifier = Modifier.size(40.dp),
                            radius = 4.dp,
                        )
                    },
                    error = {},
                    contentDescription = relatedArticle.media.name,
                )
            }
        }
        SpacerHMax()
        Text(
            text = relatedArticle.publishedAt.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun RelatedNewsItemV2(relatedArticle: RelatedArticleUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .sizeIn(maxWidth = 228.dp, minHeight = 160.dp)
            .background(
                color = TangemTheme.colors2.surface.level3,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            )
            .clickable(onClick = relatedArticle.onClick)
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.primary,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            )
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_explore_16),
                        contentDescription = null,
                        tint = TangemTheme.colors2.markers.iconGray,
                        modifier = Modifier.size(16.dp),
                    )
                    SpacerW(2.dp)
                    Text(
                        text = relatedArticle.media.name,
                        style = TangemTheme.typography2.captionSemibold12,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TangemTheme.colors2.text.neutral.secondary,
                    )
                }
                if (relatedArticle.title.isNotEmpty()) {
                    SpacerH(8.dp)
                    Text(
                        text = relatedArticle.title,
                        style = TangemTheme.typography2.bodyRegular16,
                        color = TangemTheme.colors2.text.neutral.primary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (relatedArticle.imageUrl != null) {
                SubcomposeAsyncImage(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    model = ImageRequest.Builder(context = LocalContext.current)
                        .data(relatedArticle.imageUrl)
                        .crossfade(enable = false)
                        .allowHardware(true)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    loading = {
                        RectangleShimmer(
                            modifier = Modifier.size(44.dp),
                            radius = 12.dp,
                        )
                    },
                    error = {},
                    contentDescription = relatedArticle.media.name,
                )
            }
        }
        SpacerHMax()
        Text(
            text = relatedArticle.publishedAt.resolveReference(),
            style = TangemTheme.typography2.captionSemibold12,
            color = TangemTheme.colors2.text.neutral.secondary,
        )
    }
}