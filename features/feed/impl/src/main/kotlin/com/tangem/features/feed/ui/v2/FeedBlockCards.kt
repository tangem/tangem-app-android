package com.tangem.features.feed.ui.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.res.TangemTheme

private val FeedBlockShape = RoundedCornerShape(size = 20.dp)

/**
 * Half-height shortcut card: icon at the top start, title at the bottom start.
 *
 * @param modifier must carry the card's size — the row decides it.
 */
@Composable
internal fun FeedShortcutCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    TangemSurface(
        modifier = modifier,
        color = TangemTheme.colors3.bg.opaque.primary,
        shape = FeedBlockShape,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            icon()
            Text(
                text = title,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 1,
            )
        }
    }
}

/**
 * A card with nothing but its title, for a block whose pitch hasn't been designed yet.
 *
 * @param modifier must carry the card's size — the row decides it.
 */
@Composable
internal fun FeedTitleCard(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier,
        color = TangemTheme.colors3.bg.opaque.primary,
        shape = FeedBlockShape,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.padding(all = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }
    }
}