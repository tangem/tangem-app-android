package com.tangem.features.feed.crypto.categories

import com.tangem.domain.markets.GetMarketCategoriesUseCase
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Turns a deeplink's gecko category id into the [MarketCategorySelection.Entry] the chip needs.
 *
 * The listing has to be walked to do it: there is no fetch-by-id endpoint, and the endpoint's `search`
 * parameter matches names rather than ids. Walking it doubles as validation — an id nothing matches
 * resolves to `null`, and the caller then leaves the tab unfiltered.
 */
internal class MarketCategoryResolver @Inject constructor(
    private val getMarketCategoriesUseCase: GetMarketCategoriesUseCase,
) {

    // the tab holds its first list request until this answers, so a stalled walk must not hold the
    // whole Market Pulse list in a shimmer — an unfiltered list is the better outcome
    suspend fun resolve(categoryId: String, currency: String): MarketCategorySelection.Entry? =
        withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) { walk(categoryId = categoryId, currency = currency) }

    private suspend fun walk(categoryId: String, currency: String): MarketCategorySelection.Entry? {
        var offset = 0

        repeat(MAX_PAGES) {
            val page = getMarketCategoriesUseCase(
                currency = currency,
                offset = offset,
                limit = PAGE_LIMIT,
            ).getOrNull() ?: return null

            page.categories.firstOrNull { category -> category.id == categoryId }
                ?.let { category -> return MarketCategorySelection.Entry(id = category.id, name = category.name) }

            offset += page.categories.size

            if (page.categories.isEmpty() || offset >= page.total) return null
        }

        return null
    }

    private companion object {

        const val PAGE_LIMIT = 100
        const val RESOLVE_TIMEOUT_MILLIS = 3_000L

        /** The catalogue grows; one deeplink does not justify an unbounded walk of it. */
        const val MAX_PAGES = 5
    }
}