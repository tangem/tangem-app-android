package com.tangem.features.foryou.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.model.ForYouNotification
import com.tangem.features.foryou.impl.ui.preview.ForYouEarnOpportunitiesPreviewData
import com.tangem.features.foryou.impl.ui.preview.ForYouPortfolioReviewPreviewData
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun ForYouContent(
    forYouUM: ForYouUM,
    bottomSheetState: State<BottomSheetState>,
    promoBannersBlockComponent: PromoBannersBlockComponent,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(bottomSheetState, promoBannersBlockComponent) {
        snapshotFlow { bottomSheetState.value == BottomSheetState.EXPANDED }
            .distinctUntilChanged()
            .collect(promoBannersBlockComponent::setVisibleOnScreen)
    }
    val background = LocalMainBottomSheetColor.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding())
            .drawBehind { drawRect(background.value) },
    ) {
        promoBannersBlockComponent.ContentWithPadding(
            modifier = Modifier
                .padding(top = 12.dp)
                .conditional(forYouUM.notifications.isEmpty()) {
                    padding(bottom = 48.dp)
                },
            walletId = null,
            horizontalItemPadding = 16.dp,
        )

        forYouUM.notifications.fastForEachIndexed { index, notification ->
            key(notification.state) {
                TangemMessageBanner(
                    state = notification.state,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .conditional(index == 0) {
                            padding(top = 12.dp)
                        }
                        .conditional(index == forYouUM.notifications.lastIndex) {
                            padding(bottom = 48.dp)
                        },
                )
            }
        }

        ForYouPortfolioReview(
            portfolioReviewUM = forYouUM.portfolioReviewUM,
            periodPickerUM = forYouUM.periodPickerUM,
            onPeriodClick = forYouUM.onPeriodClick,
            portfolioFilter = forYouUM.portfolioFilter,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SpacerH(48.dp)

        ForYouEarnOpportunities(
            earnOpportunitiesUM = forYouUM.earnOpportunities,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SpacerH(48.dp)
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ForYouContent_Preview(@PreviewParameter(ForYouContentPreviewProvider::class) params: ForYouUM) {
    TangemThemePreviewRedesign {
        ForYouContent(
            forYouUM = params,
            bottomSheetState = remember { mutableStateOf(BottomSheetState.EXPANDED) },
            promoBannersBlockComponent = object : PromoBannersBlockComponent {
                @Composable
                override fun ContentWithPadding(horizontalItemPadding: Dp, walletId: String?, modifier: Modifier) {
                }

                override fun setVisibleOnScreen(isVisible: Boolean) {}
            },
            contentPadding = PaddingValues.Zero,
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}

private class ForYouContentPreviewProvider : PreviewParameterProvider<ForYouUM> {
    override val values: Sequence<ForYouUM>
        get() = sequenceOf(
            ForYouUM(
                notifications = persistentListOf(ForYouNotification.UsedOutdatedData),
                earnOpportunities = ForYouEarnOpportunitiesPreviewData.tokensRewards,
                portfolioReviewUM = ForYouPortfolioReviewPreviewData.reviewContent,
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
                // Active state on purpose: ForYouPortfolioReview's own preview covers the inactive chip.
                portfolioFilter = TangemFilterItemUM.Active(
                    id = "portfolio_selector",
                    value = stringReference("Accounts"),
                    counter = 3,
                    onClick = {},
                    onClearClick = {},
                ),
            ),
        )
}
// endregion