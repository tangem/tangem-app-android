package com.tangem.features.collectibles.impl.main.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import kotlinx.collections.immutable.ImmutableList

/** 36dp pills plus 8dp vertical padding, matching the tab band in the design. */
internal val TabBandHeight = 52.dp

/** Keeps 20dp between the tab pills and the first pack row, pinned or not. */
internal val TabBandBottomGap = 12.dp

@Composable
internal fun CollectiblesTabBand(
    tabs: ImmutableList<TangemTabItemUM>,
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    TangemTabNavigation(
        tabs = tabs,
        scrollState = scrollState,
        contentPadding = contentPadding,
        modifier = modifier.height(TabBandHeight),
    )
}