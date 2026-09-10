package com.tangem.features.collectibles.impl.main.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.features.collectibles.impl.discovery.ui.CollectiblesTabBand
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun CollectiblesPinnedHeader(
    tabs: ImmutableList<TangemTabItemUM>,
    scrollState: ScrollState,
    topPadding: Dp,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
) {
    val frostHeight by animateDpAsState(
        targetValue = if (isPinned) topPadding + 52.dp else topPadding,
        label = "headerFrostHeight",
    )
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        TangemFade(
            position = TangemFade.Position.Top,
            blur = true,
            modifier = Modifier.height(frostHeight),
        )
        CollectiblesTabBand(
            tabs = tabs,
            scrollState = scrollState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .then(
                    if (isPinned) {
                        Modifier.alpha(1f)
                    } else {
                        Modifier.alpha(0f)
                    },
                ),
        )
    }
}