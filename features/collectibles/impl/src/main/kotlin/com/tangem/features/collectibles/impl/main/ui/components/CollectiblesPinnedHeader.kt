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
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.features.collectibles.impl.discovery.ui.CollectiblesTabBand
import com.tangem.features.collectibles.impl.discovery.ui.CollectiblesTabBandSkeleton
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM

@Composable
internal fun CollectiblesPinnedHeader(
    tabs: CollectiblesDiscoveryUM.TabsUM,
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
        // A haze source above the list, so the top bar's material buttons sample the frost and not only
        // the content under it; the nested fade still blurs only the list (lower zIndex).
        Box(
            modifier = Modifier
                .height(frostHeight)
                .hazeSourceTangem(zIndex = 1f),
        ) {
            TangemFade(
                position = TangemFade.Position.Top,
                variant = TangemFade.Variant.Hard,
                blur = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
        val bandModifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .alpha(if (isPinned) 1f else 0f)

        when (tabs) {
            is CollectiblesDiscoveryUM.TabsUM.Content -> CollectiblesTabBand(
                tabs = tabs.tabs,
                scrollState = scrollState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = bandModifier,
            )
            CollectiblesDiscoveryUM.TabsUM.Loading -> CollectiblesTabBandSkeleton(modifier = bandModifier)
            CollectiblesDiscoveryUM.TabsUM.Hidden -> Unit
        }
    }
}