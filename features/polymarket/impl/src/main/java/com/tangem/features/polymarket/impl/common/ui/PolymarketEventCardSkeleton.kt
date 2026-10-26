package com.tangem.features.polymarket.impl.common.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

internal val PolymarketEventCardSkeletonHeight = 158.dp

@Composable
internal fun PolymarketEventCardSkeleton(modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors3.bg.secondary,
        shape = PolymarketCardShape,
    ) {
        Column {
            PolymarketCardHeaderSkeleton()
            EventRowSkeleton()
        }
    }
}

@Composable
internal fun PolymarketCardHeaderSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemShimmer(modifier = Modifier.size(width = 232.dp, height = 16.dp), radius = 8.dp)
            TangemShimmer(modifier = Modifier.size(width = 139.dp, height = 16.dp), radius = 8.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                TangemShimmer(modifier = Modifier.size(width = 75.dp, height = 12.dp))
                TangemShimmer(modifier = Modifier.size(width = 67.dp, height = 12.dp))
            }
        }
        TangemShimmer(modifier = Modifier.size(PolymarketCardIconSize), radius = PolymarketCardIconSize / 2)
    }
}

@Composable
private fun EventRowSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TangemShimmer(modifier = Modifier.size(width = 169.dp, height = 16.dp), radius = 8.dp)
        TangemShimmer(modifier = Modifier.size(width = 101.dp, height = 12.dp))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Narrow", showBackground = true, widthDp = 320)
@Composable
private fun PolymarketEventCardSkeletonPreview() {
    TangemThemePreviewRedesign {
        PolymarketEventCardSkeleton(modifier = Modifier.padding(16.dp))
    }
}