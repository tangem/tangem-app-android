package com.tangem.features.collectibles.impl.discovery.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/** Placeholder band, laid out to the height [CollectiblesTabBand] takes, shown until the categories arrive. */
@Composable
internal fun CollectiblesTabBandSkeleton(modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        Row(
            modifier = modifier
                .height(BAND_HEIGHT)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TAB_WIDTHS.forEach { width ->
                TangemShimmer(
                    modifier = Modifier.size(width = width, height = 36.dp),
                    radius = 18.dp,
                )
            }
        }
    }
}

/** Placeholder pack grid, shown until the first page arrives. */
@Composable
internal fun CollectiblesPackGridSkeleton(modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        Column(modifier = modifier.fillMaxWidth()) {
            repeat(ROW_COUNT) {
                PackRowSkeleton()
            }
        }
    }
}

@Composable
private fun PackRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(PACKS_PER_ROW) {
            PackCardSkeleton(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PackCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TangemShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PACK_CARD_ASPECT_RATIO),
            radius = 20.dp,
        )
        SpacerH(8.dp)
        TangemShimmer(style = TangemTheme.typography3.subheading.medium)
        SpacerH(2.dp)
        TangemShimmer(style = TangemTheme.typography3.subheading.medium)
    }
}

private const val PACKS_PER_ROW = 2
private const val ROW_COUNT = 2
private val BAND_HEIGHT: Dp = 52.dp
private val TAB_WIDTHS: List<Dp> = listOf(64.dp, 80.dp)

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesDiscoverySkeletonPreview() {
    TangemThemePreviewRedesign {
        Column {
            CollectiblesTabBandSkeleton()
            SpacerH(12.dp)
            CollectiblesPackGridSkeleton()
        }
    }
}