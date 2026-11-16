package com.tangem.features.polymarket.impl.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme

internal val PolymarketCardIconSize = 40.dp

/**
 * The chrome every Polymarket card shares: a surface with a title, a round icon on the right, an optional
 * [meta] line under the title and an arbitrary [content] block below the header. The card itself carries no
 * feature knowledge — the feed, the search results and the position cards differ only in what they slot in.
 */
@Suppress("LongParameterList")
@Composable
internal fun PolymarketEventCardFrame(
    title: TextReference,
    iconUrl: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    titleMetaSpacing: Dp = 4.dp,
    showIcon: Boolean = true,
    meta: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TangemSurface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors3.bg.secondary,
        shape = PolymarketCardShape,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            TangemRow(
                modifier = Modifier.fillMaxWidth(),
                includeInnerPaddings = false,
                titleSlot = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(titleMetaSpacing),
                    ) {
                        TangemRowText(text = title, role = TangemRowTextRole.Title, maxLines = 2)
                        meta?.invoke()
                    }
                },
                endSlot = if (showIcon) {
                    { PolymarketCardIcon(iconUrl = iconUrl) }
                } else {
                    null
                },
            )

            content?.invoke(this)
        }
    }
}

@Composable
internal fun PolymarketCardIcon(iconUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(PolymarketCardIconSize)
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