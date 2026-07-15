package com.tangem.features.foryou.impl.model.transformer

import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.ForYouNotification
import com.tangem.features.foryou.impl.model.converter.portfolioReview.ForYouPortfolioReviewConverter
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

/**
 * Applies one combined emission to the [ForYouUM] state: sets the pre-built portfolio-review and
 * earn-opportunities sections (see [ForYouPortfolioReviewConverter] and
 * `ForYouEarnOpportunitiesConverter`) and derives the outdated-data notification from
 * [accountStatusList]'s total-balance source.
 *

 * subsequent refreshes the previous picker is carried over so the user's selection is not reset.
 *
 * Modelled on `SetTokenListTransformer` (a transformer that rebuilds the state while delegating the
 * section construction to dedicated converters).
 */
internal class SetPortfolioReviewTransformer(
    private val accountStatusList: AccountStatusList?,
    private val portfolioReviewUM: PortfolioReviewUM,
    private val earnOpportunitiesUM: EarnOpportunitiesUM,
) : Transformer<ForYouUM> {

    override fun transform(prevState: ForYouUM): ForYouUM {
        val loadedBalance = accountStatusList?.totalFiatBalance as? TotalFiatBalance.Loaded
        return prevState.copy(
            notifications = if (loadedBalance?.source == StatusSource.ONLY_CACHE) {
                persistentListOf(ForYouNotification.UsedOutdatedData)
            } else {
                persistentListOf()
            },
            earnOpportunities = earnOpportunitiesUM,
            portfolioReviewUM = portfolioReviewUM,
            periodPickerUM = when (prevState.portfolioReviewUM) {
                is PortfolioReviewUM.Loading -> createPeriodPicker()
                is PortfolioReviewUM.Content -> prevState.periodPickerUM
            },
        )
    }

    private fun createPeriodPicker(): TangemSegmentedPickerUM {
        // TODO For you replace with data from backend
        val day = TangemSegmentUM(id = "0", title = stringReference("Day"))
        return TangemSegmentedPickerUM(
            items = persistentListOf(
                day,
                TangemSegmentUM(id = "1", title = stringReference("Week")),
                TangemSegmentUM(id = "2", title = stringReference("Month")),
            ),
            initialSelectedItem = day,
            isFixed = true,
            isAltSurface = true,
        )
    }
}