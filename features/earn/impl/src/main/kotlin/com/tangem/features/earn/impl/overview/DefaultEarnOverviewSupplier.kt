package com.tangem.features.earn.impl.overview

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.earn.api.overview.EarnBannerOverview
import com.tangem.features.earn.api.overview.EarnOverview
import com.tangem.features.earn.api.overview.EarnOverviewSupplier
import com.tangem.features.earn.api.overview.EarnOverviews
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.ProductAvailability
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject

internal class DefaultEarnOverviewSupplier @Inject constructor(
    private val summarySupplier: EarnProductSummarySupplier,
    private val availability: EarnAvailability,
    private val toggles: EarnFeatureToggles,
    private val registry: EarnProductsRegistry,
    private val selector: EarnHeadlineSelector,
    private val dispatchers: CoroutineDispatcherProvider,
) : EarnOverviewSupplier {

    /**
     * A projection without a cache of its own: a failure and "loading" are already expressed by the `source` of the
     * product summaries. An unsuitable wallet never reads them: `flatMapLatest` cancels the subscription, so the
     * producer stops with its last subscriber.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(userWalletId: UserWalletId): Flow<EarnOverviews> = availability.isWalletAvailable(userWalletId)
        .flatMapLatest { isAvailable ->
            if (isAvailable) projection(userWalletId) else flowOf(EarnOverviews.Empty)
        }
        .distinctUntilChanged()
        .flowOn(dispatchers.default)

    private fun projection(userWalletId: UserWalletId): Flow<EarnOverviews> = summarySupplier(userWalletId)
        .map { summaries ->
            EarnOverviews(
                byCurrency = (summaries.products.keys + summaries.rateSources.keys).associateWith { id ->
                    overviewOf(products = summaries[id], rateSources = summaries.rateSources[id].orEmpty())
                },
                source = summaries.source,
            )
        }

    private fun overviewOf(products: List<EarnProductSummary>, rateSources: List<EarnProductSummary>) = EarnOverview(
        headline = selector.select(products + rateSources),
        banner = bannerOf(products),
    )

    /**
     * The banner counts every registered product, pooled staking included, and shows the best rate among them; rate
     * sources stay out of it. It is Earn UI, so the Earn toggle switches it off, while the headline does not depend on
     * the toggle: the Staking and Yield Mode badges stay. When every product is closed for deposits and holds no
     * position, there is nothing to enable, so there is no banner.
     */
    private fun bannerOf(products: List<EarnProductSummary>): EarnBannerOverview? {
        if (!toggles.isEarnEnabled) return null

        val counted = products.filter { registry.find(it.productId) != null }
        val positions = counted.mapNotNull { it.position }
        if (positions.isEmpty() && counted.none { it.availability == ProductAvailability.Available }) return null

        val amounts = positions.mapNotNull { it.cryptoAmount }
        val best = selector.winnerOf(counted)

        return EarnBannerOverview(
            rate = best?.rate,
            bestProductId = best?.productId,
            activeCount = positions.size,
            totalPosition = amounts.takeIf { it.isNotEmpty() }?.reduce(BigDecimal::add),
        )
    }
}