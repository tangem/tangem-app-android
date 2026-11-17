package com.tangem.features.polymarket.impl.details.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

internal const val EVENT_DETAILS_HEADER_KEY = "header"

private val ContentTopGap = 8.dp
private val CollapsedBarHeight = 56.dp
private val PinnedButtonsClearance = 72.dp
private val PinnedButtonsTopOffset = 6.dp

/** The scrolling header must use [EVENT_DETAILS_HEADER_KEY] and the supplied top content padding. */
@Suppress("LongParameterList")
@Composable
internal fun PolymarketEventDetailsScaffold(
    listState: LazyListState,
    title: TextReference?,
    categoryLabel: TextReference?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Lazy item offsets exclude the top content padding, so the toolbar edge is at -ContentTopGap.
    val collapseLinePx = with(LocalDensity.current) { -ContentTopGap.roundToPx() }
    val isBarCollapsed by remember(listState, collapseLinePx) {
        derivedStateOf {
            val items = listState.layoutInfo.visibleItemsInfo
            val header = items.firstOrNull { it.key == EVENT_DETAILS_HEADER_KEY }
            header?.let { it.offset + it.size <= collapseLinePx } ?: items.isNotEmpty()
        }
    }
    val frostHeight by animateDpAsState(
        targetValue = if (isBarCollapsed && title != null) statusBarPadding + CollapsedBarHeight else 0.dp,
        label = "detailsFrostHeight",
    )

    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {},
        overlay = {
            TangemFade(
                position = TangemFade.Position.Top,
                blur = true,
                modifier = Modifier.height(frostHeight),
            )
            if (title != null) {
                AnimatedVisibility(visible = isBarCollapsed, enter = fadeIn(), exit = fadeOut()) {
                    CollapsedBar(
                        title = title,
                        categoryLabel = categoryLabel,
                        icon = icon,
                        modifier = Modifier.padding(top = statusBarPadding),
                    )
                }
            }
            TangemButton.Back(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = statusBarPadding + PinnedButtonsTopOffset, start = 16.dp),
                onClick = onBackClick,
            )
        },
    ) { padding ->
        content(
            PaddingValues(
                top = statusBarPadding + CollapsedBarHeight + ContentTopGap,
                bottom = padding.calculateBottomPadding(),
            ),
        )
    }
}

@Composable
private fun CollapsedBar(
    title: TextReference,
    categoryLabel: TextReference?,
    icon: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CollapsedBarHeight)
            .padding(horizontal = PinnedButtonsClearance),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title.resolveReference(),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.body.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            categoryLabel?.let { TangemBadge(text = it, size = TangemBadge.Size.X4) }
        }
    }
}