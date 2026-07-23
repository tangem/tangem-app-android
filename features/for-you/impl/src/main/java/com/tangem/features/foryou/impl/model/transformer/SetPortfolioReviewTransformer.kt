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
import com.tangem.features.foryou.model.ForYouPeriod
import com.tangem.features.foryou.impl.model.converter.portfolioReview.ForYouPortfolioReviewConverter
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Applies one combined emission to the [ForYouUM] state: sets the pre-built portfolio-review and
 * earn-opportunities sections (see [ForYouPortfolioReviewConverter] and
 * `ForYouEarnOpportunitiesConverter`), derives the outdated-data notification from the selected
 * portfolio's aggregate total-balance source, and builds the portfolio-selector badge label from the
 * selected accounts against [ForYouSelectedPortfolio.totalAccountsCount].
 *

 * subsequent refreshes the previous picker is carried over so the user's selection is not reset.
 *
 * The converters build every item collapsed (the sentiment badge is baked in for the selected period by
 * [ForYouPortfolioReviewConverter]); the current expanded-assets selections are re-applied here, read
 * through the [expandedPortfolioReviewAssetIds] / [expandedEarnOpportunitiesAssetIds] providers at
 * transform time — not captured at conversion time — so a data refresh racing an expand click cannot
 * revert the expansion.
 *
 * Modelled on `SetTokenListTransformer` (a transformer that rebuilds the state while delegating the
 * section construction to dedicated converters).
 */
internal class SetPortfolioReviewTransformer(
    private val selectedPortfolio: ForYouSelectedPortfolio,
    private val portfolioReviewUM: PortfolioReviewUM,
    private val earnOpportunitiesUM: EarnOpportunitiesUM,
    private val expandedPortfolioReviewAssetIds: () -> Set<String>,
    private val expandedEarnOpportunitiesAssetIds: () -> Set<String>,
) : Transformer<ForYouUM> {

    override fun transform(prevState: ForYouUM): ForYouUM {
        val loadedBalance = selectedPortfolio.totalFiatBalance as? TotalFiatBalance.Loaded
        return prevState.copy(
            notifications = if (loadedBalance?.source == StatusSource.ONLY_CACHE) {
                persistentListOf(ForYouNotification.UsedOutdatedData)
            } else {
                persistentListOf()
            },
            earnOpportunities = earnOpportunitiesUM.applyExpandedAssets(expandedEarnOpportunitiesAssetIds()),
            portfolioReviewUM = portfolioReviewUM.applyExpandedAssets(expandedPortfolioReviewAssetIds()),
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
        val items = ForYouPeriod.entries.map { period ->
            TangemSegmentUM(id = period.id, title = period.title)
        }
        return TangemSegmentedPickerUM(
            items = items.toPersistentList(),
            initialSelectedItem = items.first { it.id == ForYouPeriod.Day.id },
            isFixed = true,
            isAltSurface = true,
        )
    }
}