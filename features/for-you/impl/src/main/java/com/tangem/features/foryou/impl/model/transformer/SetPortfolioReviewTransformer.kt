package com.tangem.features.foryou.impl.model.transformer

import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
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
 * Applies one combined emission to the [ForYouUM] state: sets the pre-built portfolio-review,
 * earn-opportunities and portfolio-filter sections (see [ForYouPortfolioReviewConverter],
 * `ForYouEarnOpportunitiesConverter` and `ForYouPortfolioFilterConverter`) and derives the
 * outdated-data notification from the selected portfolio's aggregate total-balance source.
 *
 * The period picker is left as-is, so a refresh never resets the user's period selection.
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
    private val portfolioFilter: TangemFilterItemUM,
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
            portfolioFilter = portfolioFilter,
        )
    }
}