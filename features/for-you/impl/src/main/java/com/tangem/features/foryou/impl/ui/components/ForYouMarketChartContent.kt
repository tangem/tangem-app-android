package com.tangem.features.foryou.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM

@Composable
internal fun ForYouMarketChartContent(portfolioReviewUM: PortfolioReviewUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    top = 16.dp,
                    end = 32.dp,
                    start = 32.dp,
                    bottom = 32.dp,
                )
                .background(Cyan, CircleShape)
                .size(200.dp),
        )
        SpacerH(16.dp)

        when (portfolioReviewUM) {
            is PortfolioReviewUM.Content -> {
                Text(
                    text = portfolioReviewUM.assetCount.resolveReference(),
                    style = TangemTheme.typography3.heading.small,
                    color = TangemTheme.colors3.text.secondary,
                )
                Text(
                    text = portfolioReviewUM.topHoldingPercent.resolveReference(),
                    style = TangemTheme.typography3.heading.small,
                    color = TangemTheme.colors3.text.primary,
                )
            }
            is PortfolioReviewUM.Loading -> {
                TextShimmer(
                    style = TangemTheme.typography3.heading.small,
                    modifier = Modifier.width(50.dp),
                )
                TextShimmer(
                    style = TangemTheme.typography3.heading.small,
                    modifier = Modifier.width(75.dp),
                )
            }
        }
    }
}