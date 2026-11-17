package com.tangem.features.collectibles.impl.inventory.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.inventory.ui.preview.CollectiblesInventoryPreviewData
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM.ItemUM

@Composable
internal fun CollectiblesInventorySkeleton(
    info: ItemUM.Info?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ProvideTangemShimmer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding() + 12.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (info != null) {
                CollectiblesInventoryInfoBanner(state = info, modifier = Modifier.padding(vertical = 4.dp))
            }
            repeat(times = 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardSkeleton(modifier = Modifier.weight(1f))
                    CardSkeleton(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TangemShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = INVENTORY_CARD_ASPECT_RATIO),
            radius = 20.dp,
        )
        SpacerH(8.dp)
        TangemShimmer(style = TangemTheme.typography3.subheading.medium)
        SpacerH(2.dp)
        TangemShimmer(style = TangemTheme.typography3.subheading.medium)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesInventorySkeletonPreview() {
    TangemThemePreviewRedesign {
        CollectiblesInventorySkeleton(
            info = CollectiblesInventoryPreviewData.info,
            contentPadding = PaddingValues(),
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}