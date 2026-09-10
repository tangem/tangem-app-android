package com.tangem.features.collectibles.impl.discovery.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import kotlinx.collections.immutable.ImmutableList

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
        modifier = modifier.height(52.dp),
    )
}