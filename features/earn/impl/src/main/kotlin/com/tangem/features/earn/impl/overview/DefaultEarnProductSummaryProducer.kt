package com.tangem.features.earn.impl.overview

import arrow.core.Option
import arrow.core.some
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.getResultStatusSource
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRateEntry
import com.tangem.features.earn.product.api.product.ProductAvailability
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlin.time.Duration.Companion.seconds

internal class DefaultEarnProductSummaryProducer @AssistedInject constructor(
    @Assisted private val params: EarnProductSummaryProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val registry: EarnProductsRegistry,
    private val order: EarnSummaryOrder,
    private val rateBadgeConverter: EarnRateBadgeConverter,
    private val dispatchers: CoroutineDispatcherProvider,
) : EarnProductSummaryProducer {

    override val fallback: Option<EarnProductSummaries> = emptySummaries(StatusSource.ONLY_CACHE).some()

    override fun produce(): Flow<EarnProductSummaries> {
        val feeds = feeds()
        if (feeds.isEmpty()) return flowOf(emptySummaries(StatusSource.ACTUAL))

        return combine(feeds.map(::frames)) { frames -> frames.toList().merge() }
            .flowOn(dispatchers.default)
    }

    private fun emptySummaries(source: StatusSource) = EarnProductSummaries(
        products = emptyMap(),
        rateSources = emptyMap(),
        source = source,
    )

    private fun feeds(): List<Feed> {
        val productFeeds = registry.products.map { product ->
            Feed(
                id = product.id,
                isProduct = true,
                summaries = product.summaries(params.userWalletId),
            )
        }
        val rateSourceFeeds = registry.rateSources.map { source ->
            Feed(
                id = source.id,
                isProduct = false,
                summaries = source.rates(params.userWalletId).map { rates ->
                    rates.mapValues { (_, entry) -> entry.toSummary(source.id) }
                },
            )
        }

        return productFeeds + rateSourceFeeds
    }

    /**
     * Without risk and position amount: a rate source has no way to get them. The summary lives in `rateSources`, so
     * it never reaches the bottom sheet, the picker or the banner; the route is where the source leads on its own.
     */
    private fun EarnRateEntry.toSummary(productId: EarnProductId) = EarnProductSummary(
        productId = productId,
        rateText = rateBadgeConverter.convert(rate),
        risk = null,
        availability = ProductAvailability.Available,
        rate = rate,
        position = EarnPositionSummary(cryptoAmount = null).takeIf { isActive },
        source = source,
        onClickAction = NoAction,
        route = route,
    )

    /**
     * "Loading" goes out at once and a failure replaces the exception, a failed feed is requested again: a slow feed
     * does not hold the first frame back and a failed one does not break the others. A feed that failed after it had
     * loaded keeps its last summaries as cache until the retry succeeds, one that failed before the first answer
     * reports "failed" without data. A feed that completes without any answer counts as an empty one: otherwise its
     * "loading" frame would keep the whole summary on `CACHE` forever.
     */
    private fun frames(feed: Feed): Flow<ProductFrame> = flow {
        var isFailing = false
        var lastLoaded: ProductFrame.Loaded? = null
        val loadedFrames = feed.summaries
            .map<Map<CryptoCurrency.ID, EarnProductSummary>, ProductFrame> { summaries ->
                isFailing = false
                ProductFrame.Loaded(summaries, feed.isProduct).also { lastLoaded = it }
            }
            .onEmpty { emit(ProductFrame.Loaded(summaries = emptyMap(), isProduct = feed.isProduct)) }
            .retryWhen { cause, _ ->
                // Only `e` reaches the log file: once per series of failures, not on every retry
                if (!isFailing) TangemLogger.e("Earn summaries failed: ${feed.id.value}", cause)
                isFailing = true
                emit(lastLoaded?.asCached() ?: ProductFrame.Failed)
                delay(RETRY_DELAY)
                true
            }
        emitAll(loadedFrames)
    }.onStart { emit(ProductFrame.Loading) }

    private fun List<ProductFrame>.merge(): EarnProductSummaries {
        val loaded = filterIsInstance<ProductFrame.Loaded>()

        return EarnProductSummaries(
            products = loaded.filter { it.isProduct }.byCurrency(),
            rateSources = loaded.filterNot { it.isProduct }.byCurrency(),
            source = map(ProductFrame::source).getResultStatusSource(),
        )
    }

    private fun List<ProductFrame.Loaded>.byCurrency(): Map<CryptoCurrency.ID, List<EarnProductSummary>> =
        flatMap { it.summaries.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, list) -> list.sortedWith(order) }

    /** The last answer of a feed that failed: the data stays, only its source says that it is cache now. */
    private fun ProductFrame.Loaded.asCached() = copy(
        summaries = summaries.mapValues { (_, summary) -> summary.copy(source = StatusSource.ONLY_CACHE) },
        isCached = true,
    )

    private data class Feed(
        val id: EarnProductId,
        val isProduct: Boolean,
        val summaries: Flow<Map<CryptoCurrency.ID, EarnProductSummary>>,
    )

    /** A rate source has no screens: its summary is not shown anywhere, so there is nothing to click. */
    private data object NoAction : EarnAction {
        override val id: String = "none"
        override val label: TextReference = TextReference.EMPTY
    }

    private sealed interface ProductFrame {

        val source: StatusSource

        data object Loading : ProductFrame {
            override val source: StatusSource = StatusSource.CACHE
        }

        data object Failed : ProductFrame {
            override val source: StatusSource = StatusSource.ONLY_CACHE
        }

        data class Loaded(
            val summaries: Map<CryptoCurrency.ID, EarnProductSummary>,
            val isProduct: Boolean,
            val isCached: Boolean = false,
        ) : ProductFrame {

            override val source: StatusSource = if (isCached) {
                StatusSource.ONLY_CACHE
            } else {
                summaries.values.map { it.source }.getResultStatusSource()
            }
        }
    }

    @AssistedFactory
    interface Factory : EarnProductSummaryProducer.Factory {

        override fun create(params: EarnProductSummaryProducer.Params): DefaultEarnProductSummaryProducer
    }

    private companion object {

        val RETRY_DELAY = 10.seconds
    }
}