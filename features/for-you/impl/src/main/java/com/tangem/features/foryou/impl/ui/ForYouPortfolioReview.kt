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
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.filter.TangemFilterItem
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.MarketChart
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.entity.forYouPeriodPickerItems
import com.tangem.features.foryou.impl.ui.components.ForYouPortfolioTokenList
import com.tangem.features.foryou.impl.ui.preview.ForYouPortfolioReviewPreviewData
import com.tangem.features.foryou.model.ForYouPeriod
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ForYouPortfolioReview(
    periodPickerUM: ImmutableList<TangemSegmentedControl.Item>,
    portfolioReviewUM: PortfolioReviewUM,
    portfolioFilter: TangemFilterItemUM,
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
            TangemFilterItem(
                state = portfolioFilter,
                variant = TangemFilterItem.Variant.Transparent,
            )
        }
        SpacerH(16.dp)

        MarketChart(
            marketChart = portfolioReviewUM.marketChartUM,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH(8.dp)

        when (portfolioReviewUM) {
            is PortfolioReviewUM.Content -> TangemSegmentedControl(
                items = periodPickerUM,
                modifier = Modifier.fillMaxWidth(),
            )
            is PortfolioReviewUM.Loading -> TangemSegmentedControl(
                state = TangemSegmentedControl.State.Loading,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SpacerH8()
        ForYouPortfolioTokenList(items = portfolioReviewUM.tokenList)

        if (portfolioReviewUM is PortfolioReviewUM.Content && portfolioReviewUM.onAddFundsClick != null) {
            TangemButton(
                text = resourceReference(R.string.common_add_funds),
                onClick = portfolioReviewUM.onAddFundsClick,
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
            portfolioFilter = TangemFilterItemUM.Inactive(
                id = "portfolio_selector",
                label = stringReference("All accounts"),
                onClick = {},
            ),
            periodPickerUM = forYouPeriodPickerItems(selected = ForYouPeriod.Day, onClick = {}),
        )
    }
}

private class ForYouPortfolioReviewPreviewProvider : PreviewParameterProvider<PortfolioReviewUM> {
    override val values: Sequence<PortfolioReviewUM>
        get() = sequenceOf(
            ForYouPortfolioReviewPreviewData.reviewContent,
            ForYouPortfolioReviewPreviewData.loadingState,
            ForYouPortfolioReviewPreviewData.zeroPortfolioState,
        )
}
// endregion