package com.tangem.features.polymarket.impl.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

private val CardShape = RoundedCornerShape(16.dp)

/**
 * The chrome every Polymarket card shares: a surface with a title, a round icon on the right, an optional
 * [meta] line under the title and an arbitrary [content] block below the header. The card itself carries no
 * feature knowledge — the feed, the search results and the position cards differ only in what they slot in.
 */
@Composable
internal fun PolymarketEventCardFrame(
    title: TextReference,
    iconUrl: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    meta: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TangemSurface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors3.bg.secondary,
        shape = CardShape,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title.resolveReference(),
                        color = TangemTheme.colors3.text.primary,
                        style = TangemTheme.typography3.body.medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    meta?.invoke()
                }

                PolymarketCardIcon(iconUrl = iconUrl)
            }

            content?.invoke(this)
        }
    }
}

@Composable
internal fun PolymarketCardIcon(iconUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.tertiary),
    ) {
        if (iconUrl != null) {
            AsyncImage(
                modifier = Modifier.matchParentSize(),
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    }
}