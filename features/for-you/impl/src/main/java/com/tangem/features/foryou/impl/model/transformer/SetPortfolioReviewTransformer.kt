package com.tangem.features.foryou.impl.model.transformer

import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.ForYouNotification
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.features.foryou.impl.model.converter.portfolioReview.ForYouPortfolioReviewConverter
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

/**
 * Applies one combined emission to the [ForYouUM] state: sets the pre-built portfolio-review and
 * earn-opportunities sections (see [ForYouPortfolioReviewConverter] and
 * `ForYouEarnOpportunitiesConverter`), derives the outdated-data notification from the selected
 * portfolio's aggregate total-balance source, and builds the portfolio-selector badge label from the
 * selected accounts against [ForYouSelectedPortfolio.totalAccountsCount].
 *

 * subsequent refreshes the previous picker is carried over so the user's selection is not reset.
 *
 * Modelled on `SetTokenListTransformer` (a transformer that rebuilds the state while delegating the
 * section construction to dedicated converters).
 */
internal class SetPortfolioReviewTransformer(
    private val selectedPortfolio: ForYouSelectedPortfolio,
    private val portfolioReviewUM: PortfolioReviewUM,
    private val earnOpportunitiesUM: EarnOpportunitiesUM,
) : Transformer<ForYouUM> {

    override fun transform(prevState: ForYouUM): ForYouUM {
        val loadedBalance = selectedPortfolio.totalFiatBalance as? TotalFiatBalance.Loaded
        return prevState.copy(
            notifications = if (loadedBalance?.source == StatusSource.ONLY_CACHE) {
                persistentListOf(ForYouNotification.UsedOutdatedData)
            } else {
                persistentListOf()
            },
            earnOpportunities = earnOpportunitiesUM,
            portfolioReviewUM = portfolioReviewUM,
            portfolioSelectorLabel = buildPortfolioSelectorLabel(),
            periodPickerUM = when (prevState.portfolioReviewUM) {
                is PortfolioReviewUM.Loading -> createPeriodPicker()
                is PortfolioReviewUM.Content -> prevState.periodPickerUM
            },
        )
    }

    /**
     * The portfolio-selector badge label: "All accounts" when everything (or nothing specific) is
     * selected, the account name for a single selection, otherwise the selected count.
     */
    private fun buildPortfolioSelectorLabel(): TextReference {
        val selectedAccounts = selectedPortfolio.accountCryptoCurrencyStatuses.map { it.account }.distinct()
        return when {
            selectedAccounts.isEmpty() || selectedAccounts.size == selectedPortfolio.totalAccountsCount ->
                stringReference("All accounts")
            selectedAccounts.size == 1 -> selectedAccounts.first().accountName.toUM().value
            else -> stringReference("${selectedAccounts.size} accounts")
        }
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