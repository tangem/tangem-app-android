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
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPicker
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_down_16
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.MarketChart
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.ui.components.ForYouPortfolioTokenList
import com.tangem.features.foryou.impl.ui.preview.ForYouPortfolioReviewPreviewData
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ForYouPortfolioReview(
    periodPickerUM: TangemSegmentedPickerUM,
    onPeriodClick: (TangemSegmentUM) -> Unit,
    portfolioReviewUM: PortfolioReviewUM,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResourceSafe(R.string.for_you_portfolio_review_title),
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

        MarketChart(
            marketChart = portfolioReviewUM.marketChartUM,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH(8.dp)

        when (portfolioReviewUM) {
            is PortfolioReviewUM.Content -> {
                TangemSegmentedPicker(
                    tangemSegmentedPickerUM = periodPickerUM,
                    onClick = onPeriodClick,
                )
            }
            is PortfolioReviewUM.Loading -> TangemShimmer(
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
            periodPickerUM = TangemSegmentedPickerUM(
                items = persistentListOf(
                    TangemSegmentUM(id = "0", title = stringReference("Day")),
                    TangemSegmentUM(id = "1", title = stringReference("Week")),
                    TangemSegmentUM(id = "2", title = stringReference("Month")),
                ),
                initialSelectedItem = TangemSegmentUM(id = "0", title = stringReference("Day")),
                isFixed = true,
                isAltSurface = true,
            ),
            onPeriodClick = {},
        )
    }
}

private class ForYouPortfolioReviewPreviewProvider : PreviewParameterProvider<PortfolioReviewUM> {
    override val values: Sequence<PortfolioReviewUM>
        get() = sequenceOf(
            ForYouPortfolioReviewPreviewData.reviewContent,
            PortfolioReviewUM.Loading(
                marketChartUM = MarketChartUM.NoData,
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