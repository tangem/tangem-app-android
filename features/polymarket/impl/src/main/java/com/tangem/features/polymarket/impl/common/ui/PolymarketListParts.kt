package com.tangem.features.polymarket.impl.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import androidx.compose.ui.unit.dp

/** How many items before the end of a paginated list the next page starts loading. */
private const val LOAD_MORE_THRESHOLD = 5

/** Full-area loader of a list that has nothing to show yet. */
@Composable
internal fun PolymarketLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        contentAlignment = Alignment.Center,
    ) {
        TangemLoader(
            color = TangemTheme.colors3.icon.primary,
            size = TangemLoaderSize.X32,
        )
    }
}

/** Footer loader shown while the next page of a paginated list is on its way. */
@Composable
internal fun PolymarketNextPageLoader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        TangemLoader(
            color = TangemTheme.colors3.icon.primary,
            size = TangemLoaderSize.X24,
        )
    }
}

/** A list that could not be served: [text] under a refresh mark, the whole area tappable to retry. */
@Composable
internal fun PolymarketReloadPrompt(text: TextReference, onReloadClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onReloadClick)
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.inverse),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.ic_arrow_refresh_20),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.inverse,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = text.resolveReference(),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.subheading.medium,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Asks for the next page once the list is scrolled within [LOAD_MORE_THRESHOLD] items of its end. The model
 * decides whether there is anything left to load, so this only has to fire on approach.
 */
@Composable
internal fun PolymarketLoadMoreEffect(listState: LazyListState, onLoadMore: () -> Unit) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) currentOnLoadMore()
    }
}