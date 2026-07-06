package com.tangem.features.foryou.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPicker
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_down_16
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.ui.components.ForYouMarketChartContent
import com.tangem.features.foryou.impl.ui.components.ForYouPortfolioTokenList
import com.tangem.features.foryou.impl.ui.preview.ForYouPortfolioReviewPreviewData
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ForYouPortfolioReview(portfolioReviewUM: PortfolioReviewUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Portfolio Review", // TODO For You
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
            )
            TangemBadge(
                text = stringReference("All accounts"), // TODO For You
                variant = TangemBadge.Variant.Solid,
                size = TangemBadge.Size.X9,
                iconEnd = TangemIconUM.Icon(Icons.ic_chevron_down_16),
            )
        }
        SpacerH(16.dp)
        ForYouMarketChartContent(portfolioReviewUM)
        SpacerH(8.dp)
        when (portfolioReviewUM) {
            is PortfolioReviewUM.Content -> {
                TangemSegmentedPicker(
                    tangemSegmentedPickerUM = portfolioReviewUM.periodPickerUM,
                    onClick = portfolioReviewUM.onPeriodClick,
                )
            }
            is PortfolioReviewUM.Loading -> RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                radius = 100.dp,
            )
        }

        ForYouPortfolioTokenList(tokenList = portfolioReviewUM.tokenList)
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ForYouPortfolioReview_Review(
    @PreviewParameter(ForYouPortfolioReviewPreviewProvider::class) params: PortfolioReviewUM,
) {
    TangemThemePreviewRedesign {
        ForYouPortfolioReview(
            portfolioReviewUM = params,
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}

private class ForYouPortfolioReviewPreviewProvider : PreviewParameterProvider<PortfolioReviewUM> {
    override val values: Sequence<PortfolioReviewUM>
        get() = sequenceOf(
            ForYouPortfolioReviewPreviewData.reviewContent,
            PortfolioReviewUM.Loading(
                tokenList = buildList {
                    repeat(4) { index ->
                        add(
                            ForYouTokenListItemUM(
                                tokenRowUM = TangemTokenRowUM.Loading(
                                    id = index.toString(),
                                ),
                                tokenList = persistentListOf(),
                                isExpanded = false,
                                isExpandable = false,
                            ),
                        )
                    }
                }.toPersistentList(),
            ),
        )
}
// endregion