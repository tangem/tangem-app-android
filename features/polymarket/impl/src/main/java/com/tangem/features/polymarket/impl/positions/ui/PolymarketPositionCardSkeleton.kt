package com.tangem.features.polymarket.impl.positions.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardIconSize
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape
import com.tangem.features.polymarket.impl.positions.ui.preview.PolymarketPositionCardPreviewProvider

@Composable
internal fun PolymarketPositionCardSkeleton(modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors3.bg.secondary,
        shape = PolymarketCardShape,
    ) {
        // Matches the content card at the default font scale, including its outcome bubble.
        Box(modifier = Modifier.height(112.dp).padding(horizontal = 16.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TangemShimmer(modifier = Modifier.size(width = 238.dp, height = 16.dp), radius = 8.dp)
                    TangemShimmer(modifier = Modifier.size(width = 143.dp, height = 16.dp), radius = 8.dp)
                }
                TangemShimmer(modifier = Modifier.size(PolymarketCardIconSize), radius = PolymarketCardIconSize / 2)
            }
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TangemShimmer(modifier = Modifier.size(width = 134.dp, height = 14.dp), radius = 7.dp)
                TangemShimmer(modifier = Modifier.size(width = 67.dp, height = 14.dp), radius = 7.dp)
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Narrow", showBackground = true, widthDp = 320)
@Composable
private fun PolymarketPositionCardSkeletonPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PolymarketPositionCardSkeleton()
            PolymarketPositionCard(state = PolymarketPositionCardPreviewProvider().values.first().state)
        }
    }
}