package com.tangem.features.earn.impl.overview

import com.tangem.features.earn.api.overview.EarnHeadline
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.ProductAvailability
import javax.inject.Inject

/**
 * The order of one currency's products: active first, then the rate descending (no rate last), then the registry rank.
 * One rule for the headline, `bestActive`, the picker chips and the bottom sheet rows.
 */
internal class EarnSummaryOrder @Inject constructor(
    private val registry: EarnProductsRegistry,
) : Comparator<EarnProductSummary> {

    private val order = compareByDescending<EarnProductSummary> { it.position != null }
        .thenByDescending { it.rate?.fraction }
        .thenBy { registry.rankOf(it.productId) }

    override fun compare(a: EarnProductSummary, b: EarnProductSummary): Int = order.compare(a, b)
}

internal class EarnHeadlineSelector @Inject constructor(
    private val order: EarnSummaryOrder,
    private val rateBadgeConverter: EarnRateBadgeConverter,
    private val registry: EarnProductsRegistry,
) {

    /** The rate is known, and the product is open or the user already has a position in it. */
    private val EarnProductSummary.isLive: Boolean
        get() = rate != null && (availability == ProductAvailability.Available || position != null)

    /**
     * "Active wins": the active candidate with the best rate, otherwise the best rate; a tie goes to the lower rank.
     * Candidates are the summaries of one currency, products and rate sources alike. The headline carries the route
     * of the winner: the For You card opens it on click.
     */
    fun select(candidates: List<EarnProductSummary>): EarnHeadline? {
        val winner = candidates
            .filter { it.isLive }
            .minWithOrNull(order)
            ?: return null
        val rate = winner.rate ?: return null

        return EarnHeadline(
            rate = rate,
            isActive = winner.position != null,
            badge = rateBadgeConverter.convert(rate),
            badgeAccent = registry.badgeAccentOf(winner.productId),
            route = winner.route,
        )
    }

    /**
     * The best of the active products: the `Auto` redirect of the picker leads to it. Rate sources are not passed
     * here, `Auto` never leads to them.
     */
    fun bestActive(summaries: List<EarnProductSummary>): EarnProductSummary? =
        summaries.filter { it.position != null }.minWithOrNull(order)
}