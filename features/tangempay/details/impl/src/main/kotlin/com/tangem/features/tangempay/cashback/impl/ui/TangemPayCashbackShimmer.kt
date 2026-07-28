package com.tangem.features.tangempay.cashback.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun TangemPayCashbackShimmer(modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        Column(modifier = modifier.fillMaxWidth()) {
            HeroShimmer()
            InfoTilesShimmer(modifier = Modifier.padding(top = 24.dp))
            HistogramShimmer(modifier = Modifier.padding(top = 24.dp))
            AdditionalCashbackShimmer(modifier = Modifier.padding(top = 24.dp))
        }
    }
}

@Composable
private fun HeroShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemShimmer(style = TangemTheme.typography3.heading.medium, textAlign = TextAlign.Center)
        TangemShimmer(style = TangemTheme.typography3.subheading.medium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InfoTilesShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TangemShimmer(modifier = Modifier.weight(1f).height(108.dp), radius = 16.dp)
        TangemShimmer(modifier = Modifier.weight(1f).height(108.dp), radius = 16.dp)
    }
}

@Composable
private fun HistogramShimmer(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeaderShimmer()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            BAR_HEIGHTS.forEach { barHeight ->
                TangemShimmer(modifier = Modifier.weight(1f).height(barHeight), radius = 8.dp)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TangemTheme.colors3.border.primary),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            repeat(BAR_HEIGHTS.size) {
                TangemShimmer(
                    style = TangemTheme.typography3.caption.medium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AdditionalCashbackShimmer(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeaderShimmer()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(times = 3) {
                TangemShimmer(modifier = Modifier.fillMaxWidth().height(108.dp), radius = 24.dp)
            }
        }
    }
}

@Composable
private fun SectionHeaderShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(16.dp),
    ) {
        TangemShimmer(style = TangemTheme.typography3.heading.small)
    }
}

private val BAR_HEIGHTS: List<Dp> = listOf(38.dp, 115.dp, 102.dp, 59.dp, 77.dp)

@Preview(showBackground = true, widthDp = 402)
@Preview(showBackground = true, widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCashbackShimmerPreview() {
    TangemThemePreviewRedesign {
        TangemPayCashbackShimmer(modifier = Modifier.background(TangemTheme.colors3.bg.primary))
    }
}