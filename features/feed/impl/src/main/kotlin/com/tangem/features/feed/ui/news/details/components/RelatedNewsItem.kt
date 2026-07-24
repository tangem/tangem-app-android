package com.tangem.features.feed.ui.news.details.components

import androidx.compose.foundation.background
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
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.news.details.state.RelatedArticleUM

@Composable
internal fun RelatedNewsItem(relatedArticle: RelatedArticleUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .sizeIn(maxWidth = 280.dp, minHeight = 164.dp)
            .background(
                color = TangemTheme.colors3.bg.secondary,
                shape = RoundedCornerShape(24.dp),
            )
            .clickable(onClick = relatedArticle.onClick)
            .padding(16.dp),
    ) {
        RelatedNewsItemBody(relatedArticle = relatedArticle)
        SpacerHMax()
        RelatedNewsItemPublishedAt(publishedAt = relatedArticle.publishedAt.resolveReference())
    }
}

@Composable
private fun RelatedNewsItemBody(relatedArticle: RelatedArticleUM) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            RelatedNewsItemSource(name = relatedArticle.media.name)
            if (relatedArticle.title.isNotEmpty()) {
                SpacerH(8.dp)
                RelatedNewsItemTitle(title = relatedArticle.title)
            }
        }
        relatedArticle.imageUrl?.let { imageUrl ->
            RelatedNewsItemImage(
                imageUrl = imageUrl,
                contentDescription = relatedArticle.media.name,
            )
        }
    }
}

@Composable
private fun RelatedNewsItemSource(name: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.ic_explore_16),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.secondary,
            modifier = Modifier.size(16.dp),
        )
        SpacerW(2.dp)
        Text(
            text = name,
            style = TangemTheme.typography3.caption.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun RelatedNewsItemTitle(title: String) {
    Text(
        text = title,
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.primary,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RelatedNewsItemImage(imageUrl: String, contentDescription: String) {
    SubcomposeAsyncImage(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop,
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(imageUrl)
            .crossfade(enable = false)
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build(),
        loading = {
            TangemShimmer(
                modifier = Modifier.fillMaxSize(),
                radius = 12.dp,
            )
        },
        error = {},
        contentDescription = contentDescription,
    )
}

@Composable
private fun RelatedNewsItemPublishedAt(publishedAt: String) {
    Text(
        text = publishedAt,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}